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
     * Vấn đề 3: Tự động xử lý các đơn READY quá thời gian lấy hàng (> 15 phút):
     * - Cơ chế Smart Handover: Tìm tài xế thay thế khả dụng (ngoại trừ tài xế hiện tại).
     * - Nếu tìm được tài xế mới: Chuyển giao phân công từ tài xế cũ sang tài xế mới.
     * - Nếu KHÔNG tìm được ai thay thế: Giữ nguyên tài xế cũ (để nếu họ đến muộn vẫn lấy được hàng),
     *   bắn thông báo nhắc nhở thúc giục (chống spam), và duy trì cờ PICKUP_TIMEOUT trên màn hình Staff.
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
                    Shipper currentShipper = assignment.getShipper();
                    Integer currentShipperId = currentShipper != null ? currentShipper.getUserId() : null;

                    log.info("Đơn hàng #{} đã quá {} phút kể từ lúc gán ({}) mà tài xế #{} chưa lấy. Đang tìm tài xế thay thế...",
                            o.getOrderId(), timeoutMinutes, assignedTime, currentShipperId);

                    // 1. Thử tìm và chuyển giao sang tài xế mới khả dụng (Smart Handover)
                    Optional<DeliveryAssignment> reassignedOpt = Optional.empty();
                    try {
                        reassignedOpt = autoDispatchService.autoReassignOrder(
                                o.getOrderId(), currentShipperId,
                                String.format("Tài xế cũ quá %d phút chưa tới lấy hàng", timeoutMinutes));
                    } catch (Exception e) {
                        log.warn("Lỗi khi autoReassignOrder cho đơn #{}: {}", o.getOrderId(), e.getMessage());
                    }

                    if (reassignedOpt.isPresent()) {
                        log.info("Smart Handover thành công: Đơn #{} đã chuyển từ Shipper #{} sang Shipper #{}",
                                o.getOrderId(), currentShipperId, reassignedOpt.get().getShipper().getUserId());
                    } else {
                        // 2. Không có ai thay thế -> VẪN GIỮ NGUYÊN TÀI XẾ CŨ!
                        log.info("Không có tài xế nào khác rảnh để thay thế đơn #{}. Giữ nguyên Shipper #{} và gửi nhắc nhở.",
                                o.getOrderId(), currentShipperId);

                        // Chống spam: Kiểm tra xem đã gửi nhắc nhở cho lần gán này chưa
                        boolean alreadyReminded = false;
                        List<OrderStatusHistory> histories = orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtAscHistoryIdAsc(o.getOrderId());
                        if (histories != null) {
                            for (OrderStatusHistory h : histories) {
                                if (h.getNote() != null && h.getNote().startsWith("[CẢNH BÁO QUÁ GIỜ]")) {
                                    if (h.getCreatedAt() != null && assignedTime != null && h.getCreatedAt().isAfter(assignedTime)) {
                                        alreadyReminded = true;
                                        break;
                                    } else if (h.getCreatedAt() == null) {
                                        alreadyReminded = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (!alreadyReminded && currentShipper != null && currentShipper.getUser() != null) {
                            try {
                                notificationService.createBaseNotification(
                                        currentShipper.getUser(),
                                        "⏰ Nhắc nhở khẩn trương: Đơn hàng #" + o.getOrderId() + " đã quá giờ lấy hàng!",
                                        String.format("Đơn hàng #%d đã quá %d phút bạn chưa đến lấy tại Shop. Vui lòng khẩn trương di chuyển đến Shop để nhận kiện hàng!",
                                                o.getOrderId(), timeoutMinutes),
                                        NotificationType.ORDER_STATUS,
                                        "/shipper/orders/" + o.getOrderId()
                                );

                                orderStatusHistoryRepository.save(new OrderStatusHistory(
                                        null, o, null, OrderStatus.READY, OrderStatus.READY,
                                        String.format("[CẢNH BÁO QUÁ GIỜ] Hệ thống đã gửi thông báo nhắc nhở tài xế %s khẩn trương đến Shop lấy hàng.",
                                                currentShipper.getUser().getFullName()),
                                        null
                                ));
                            } catch (Exception e) {
                                log.warn("Lỗi khi gửi thông báo nhắc nhở cho Shipper #{}: {}", currentShipperId, e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }
}
