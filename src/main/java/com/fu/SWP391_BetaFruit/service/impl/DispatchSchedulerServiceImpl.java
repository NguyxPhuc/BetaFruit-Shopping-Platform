package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.DeliveryAssignment;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.entity.OrderStatusHistory;
import com.fu.SWP391_BetaFruit.entity.Shipper;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.DeliveryAssignmentRepository;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.OrderStatusHistoryRepository;
import com.fu.SWP391_BetaFruit.service.AutoDispatchService;
import com.fu.SWP391_BetaFruit.service.DispatchConfigService;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchSchedulerServiceImpl {

    private final OrderRepository orderRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final AutoDispatchService autoDispatchService;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final NotificationService notificationService;

    @Autowired(required = false)
    private DispatchConfigService dispatchConfigService;

    private int getPickupTimeoutMinutes() {
        return dispatchConfigService != null ? dispatchConfigService.getPickupTimeoutMinutes() : DispatchConfigService.DEFAULT_PICKUP_TIMEOUT_MINUTES;
    }

    /**
     * Chạy định kỳ mỗi 60 giây:
     * 1. Tự động thử lại điều phối (Retry) cho các đơn READY chưa tìm được tài xế.
     * 2. Tự động thu hồi và tái gán (Timeout Recovery) cho đơn READY quá 15 phút chưa đến lấy.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @Transactional
    public void runDispatchAutomation() {
        retryUnassignedReadyOrders();
        handlePickupTimeoutOrders();
    }

    /**
     * Vấn đề 4: Tự động thử lại cho các đơn READY chưa có tài xế nhận.
     */
    public void retryUnassignedReadyOrders() {
        List<Order> unassignedReadyOrders = orderRepository.findAll().stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.READY)
                .filter(o -> deliveryAssignmentRepository.findByOrderOrderId(o.getOrderId()).isEmpty())
                .toList();

        if (unassignedReadyOrders.isEmpty()) {
            return;
        }

        log.info("DispatchScheduler: Tìm thấy {} đơn READY chưa có tài xế, đang tự động thử lại...", unassignedReadyOrders.size());
        for (Order o : unassignedReadyOrders) {
            try {
                autoDispatchService.autoDispatchOrder(o.getOrderId());
            } catch (Exception e) {
                log.warn("Lỗi khi retry auto-dispatch đơn #{}: {}", o.getOrderId(), e.getMessage());
            }
        }
    }

    /**
     * Vấn đề 3: Tự động thu hồi và gán lại cho các đơn READY quá thời gian lấy hàng (> 15 phút).
     */
    public void handlePickupTimeoutOrders() {
        int timeoutMinutes = getPickupTimeoutMinutes();
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);

        List<Order> readyOrders = orderRepository.findAll().stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.READY)
                .toList();

        for (Order o : readyOrders) {
            Optional<DeliveryAssignment> assignmentOpt = deliveryAssignmentRepository.findByOrderOrderId(o.getOrderId());
            if (assignmentOpt.isPresent()) {
                DeliveryAssignment assignment = assignmentOpt.get();
                LocalDateTime assignedTime = assignment.getAssignedAt() != null ? assignment.getAssignedAt() : o.getCreatedAt();

                if (assignedTime != null && assignedTime.isBefore(timeoutThreshold)) {
                    Shipper oldShipper = assignment.getShipper();
                    log.warn("Đơn hàng #{} đã quá {} phút kể từ lúc gán ({}) mà tài xế #{} chưa lấy. Tiến hành thu hồi tự động...",
                            o.getOrderId(), timeoutMinutes, assignedTime, oldShipper != null ? oldShipper.getUserId() : null);

                    // 1. Xóa bản ghi phân công cũ
                    deliveryAssignmentRepository.delete(assignment);

                    // 2. Ghi lịch sử thu hồi
                    String note = String.format("Hệ thống tự động thu hồi đơn #%d do tài xế %s quá %d phút chưa tới lấy hàng.",
                            o.getOrderId(),
                            (oldShipper != null && oldShipper.getUser() != null) ? oldShipper.getUser().getFullName() : "Tài xế",
                            timeoutMinutes);
                    orderStatusHistoryRepository.save(new OrderStatusHistory(
                            null, o, null, OrderStatus.READY, OrderStatus.READY, note, null
                    ));

                    // 3. Gửi thông báo cho tài xế cũ
                    if (oldShipper != null && oldShipper.getUser() != null) {
                        try {
                            notificationService.createBaseNotification(
                                    oldShipper.getUser(),
                                    "⚠️ Thu hồi đơn hàng #" + o.getOrderId(),
                                    "Đơn hàng #" + o.getOrderId() + " đã bị thu hồi do quá " + timeoutMinutes + " phút bạn chưa đến lấy tại Shop.",
                                    NotificationType.ORDER_STATUS,
                                    "/shipper/orders"
                            );
                        } catch (Exception e) {
                            log.warn("Lỗi gửi thông báo thu hồi cho Shipper #{}: {}", oldShipper.getUserId(), e.getMessage());
                        }
                    }

                    // 4. Lập tức thử tự động gán lại cho tài xế khác khả dụng
                    try {
                        autoDispatchService.autoDispatchOrder(o.getOrderId());
                    } catch (Exception e) {
                        log.warn("Lỗi tái gán sau khi thu hồi đơn #{}: {}", o.getOrderId(), e.getMessage());
                    }
                }
            }
        }
    }
}
