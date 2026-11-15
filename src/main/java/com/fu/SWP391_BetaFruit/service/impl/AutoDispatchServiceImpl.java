package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.AddressType;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.AutoDispatchService;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoDispatchServiceImpl implements AutoDispatchService {

    public static final BigDecimal COD_CEILING_LIMIT = new BigDecimal("3000000"); // 3.000.000 ₫
    public static final int MAX_ONGOING_ORDERS = 5;

    private static final List<OrderStatus> ONGOING_STATUSES = List.of(
            OrderStatus.READY,
            OrderStatus.PICKED_UP,
            OrderStatus.DELIVERING
    );

    private final OrderRepository orderRepository;
    private final ShipperRepository shipperRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryAssignmentService deliveryAssignmentService;
    private final NotificationService notificationService;
    private final AddressRepository addressRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.fu.SWP391_BetaFruit.service.DispatchConfigService dispatchConfigService;

    public int getMaxOngoingOrders() {
        return dispatchConfigService != null ? dispatchConfigService.getMaxOngoingOrders() : MAX_ONGOING_ORDERS;
    }

    public BigDecimal getCodCeilingLimit() {
        return dispatchConfigService != null ? dispatchConfigService.getCodCeilingLimit() : COD_CEILING_LIMIT;
    }

    record Candidate(Shipper shipper, long ongoingOrders, BigDecimal codDebt) {}

    private List<Candidate> findEligibleCandidates(Order order, Integer excludeShipperId) {
        List<Shipper> allShippers = shipperRepository.findAll();
        if (allShippers.isEmpty()) {
            return Collections.emptyList();
        }

        String shopCity = "";
        if (addressRepository != null && order.getShop() != null && order.getShop().getOwner() != null) {
            Integer ownerId = order.getShop().getOwner().getUserId();
            shopCity = addressRepository.findByUser_UserIdAndAddressTypeAndIsActiveTrue(ownerId, AddressType.PICKUP)
                    .stream().findFirst().map(Address::getCity).orElse("");
        }

        BigDecimal orderAmount = (order.getFinalAmount() != null) ? order.getFinalAmount() : BigDecimal.ZERO;
        boolean isCod = (order.getPaymentMethod() == PaymentMethod.COD);
        int maxOrders = getMaxOngoingOrders();
        BigDecimal ceilingLimit = getCodCeilingLimit();

        List<Candidate> candidates = new ArrayList<>();
        for (Shipper shipper : allShippers) {
            if (excludeShipperId != null && shipper.getUserId().equals(excludeShipperId)) {
                continue;
            }

            User user = shipper.getUser();
            // Rule 1: Tài khoản ACTIVE
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                continue;
            }

            // Rule 2: Cân bằng tải - số đơn dở dang < maxOngoingOrders
            long ongoingOrders = deliveryAssignmentRepository.countOngoingOrdersByShipper(
                    shipper.getUserId(), ONGOING_STATUSES);
            if (ongoingOrders >= maxOrders) {
                continue;
            }

            // Rule 3: Kiểm soát rủi ro công nợ COD <= codCeilingLimit
            BigDecimal currentDebt = (shipper.getCodDebt() != null) ? shipper.getCodDebt() : BigDecimal.ZERO;
            if (isCod) {
                BigDecimal projectedDebt = currentDebt.add(orderAmount);
                if (projectedDebt.compareTo(ceilingLimit) > 0) {
                    continue; // Bỏ qua nếu vượt trần nợ COD
                }
            }

            // Rule 4: So khớp khu vực hoạt động của Shipper với Shop (Geographical Matching)
            String shipperCity = "";
            if (addressRepository != null) {
                shipperCity = addressRepository.findByUser_UserIdAndAddressTypeAndIsActiveTrue(
                        shipper.getUserId(), AddressType.PICKUP)
                        .stream().findFirst().map(Address::getCity).orElse("");
            }

            if (!isAreaMatching(shopCity, shipperCity)) {
                log.info("Shipper #{} (khu vực: '{}') không cùng khu vực với Shop (khu vực: '{}'), bỏ qua",
                        shipper.getUserId(), shipperCity, shopCity);
                continue;
            }

            candidates.add(new Candidate(shipper, ongoingOrders, currentDebt));
        }

        candidates.sort(Comparator.comparingLong(Candidate::ongoingOrders)
                .thenComparing(Candidate::codDebt));
        return candidates;
    }

    @Override
    @Transactional
    public Optional<DeliveryAssignment> autoDispatchOrder(Integer orderId) {
        Order order = orderRepository.findLockedById(orderId).orElse(null);
        if (order == null) {
            log.warn("Không tìm thấy đơn hàng #{} để tự động gán Shipper", orderId);
            return Optional.empty();
        }

        if (order.getOrderStatus() != OrderStatus.READY) {
            log.info("Đơn hàng #{} không ở trạng thái READY (hiện tại: {}), bỏ qua auto-dispatch",
                    orderId, order.getOrderStatus());
            return Optional.empty();
        }

        if (deliveryAssignmentRepository.findByOrderOrderId(orderId).isPresent()) {
            log.info("Đơn hàng #{} đã có bản ghi DeliveryAssignment, bỏ qua auto-dispatch", orderId);
            return Optional.empty();
        }

        List<Candidate> candidates = findEligibleCandidates(order, null);
        if (candidates.isEmpty()) {
            log.warn("Tất cả Shipper đều quá tải, chạm trần COD hoặc không cùng khu vực với Shop. Đơn hàng #{} cần Operations Staff can thiệp.", orderId);
            return Optional.empty();
        }

        Shipper selectedShipper = candidates.get(0).shipper();
        User assigner = (order.getShop() != null && order.getShop().getOwner() != null)
                ? order.getShop().getOwner()
                : selectedShipper.getUser();

        try {
            DeliveryAssignment assignment = deliveryAssignmentService.assign(
                    orderId, selectedShipper.getUserId(), assigner.getUserId());

            // Gửi thông báo đẩy cho Shipper
            String shopName = (order.getShop() != null) ? order.getShop().getShopName() : "Cửa hàng";
            String title = "🛵 Đơn hàng mới #" + orderId + " đã sẵn sàng lấy!";
            String message = String.format("Cửa hàng %s đã chuẩn bị xong hoa quả. Vui lòng đến lấy hàng trong vòng 15 phút.", shopName);

            try {
                notificationService.createBaseNotification(
                        selectedShipper.getUser(),
                        title,
                        message,
                        NotificationType.ORDER_STATUS,
                        "/shipper/orders/" + orderId
                );
            } catch (Exception e) {
                log.warn("Không thể gửi thông báo cho Shipper #{}: {}", selectedShipper.getUserId(), e.getMessage());
            }

            log.info("Auto-Dispatch Chặng 1 THÀNH CÔNG: Đơn hàng #{} đã được tự động gán cho Shipper #{} ({})",
                    orderId, selectedShipper.getUserId(), selectedShipper.getUser().getFullName());

            return Optional.of(assignment);
        } catch (Exception e) {
            log.error("Lỗi khi gán đơn hàng #{} cho Shipper #{}: {}", orderId, selectedShipper.getUserId(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public Optional<DeliveryAssignment> autoReassignOrder(Integer orderId, Integer currentShipperId, String reason) {
        Order order = orderRepository.findLockedById(orderId).orElse(null);
        if (order == null || order.getOrderStatus() != OrderStatus.READY) {
            return Optional.empty();
        }

        DeliveryAssignment existingAssignment = deliveryAssignmentRepository.findByOrderOrderId(orderId).orElse(null);
        if (existingAssignment == null) {
            return Optional.empty();
        }

        // Tìm ứng viên thay thế khả dụng (loại trừ currentShipperId)
        List<Candidate> candidates = findEligibleCandidates(order, currentShipperId);
        if (candidates.isEmpty()) {
            return Optional.empty(); // Không có ai thay thế -> không swap, giữ nguyên tài xế cũ
        }

        Shipper selectedShipper = candidates.get(0).shipper();
        Shipper oldShipper = existingAssignment.getShipper();
        User assigner = existingAssignment.getAssignedBy() != null ? existingAssignment.getAssignedBy() : selectedShipper.getUser();

        // 1. Thu hồi phân công cũ và tạo phân công mới
        deliveryAssignmentRepository.delete(existingAssignment);
        deliveryAssignmentRepository.flush();

        DeliveryAssignment newAssignment = new DeliveryAssignment(null, order, selectedShipper, assigner, null);
        DeliveryAssignment saved = deliveryAssignmentRepository.save(newAssignment);

        // 2. Ghi log lịch sử chuyển giao
        String note = String.format("Hệ thống tự động chuyển giao đơn cho tài xế mới: %s%s",
                selectedShipper.getUser().getFullName(),
                (reason != null && !reason.isBlank() ? " (Lý do: " + reason.trim() + ")" : ""));
        orderStatusHistoryRepository.save(new OrderStatusHistory(
                null, order, null, OrderStatus.READY, OrderStatus.READY, note, null
        ));

        // 3. Thông báo cho tài xế cũ
        if (oldShipper != null && oldShipper.getUser() != null) {
            try {
                notificationService.createBaseNotification(
                        oldShipper.getUser(),
                        "⚠️ Đơn hàng #" + orderId + " đã được chuyển giao cho tài xế khác",
                        "Đơn hàng #" + orderId + " đã được chuyển sang tài xế khác do quá hạn lấy hàng tại Shop.",
                        NotificationType.ORDER_STATUS,
                        "/shipper/orders"
                );
            } catch (Exception e) {
                log.warn("Lỗi gửi thông báo chuyển giao cho Shipper cũ #{}: {}", oldShipper.getUserId(), e.getMessage());
            }
        }

        // 4. Thông báo cho tài xế mới
        String shopName = (order.getShop() != null) ? order.getShop().getShopName() : "Cửa hàng";
        try {
            notificationService.createBaseNotification(
                    selectedShipper.getUser(),
                    "🛵 Đơn hàng mới #" + orderId + " được điều phối cho bạn!",
                    String.format("Cửa hàng %s đã chuẩn bị xong hoa quả. Vui lòng đến lấy hàng trong vòng 15 phút.", shopName),
                    NotificationType.ORDER_STATUS,
                    "/shipper/orders/" + orderId
            );
        } catch (Exception e) {
            log.warn("Lỗi gửi thông báo cho Shipper mới #{}: {}", selectedShipper.getUserId(), e.getMessage());
        }

        log.info("Smart Handover THÀNH CÔNG: Đơn hàng #{} đã chuyển giao từ Shipper #{} sang Shipper #{}",
                orderId, currentShipperId, selectedShipper.getUserId());

        return Optional.of(saved);
    }


    private boolean isAreaMatching(String areaA, String areaB) {
        if (areaA == null || areaB == null || areaA.isBlank() || areaB.isBlank()) return true;
        String a = normalize(areaA);
        String b = normalize(areaB);
        return a.contains(b) || b.contains(a);
    }

    private String normalize(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9]", "");
    }
}
