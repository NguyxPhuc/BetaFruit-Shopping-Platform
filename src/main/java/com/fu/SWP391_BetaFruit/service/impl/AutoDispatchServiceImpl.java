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

        List<Shipper> allShippers = shipperRepository.findAll();
        if (allShippers.isEmpty()) {
            log.warn("Hệ thống chưa có tài khoản Shipper nào để điều phối đơn #{}", orderId);
            return Optional.empty();
        }

        // Lấy khu vực của Shop (từ địa chỉ PICKUP của Shop Owner)
        String shopCity = "";
        if (addressRepository != null && order.getShop() != null && order.getShop().getOwner() != null) {
            Integer ownerId = order.getShop().getOwner().getUserId();
            shopCity = addressRepository.findByUser_UserIdAndAddressTypeAndIsActiveTrue(ownerId, AddressType.PICKUP)
                    .stream().findFirst().map(Address::getCity).orElse("");
        }

        // Lọc và chấm điểm các ứng viên Shipper
        record Candidate(Shipper shipper, long ongoingOrders, BigDecimal codDebt) {}
        List<Candidate> candidates = new ArrayList<>();

        BigDecimal orderAmount = (order.getFinalAmount() != null) ? order.getFinalAmount() : BigDecimal.ZERO;
        boolean isCod = (order.getPaymentMethod() == PaymentMethod.COD);

        for (Shipper shipper : allShippers) {
            User user = shipper.getUser();
            // Rule 1: Tài khoản ACTIVE
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                continue;
            }

            // Rule 2: Cân bằng tải - số đơn dở dang < 5
            long ongoingOrders = deliveryAssignmentRepository.countOngoingOrdersByShipper(
                    shipper.getUserId(), ONGOING_STATUSES);
            if (ongoingOrders >= MAX_ONGOING_ORDERS) {
                continue;
            }

            // Rule 3: Kiểm soát rủi ro công nợ COD <= 3.000.000 ₫
            BigDecimal currentDebt = (shipper.getCodDebt() != null) ? shipper.getCodDebt() : BigDecimal.ZERO;
            if (isCod) {
                BigDecimal projectedDebt = currentDebt.add(orderAmount);
                if (projectedDebt.compareTo(COD_CEILING_LIMIT) > 0) {
                    continue; // Bỏ qua nếu vượt trần nợ COD
                }
            }

            // Rule 4: So khớp khu vực hoạt động của Shipper với Shop (Geographical Matching)
            String shipperCity = "";
            if (addressRepository != null) {
                shipperCity = addressRepository.findByUser_UserIdAndAddressTypeAndIsActiveTrue(
                        shipper.getUserId(), AddressType.OPERATING)
                        .stream().findFirst().map(Address::getCity).orElse("");
            }

            if (!isAreaMatching(shopCity, shipperCity)) {
                log.info("Shipper #{} (khu vực: '{}') không cùng khu vực với Shop (khu vực: '{}'), bỏ qua",
                        shipper.getUserId(), shipperCity, shopCity);
                continue;
            }

            candidates.add(new Candidate(shipper, ongoingOrders, currentDebt));
        }

        if (candidates.isEmpty()) {
            log.warn("Tất cả Shipper đều quá tải, chạm trần COD hoặc không cùng khu vực với Shop '{}'. Đơn hàng #{} cần Operations Staff can thiệp.",
                    shopCity, orderId);
            return Optional.empty();
        }

        // Sắp xếp ưu tiên: ít đơn dở dang nhất -> nợ COD ít nhất
        candidates.sort(Comparator.comparingLong(Candidate::ongoingOrders)
                .thenComparing(Candidate::codDebt));

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
    public Optional<DeliveryAssignment> autoDispatchLeg2(Integer orderId, Integer currentShipperId) {
        Order order = orderRepository.findLockedById(orderId).orElse(null);
        if (order == null) {
            log.warn("Không tìm thấy đơn hàng #{} để tự động gán Shipper chặng 2", orderId);
            return Optional.empty();
        }

        // Lấy địa chỉ giao hàng của Khách hàng
        String customerCity = "";
        if (addressRepository != null && order.getCustomer() != null) {
            customerCity = addressRepository.findByUser_UserIdAndAddressTypeAndIsActiveTrue(
                    order.getCustomer().getUserId(), AddressType.SHIPPING)
                    .stream().findFirst().map(Address::getCity).orElse("");
        }
        if (customerCity.isBlank() && order.getSnapshotShippingAddress() != null) {
            customerCity = order.getSnapshotShippingAddress();
        }

        List<Shipper> allShippers = shipperRepository.findAll();
        BigDecimal orderAmount = (order.getFinalAmount() != null) ? order.getFinalAmount() : BigDecimal.ZERO;
        boolean isCod = (order.getPaymentMethod() == PaymentMethod.COD);

        record Candidate(Shipper shipper, long ongoingOrders, BigDecimal codDebt, String area) {}
        List<Candidate> candidates = new ArrayList<>();

        for (Shipper shipper : allShippers) {
            // Loại trừ Shipper 1 (tài xế vừa bàn giao tại trạm)
            if (currentShipperId != null && Objects.equals(shipper.getUserId(), currentShipperId)) {
                continue;
            }

            User user = shipper.getUser();
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                continue;
            }

            long ongoingOrders = deliveryAssignmentRepository.countOngoingOrdersByShipper(
                    shipper.getUserId(), ONGOING_STATUSES);
            if (ongoingOrders >= MAX_ONGOING_ORDERS) {
                continue;
            }

            BigDecimal currentDebt = (shipper.getCodDebt() != null) ? shipper.getCodDebt() : BigDecimal.ZERO;
            if (isCod) {
                BigDecimal projectedDebt = currentDebt.add(orderAmount);
                if (projectedDebt.compareTo(COD_CEILING_LIMIT) > 0) {
                    continue;
                }
            }

            // Kiểm tra khu vực hoạt động của Shipper 2 có khớp với Khách hàng không
            String shipperCity = "";
            if (addressRepository != null) {
                shipperCity = addressRepository.findByUser_UserIdAndAddressTypeAndIsActiveTrue(
                        shipper.getUserId(), AddressType.OPERATING)
                        .stream().findFirst().map(Address::getCity).orElse("");
            }

            if (!isAreaMatching(customerCity, shipperCity)) {
                log.info("Shipper #{} (khu vực: '{}') không cùng khu vực với Khách hàng (khu vực: '{}'), bỏ qua chặng 2",
                        shipper.getUserId(), shipperCity, customerCity);
                continue;
            }

            candidates.add(new Candidate(shipper, ongoingOrders, currentDebt, shipperCity));
        }

        if (candidates.isEmpty()) {
            log.warn("Chặng 2: Không tìm thấy Shipper khả dụng cùng khu vực khách hàng '{}'. Cần Staff can thiệp.", customerCity);
            return Optional.empty();
        }

        candidates.sort(Comparator.comparingLong(Candidate::ongoingOrders)
                .thenComparing(Candidate::codDebt));

        Candidate best = candidates.get(0);
        Shipper selectedShipper = best.shipper();

        try {
            DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderOrderId(orderId).orElse(null);
            if (assignment != null) {
                assignment.setShipper(selectedShipper);
                assignment.setAssignedBy(selectedShipper.getUser());
                deliveryAssignmentRepository.save(assignment);
            } else {
                assignment = deliveryAssignmentRepository.save(
                        new DeliveryAssignment(null, order, selectedShipper, selectedShipper.getUser(), null)
                );
            }

            // Ghi lịch sử chuyển giao sang Shipper 2
            if (orderStatusHistoryRepository != null) {
                String note = "Hệ thống tự động điều phối Chặng 2: Gán tài xế " + selectedShipper.getUser().getFullName()
                        + " (Khu vực: " + best.area() + ") tiếp nhận kiện hàng tại trạm ảo";
                orderStatusHistoryRepository.save(new OrderStatusHistory(
                        null, order, selectedShipper.getUser(), order.getOrderStatus(), order.getOrderStatus(), note, null
                ));
            }

            // Bắn thông báo đẩy cho Shipper 2
            try {
                notificationService.createBaseNotification(
                        selectedShipper.getUser(),
                        "🛵 Kiện hàng trung chuyển #" + orderId + " đã sẵn sàng!",
                        "Bạn được tự động điều phối tiếp nhận đơn #" + orderId + " tại trạm ảo để giao cho khách.",
                        NotificationType.ORDER_STATUS,
                        "/shipper/orders/" + orderId
                );
            } catch (Exception e) {
                log.warn("Không thể gửi thông báo cho Shipper #{}: {}", selectedShipper.getUserId(), e.getMessage());
            }

            log.info("Auto-Dispatch Chặng 2 THÀNH CÔNG: Đơn hàng #{} đã được tự động gán cho Shipper #{} ({})",
                    orderId, selectedShipper.getUserId(), selectedShipper.getUser().getFullName());

            return Optional.of(assignment);
        } catch (Exception e) {
            log.error("Lỗi khi gán Chặng 2 đơn #{} cho Shipper #{}: {}", orderId, selectedShipper.getUserId(), e.getMessage());
            return Optional.empty();
        }
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
