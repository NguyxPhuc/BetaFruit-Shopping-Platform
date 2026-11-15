package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.DeliveryAssignment;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.entity.OrderStatusHistory;
import com.fu.SWP391_BetaFruit.entity.Shipper;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.DeliveryAssignmentRepository;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.OrderStatusHistoryRepository;
import com.fu.SWP391_BetaFruit.service.impl.DispatchSchedulerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchSchedulerServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private AutoDispatchService autoDispatchService;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DispatchConfigService dispatchConfigService;

    @InjectMocks
    private DispatchSchedulerServiceImpl dispatchSchedulerService;

    private Order readyOrder;
    private Shipper shipper;
    private User shipperUser;
    private DeliveryAssignment staleAssignment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dispatchSchedulerService, "dispatchConfigService", dispatchConfigService);

        readyOrder = new Order();
        readyOrder.setOrderId(901);
        readyOrder.setOrderStatus(OrderStatus.READY);
        readyOrder.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        shipperUser = new User();
        shipperUser.setUserId(88);
        shipperUser.setFullName("Nguyen Van Shipper");

        shipper = new Shipper();
        shipper.setUserId(88);
        shipper.setUser(shipperUser);

        staleAssignment = new DeliveryAssignment(10, readyOrder, shipper, null, LocalDateTime.now().minusMinutes(20));
    }

    @Test
    @DisplayName("Scheduler tự động retry auto-dispatch cho đơn READY chưa có Shipper")
    void retryUnassignedReadyOrders_CallsAutoDispatch() {
        when(orderRepository.findAll()).thenReturn(List.of(readyOrder));
        when(deliveryAssignmentRepository.findByOrderOrderId(901)).thenReturn(Optional.empty());

        dispatchSchedulerService.retryUnassignedReadyOrders();

        verify(autoDispatchService).autoDispatchOrder(901);
    }

    @Test
    @DisplayName("Smart Handover: Chuyển giao thành công sang tài xế mới khi tìm được ứng viên khả dụng")
    void handlePickupTimeoutOrders_SmartHandover_WhenAlternativeShipperFound() {
        when(dispatchConfigService.getPickupTimeoutMinutes()).thenReturn(15);
        when(orderRepository.findAll()).thenReturn(List.of(readyOrder));
        when(deliveryAssignmentRepository.findByOrderOrderId(901)).thenReturn(Optional.of(staleAssignment));

        Shipper newShipper = new Shipper();
        newShipper.setUserId(99);
        DeliveryAssignment newAssignment = new DeliveryAssignment(20, readyOrder, newShipper, null, LocalDateTime.now());

        when(autoDispatchService.autoReassignOrder(eq(901), eq(88), any())).thenReturn(Optional.of(newAssignment));

        dispatchSchedulerService.handlePickupTimeoutOrders();

        // Kiểm chứng Smart Handover được kích hoạt
        verify(autoDispatchService).autoReassignOrder(eq(901), eq(88), any());
        // Scheduler không tự xóa bừa mà để autoReassignOrder xử lý trọn gói
        verify(deliveryAssignmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Smart Handover: Giữ nguyên tài xế cũ và gửi nhắc nhở khẩn khi KHÔNG có ai thay thế")
    void handlePickupTimeoutOrders_RetainsShipperAndSendsNudge_WhenNoAlternative() {
        when(dispatchConfigService.getPickupTimeoutMinutes()).thenReturn(15);
        when(orderRepository.findAll()).thenReturn(List.of(readyOrder));
        when(deliveryAssignmentRepository.findByOrderOrderId(901)).thenReturn(Optional.of(staleAssignment));
        when(autoDispatchService.autoReassignOrder(eq(901), eq(88), any())).thenReturn(Optional.empty());
        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtAscHistoryIdAsc(901)).thenReturn(Collections.emptyList());

        dispatchSchedulerService.handlePickupTimeoutOrders();

        // 1. Phân công của tài xế cũ KHÔNG bị xóa
        verify(deliveryAssignmentRepository, never()).delete(any());
        // 2. Gửi thông báo nhắc nhở thúc giục tài xế cũ
        verify(notificationService).createBaseNotification(eq(shipperUser), contains("Nhắc nhở"), any(), any(), any());
        // 3. Ghi vết cảnh báo vào lịch sử
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Chống spam: Không gửi lại thông báo nhắc nhở nếu đã gửi trong chu kỳ timeout hiện tại")
    void handlePickupTimeoutOrders_DoesNotSpamNudge_WhenAlreadyReminded() {
        when(dispatchConfigService.getPickupTimeoutMinutes()).thenReturn(15);
        when(orderRepository.findAll()).thenReturn(List.of(readyOrder));
        when(deliveryAssignmentRepository.findByOrderOrderId(901)).thenReturn(Optional.of(staleAssignment));
        when(autoDispatchService.autoReassignOrder(eq(901), eq(88), any())).thenReturn(Optional.empty());

        OrderStatusHistory existingReminder = new OrderStatusHistory();
        existingReminder.setNote("[CẢNH BÁO QUÁ GIỜ] Hệ thống đã gửi thông báo nhắc nhở tài xế");
        existingReminder.setCreatedAt(LocalDateTime.now().minusMinutes(5)); // Sau thời điểm gán

        when(orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtAscHistoryIdAsc(901))
                .thenReturn(List.of(existingReminder));

        dispatchSchedulerService.handlePickupTimeoutOrders();

        // Không gửi thêm thông báo nào
        verify(notificationService, never()).createBaseNotification(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Scheduler bỏ qua đơn hàng chưa vượt quá thời gian timeout quy định")
    void handlePickupTimeoutOrders_Skips_WhenWithinThreshold() {
        when(dispatchConfigService.getPickupTimeoutMinutes()).thenReturn(15);
        // Đơn mới gán 5 phút trước
        staleAssignment.setAssignedAt(LocalDateTime.now().minusMinutes(5));

        when(orderRepository.findAll()).thenReturn(List.of(readyOrder));
        when(deliveryAssignmentRepository.findByOrderOrderId(901)).thenReturn(Optional.of(staleAssignment));

        dispatchSchedulerService.handlePickupTimeoutOrders();

        verify(deliveryAssignmentRepository, never()).delete(any());
        verify(autoDispatchService, never()).autoReassignOrder(any(), any(), any());
    }
}
