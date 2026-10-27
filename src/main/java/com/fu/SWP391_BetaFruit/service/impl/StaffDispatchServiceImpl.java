package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.DispatchExceptionOrderResponse;
import com.fu.SWP391_BetaFruit.dto.response.DispatchShipperCandidateResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.AddressType;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import com.fu.SWP391_BetaFruit.service.StaffDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffDispatchServiceImpl implements StaffDispatchService {

    private static final List<OrderStatus> ONGOING_STATUSES = List.of(
            OrderStatus.READY,
            OrderStatus.PICKED_UP,
            OrderStatus.DELIVERING
    );

    private final OrderRepository orderRepository;
    private final ShipperRepository shipperRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired(required = false)
    private AddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DispatchExceptionOrderResponse> getExceptionOrders() {
        List<DispatchExceptionOrderResponse> exceptions = new ArrayList<>();

        // 1. Quét các đơn READY nhưng chưa có Shipper nào được gán
        List<Order> readyOrders = orderRepository.findAll().stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.READY)
                .toList();

        for (Order o : readyOrders) {
            Optional<DeliveryAssignment> assignmentOpt = deliveryAssignmentRepository.findByOrderOrderId(o.getOrderId());
            if (assignmentOpt.isEmpty()) {
                exceptions.add(DispatchExceptionOrderResponse.builder()
                        .orderId(o.getOrderId())
                        .orderCode(String.format("BF%06d", o.getOrderId()))
                        .shopName(o.getShop() != null ? o.getShop().getShopName() : "Cửa hàng")
                        .customerName(o.getCustomer() != null ? o.getCustomer().getFullName() : "Khách hàng")
                        .shippingAddress(o.getSnapshotShippingAddress())
                        .finalAmount(o.getFinalAmount())
                        .paymentMethod(o.getPaymentMethod() != null ? o.getPaymentMethod().name() : "")
                        .orderStatus(o.getOrderStatus().name())
                        .orderStatusLabel("Sẵn sàng giao (Chờ tài xế)")
                        .currentShipperId(null)
                        .currentShipperName("Chưa có")
                        .exceptionType("UNASSIGNED")
                        .incidentDetails("Hệ thống chưa tìm được tài xế tự động khả dụng")
                        .incidentTime(o.getCreatedAt())
                        .build());
            }
        }

        // 2. Quét các đơn đang có báo cáo sự cố từ bảng OrderStatusHistory
        List<OrderStatusHistory> histories = orderStatusHistoryRepository.findAll();
        for (OrderStatusHistory h : histories) {
            if (h.getNote() != null && h.getNote().startsWith("[SỰ CỐ TRÊN ĐƯỜNG]")) {
                Order order = h.getOrder();
                if (order != null && (order.getOrderStatus() == OrderStatus.PICKED_UP || order.getOrderStatus() == OrderStatus.DELIVERING)) {
                    Optional<DeliveryAssignment> assignmentOpt = deliveryAssignmentRepository.findByOrderOrderId(order.getOrderId());
                    Shipper currentShipper = assignmentOpt.map(DeliveryAssignment::getShipper).orElse(null);

                    exceptions.add(DispatchExceptionOrderResponse.builder()
                            .orderId(order.getOrderId())
                            .orderCode(String.format("BF%06d", order.getOrderId()))
                            .shopName(order.getShop() != null ? order.getShop().getShopName() : "Cửa hàng")
                            .customerName(order.getCustomer() != null ? order.getCustomer().getFullName() : "Khách hàng")
                            .shippingAddress(order.getSnapshotShippingAddress())
                            .finalAmount(order.getFinalAmount())
                            .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "")
                            .orderStatus(order.getOrderStatus().name())
                            .orderStatusLabel(order.getOrderStatus() == OrderStatus.PICKED_UP ? "Đã lấy hàng" : "Đang giao hàng")
                            .currentShipperId(currentShipper != null ? currentShipper.getUserId() : null)
                            .currentShipperName(currentShipper != null && currentShipper.getUser() != null ? currentShipper.getUser().getFullName() : "Tài xế")
                            .exceptionType("INCIDENT_REPORTED")
                            .incidentDetails(h.getNote())
                            .incidentTime(h.getCreatedAt())
                            .build());
                }
            }
        }

        return exceptions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchShipperCandidateResponse> getAvailableShippers() {
        List<Shipper> shippers = shipperRepository.findAll();
        List<DispatchShipperCandidateResponse> candidates = new ArrayList<>();

        for (Shipper s : shippers) {
            User user = s.getUser();
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                continue;
            }

            long ongoingCount = deliveryAssignmentRepository.countOngoingOrdersByShipper(s.getUserId(), ONGOING_STATUSES);
            BigDecimal codDebt = (s.getCodDebt() != null) ? s.getCodDebt() : BigDecimal.ZERO;
            boolean isOverloaded = ongoingCount >= 5;
            boolean isCodWarning = codDebt.compareTo(new BigDecimal("3000000")) >= 0;

            String operatingCity = "Chưa rõ";
            if (addressRepository != null) {
                operatingCity = addressRepository.findByUser_UserIdAndAddressTypeAndIsActiveTrue(
                        s.getUserId(), AddressType.PICKUP)
                        .stream().findFirst().map(Address::getCity).orElse("Chưa rõ");
            }

            candidates.add(DispatchShipperCandidateResponse.builder()
                    .shipperId(s.getUserId())
                    .fullName(user.getFullName())
                    .phone(user.getPhone())
                    .ongoingOrdersCount(ongoingCount)
                    .codDebt(codDebt)
                    .isOverloaded(isOverloaded)
                    .isCodWarning(isCodWarning)
                    .operatingCity(operatingCity)
                    .build());
        }

        candidates.sort(Comparator.comparing(DispatchShipperCandidateResponse::isOverloaded)
                .thenComparing(DispatchShipperCandidateResponse::ongoingOrdersCount));

        return candidates;
    }

    @Override
    @Transactional
    public DeliveryAssignment reassignShipper(Integer orderId, Integer newShipperId, Integer staffId, String reason) {
        Order order = orderRepository.findLockedById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng #" + orderId));

        Shipper newShipper = shipperRepository.findById(newShipperId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài xế cứu viện #" + newShipperId));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên vận hành #" + staffId));

        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderOrderId(orderId).orElse(null);

        if (assignment == null) {
            assignment = new DeliveryAssignment(null, order, newShipper, staff, null);
        } else {
            assignment.setShipper(newShipper);
            assignment.setAssignedBy(staff);
        }

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);

        String note = "Điều phối viên đã gán tài xế: " + newShipper.getUser().getFullName()
                + (reason != null && !reason.trim().isEmpty() ? " (Lý do: " + reason.trim() + ")" : "");

        orderStatusHistoryRepository.save(new OrderStatusHistory(
                null, order, staff, order.getOrderStatus(), order.getOrderStatus(), note, null
        ));

        // Bắn thông báo đẩy cho tài xế mới
        try {
            notificationService.createBaseNotification(
                    newShipper.getUser(),
                    "🚨 Phân công điều phối cứu viện đơn #" + orderId,
                    "Bạn được phân công tiếp nhận kiện hàng cho đơn #" + orderId + ". " + note,
                    NotificationType.ORDER_STATUS,
                    "/shipper/orders/" + orderId
            );
        } catch (Exception e) {
            log.warn("Lỗi khi bắn thông báo cho Shipper #{}: {}", newShipperId, e.getMessage());
        }

        log.info("Staff #{} đã tái gán đơn #{} cho Shipper #{} thành công. Lý do: {}",
                staffId, orderId, newShipperId, reason);

        return saved;
    }
}
