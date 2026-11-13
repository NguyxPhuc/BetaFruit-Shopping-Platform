package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.DispatchExceptionOrderResponse;
import com.fu.SWP391_BetaFruit.dto.response.DispatchShipperCandidateResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.AddressType;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
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

    @Autowired(required = false)
    private com.fu.SWP391_BetaFruit.service.DispatchConfigService dispatchConfigService;

    public int getMaxOngoingOrders() {
        return dispatchConfigService != null ? dispatchConfigService.getMaxOngoingOrders() : com.fu.SWP391_BetaFruit.service.DispatchConfigService.DEFAULT_MAX_ONGOING_ORDERS;
    }

    public BigDecimal getCodCeilingLimit() {
        return dispatchConfigService != null ? dispatchConfigService.getCodCeilingLimit() : com.fu.SWP391_BetaFruit.service.DispatchConfigService.DEFAULT_COD_CEILING_LIMIT;
    }

    public int getPickupTimeoutMinutes() {
        return dispatchConfigService != null ? dispatchConfigService.getPickupTimeoutMinutes() : com.fu.SWP391_BetaFruit.service.DispatchConfigService.DEFAULT_PICKUP_TIMEOUT_MINUTES;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispatchExceptionOrderResponse> getExceptionOrders() {
        List<DispatchExceptionOrderResponse> exceptions = new ArrayList<>();

        // 1. Quét các đơn READY: chưa có Shipper HOẶC đã có Shipper nhưng quá 15 phút chưa đến lấy
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
            } else {
                // Đơn đã gán nhưng kiểm tra xem có bị quá hạn lấy hàng không (Timeout)
                DeliveryAssignment assignment = assignmentOpt.get();
                LocalDateTime assignedTime = assignment.getAssignedAt() != null ? assignment.getAssignedAt() : o.getCreatedAt();
                int timeoutMinutes = getPickupTimeoutMinutes();
                if (assignedTime != null && assignedTime.plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now())) {
                    Shipper currentShipper = assignment.getShipper();
                    exceptions.add(DispatchExceptionOrderResponse.builder()
                            .orderId(o.getOrderId())
                            .orderCode(String.format("BF%06d", o.getOrderId()))
                            .shopName(o.getShop() != null ? o.getShop().getShopName() : "Cửa hàng")
                            .customerName(o.getCustomer() != null ? o.getCustomer().getFullName() : "Khách hàng")
                            .shippingAddress(o.getSnapshotShippingAddress())
                            .finalAmount(o.getFinalAmount())
                            .paymentMethod(o.getPaymentMethod() != null ? o.getPaymentMethod().name() : "")
                            .orderStatus(o.getOrderStatus().name())
                            .orderStatusLabel("Quá giờ lấy hàng (> " + timeoutMinutes + " phút)")
                            .currentShipperId(currentShipper != null ? currentShipper.getUserId() : null)
                            .currentShipperName(currentShipper != null && currentShipper.getUser() != null ? currentShipper.getUser().getFullName() : "Tài xế")
                            .exceptionType("PICKUP_TIMEOUT")
                            .incidentDetails(String.format("Tài xế đã nhận đơn nhưng quá %d phút chưa tới lấy hàng", timeoutMinutes))
                            .incidentTime(assignedTime)
                            .build());
                }
            }
        }

        // 2. Quét các đơn đang có báo cáo sự cố từ bảng OrderStatusHistory
        // Gom nhóm theo orderId để tránh trùng lặp và loại bỏ các sự cố đã được cứu viện
        List<OrderStatusHistory> histories = orderStatusHistoryRepository.findAll();
        Map<Integer, List<OrderStatusHistory>> orderHistoriesMap = new LinkedHashMap<>();
        for (OrderStatusHistory h : histories) {
            if (h.getOrder() != null && h.getOrder().getOrderId() != null) {
                orderHistoriesMap.computeIfAbsent(h.getOrder().getOrderId(), k -> new ArrayList<>()).add(h);
            }
        }

        for (Map.Entry<Integer, List<OrderStatusHistory>> entry : orderHistoriesMap.entrySet()) {
            List<OrderStatusHistory> list = entry.getValue();
            Order order = list.get(0).getOrder();
            if (order == null || (order.getOrderStatus() != OrderStatus.PICKED_UP && order.getOrderStatus() != OrderStatus.DELIVERING)) {
                continue;
            }

            // Sắp xếp lịch sử: ưu tiên createdAt desc, fallback historyId desc
            list.sort((h1, h2) -> {
                if (h1.getCreatedAt() != null && h2.getCreatedAt() != null) {
                    return h2.getCreatedAt().compareTo(h1.getCreatedAt());
                }
                if (h1.getHistoryId() != null && h2.getHistoryId() != null) {
                    return h2.getHistoryId().compareTo(h1.getHistoryId());
                }
                return 0;
            });

            OrderStatusHistory latestIncident = null;
            OrderStatusHistory latestRescue = null;

            for (OrderStatusHistory h : list) {
                if (h.getNote() != null) {
                    if (h.getNote().startsWith("[SỰ CỐ TRÊN ĐƯỜNG]") && latestIncident == null) {
                        latestIncident = h;
                    }
                    if (h.getNote().startsWith("Điều phối viên đã gán tài xế") && latestRescue == null) {
                        latestRescue = h;
                    }
                }
            }

            if (latestIncident != null) {
                boolean isRescued = false;
                if (latestRescue != null) {
                    if (latestRescue.getCreatedAt() != null && latestIncident.getCreatedAt() != null) {
                        isRescued = latestRescue.getCreatedAt().isAfter(latestIncident.getCreatedAt());
                    } else if (latestRescue.getHistoryId() != null && latestIncident.getHistoryId() != null) {
                        isRescued = latestRescue.getHistoryId() > latestIncident.getHistoryId();
                    } else {
                        int rescueIdx = list.indexOf(latestRescue);
                        int incidentIdx = list.indexOf(latestIncident);
                        isRescued = rescueIdx < incidentIdx;
                    }
                }

                if (!isRescued) {
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
                            .incidentDetails(latestIncident.getNote())
                            .incidentTime(latestIncident.getCreatedAt())
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
            boolean isOverloaded = ongoingCount >= getMaxOngoingOrders();
            boolean isCodWarning = codDebt.compareTo(getCodCeilingLimit()) >= 0;

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

        // 1. Chặn đơn hàng không ở trạng thái vận hành hợp lệ
        List<OrderStatus> allowableStatuses = List.of(OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.DELIVERING);
        if (!allowableStatuses.contains(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể điều phối đơn hàng đang ở trạng thái: " + order.getOrderStatus());
        }

        // 2. Chặn Shipper không ở trạng thái ACTIVE
        User shipperUser = newShipper.getUser();
        if (shipperUser == null || shipperUser.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài xế này hiện đang bị khóa hoặc không hoạt động!");
        }

        // 3. Chặn Shipper đã nhận tối đa số đơn dở dang cho phép
        int maxOrders = getMaxOngoingOrders();
        long ongoingCount = deliveryAssignmentRepository.countOngoingOrdersByShipper(newShipperId, ONGOING_STATUSES);
        if (ongoingCount >= maxOrders) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tài xế đã nhận tối đa " + maxOrders + " đơn dở dang. Vui lòng chọn tài xế khác!");
        }

        // 4. Chặn đơn COD vượt trần nợ cho phép
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            BigDecimal currentDebt = (newShipper.getCodDebt() != null) ? newShipper.getCodDebt() : BigDecimal.ZERO;
            BigDecimal orderAmount = (order.getFinalAmount() != null) ? order.getFinalAmount() : BigDecimal.ZERO;
            BigDecimal ceiling = getCodCeilingLimit();
            if (currentDebt.add(orderAmount).compareTo(ceiling) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Đơn hàng COD này sẽ làm công nợ của tài xế vượt mức tối đa " + ceiling + " ₫!");
            }
        }

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
