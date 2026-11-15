package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.DispatchExceptionOrderResponse;
import com.fu.SWP391_BetaFruit.dto.response.DispatchShipperCandidateResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import com.fu.SWP391_BetaFruit.repository.AddressRepository;
import com.fu.SWP391_BetaFruit.repository.DeliveryAssignmentRepository;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.OrderStatusHistoryRepository;
import com.fu.SWP391_BetaFruit.repository.ShipperRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.impl.OrderDeliveryService;
import com.fu.SWP391_BetaFruit.service.impl.StaffDispatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffDispatchTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShipperRepository shipperRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private StaffDispatchServiceImpl staffDispatchService;

    private Order order;
    private Shipper originalShipper;
    private Shipper rescueShipper;
    private User staff;
    private DeliveryAssignment assignment;

    @BeforeEach
    void setUp() {
        User u1 = new User();
        u1.setUserId(101);
        u1.setFullName("Nguyen Van A (Shipper)");
        u1.setStatus(UserStatus.ACTIVE);

        originalShipper = new Shipper();
        originalShipper.setUserId(101);
        originalShipper.setUser(u1);
        originalShipper.setCodDebt(new BigDecimal("500000"));

        User u2 = new User();
        u2.setUserId(102);
        u2.setFullName("Tran Van B (Rescue Shipper)");
        u2.setStatus(UserStatus.ACTIVE);

        rescueShipper = new Shipper();
        rescueShipper.setUserId(102);
        rescueShipper.setUser(u2);
        rescueShipper.setCodDebt(BigDecimal.ZERO);

        staff = new User();
        staff.setUserId(201);
        staff.setFullName("Le Van Staff");

        order = new Order();
        order.setOrderId(701);
        order.setOrderStatus(OrderStatus.DELIVERING);
        order.setFinalAmount(new BigDecimal("250000"));
        order.setPaymentMethod(PaymentMethod.COD);
        order.setSnapshotShippingAddress("456 Cau Giay, Ha Noi");

        assignment = new DeliveryAssignment(55, order, originalShipper, staff, null);
    }

    @Test
    @DisplayName("Điều phối viên tái gán tài xế cứu viện thành công và gửi thông báo")
    void reassignShipper_SuccessfullyUpdatesAssignmentAndNotifies() {
        when(orderRepository.findLockedById(701)).thenReturn(Optional.of(order));
        when(shipperRepository.findById(102)).thenReturn(Optional.of(rescueShipper));
        when(userRepository.findById(201)).thenReturn(Optional.of(staff));
        when(deliveryAssignmentRepository.findByOrderOrderId(701)).thenReturn(Optional.of(assignment));
        when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryAssignment updated = staffDispatchService.reassignShipper(701, 102, 201, "Tài xế cũ bị hỏng xe");

        assertNotNull(updated);
        assertEquals(102, updated.getShipper().getUserId());
        assertEquals(201, updated.getAssignedBy().getUserId());

        verify(deliveryAssignmentRepository).save(assignment);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        verify(notificationService).createBaseNotification(eq(rescueShipper.getUser()), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Lấy danh sách đơn hàng ngoại lệ (unassigned và incident)")
    void getExceptionOrders_ReturnsUnassignedAndIncidentOrders() {
        Order unassignedOrder = new Order();
        unassignedOrder.setOrderId(801);
        unassignedOrder.setOrderStatus(OrderStatus.READY);
        unassignedOrder.setFinalAmount(new BigDecimal("150000"));

        when(orderRepository.findAll()).thenReturn(List.of(unassignedOrder));
        when(deliveryAssignmentRepository.findByOrderOrderId(801)).thenReturn(Optional.empty());

        OrderStatusHistory incidentHistory = new OrderStatusHistory();
        incidentHistory.setOrder(order);
        incidentHistory.setNote("[SỰ CỐ TRÊN ĐƯỜNG] Hỏng xe: Xe thủng lốp tại Xuân Thủy");

        when(orderStatusHistoryRepository.findAll()).thenReturn(List.of(incidentHistory));
        when(deliveryAssignmentRepository.findByOrderOrderId(701)).thenReturn(Optional.of(assignment));

        List<DispatchExceptionOrderResponse> exceptions = staffDispatchService.getExceptionOrders();

        assertNotNull(exceptions);
        assertEquals(2, exceptions.size());
        assertTrue(exceptions.stream().anyMatch(e -> e.exceptionType().equals("UNASSIGNED")));
        assertTrue(exceptions.stream().anyMatch(e -> e.exceptionType().equals("INCIDENT_REPORTED")));
    }

    @Test
    @DisplayName("Lấy danh sách ứng viên Shipper và hiển thị cảnh báo quá tải / trần COD")
    void getAvailableShippers_ReturnsCandidatesWithLoadAndCodDebt() {
        when(shipperRepository.findAll()).thenReturn(List.of(originalShipper, rescueShipper));
        when(deliveryAssignmentRepository.countOngoingOrdersByShipper(eq(101), any())).thenReturn(5L); // Quá tải
        when(deliveryAssignmentRepository.countOngoingOrdersByShipper(eq(102), any())).thenReturn(1L);

        List<DispatchShipperCandidateResponse> candidates = staffDispatchService.getAvailableShippers();

        assertNotNull(candidates);
        assertEquals(2, candidates.size());

        DispatchShipperCandidateResponse first = candidates.get(0); // Ưu tiên chưa quá tải
        assertEquals(102, first.shipperId());
        assertFalse(first.isOverloaded());

        DispatchShipperCandidateResponse second = candidates.get(1);
        assertEquals(101, second.shipperId());
        assertTrue(second.isOverloaded());
    }

    @Test
    @DisplayName("Gán thủ công thất bại: Đơn hàng đã ở trạng thái hoàn tất hoặc hủy (SUCCESS/CANCELLED)")
    void reassignShipper_Throws_WhenOrderStatusNotAllowable() {
        order.setOrderStatus(OrderStatus.SUCCESS);
        when(orderRepository.findLockedById(701)).thenReturn(Optional.of(order));
        when(shipperRepository.findById(102)).thenReturn(Optional.of(rescueShipper));
        when(userRepository.findById(201)).thenReturn(Optional.of(staff));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                staffDispatchService.reassignShipper(701, 102, 201, "Test"));

        assertTrue(ex.getReason().contains("Không thể điều phối đơn hàng đang ở trạng thái: SUCCESS"));
    }

    @Test
    @DisplayName("Gán thủ công thất bại: Tài khoản Shipper bị vô hiệu hóa (INACTIVE/DEACTIVATED)")
    void reassignShipper_Throws_WhenShipperNotActive() {
        rescueShipper.getUser().setStatus(UserStatus.DEACTIVATED);
        when(orderRepository.findLockedById(701)).thenReturn(Optional.of(order));
        when(shipperRepository.findById(102)).thenReturn(Optional.of(rescueShipper));
        when(userRepository.findById(201)).thenReturn(Optional.of(staff));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                staffDispatchService.reassignShipper(701, 102, 201, "Test"));

        assertTrue(ex.getReason().contains("Tài xế này hiện đang bị khóa hoặc không hoạt động"));
    }

    @Test
    @DisplayName("Gán thủ công thất bại: Tài xế cứu viện đã quá tải (>= 5 đơn)")
    void reassignShipper_Throws_WhenShipperOverloaded() {
        when(orderRepository.findLockedById(701)).thenReturn(Optional.of(order));
        when(shipperRepository.findById(102)).thenReturn(Optional.of(rescueShipper));
        when(userRepository.findById(201)).thenReturn(Optional.of(staff));
        when(deliveryAssignmentRepository.countOngoingOrdersByShipper(eq(102), any())).thenReturn(5L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                staffDispatchService.reassignShipper(701, 102, 201, "Test"));

        assertTrue(ex.getReason().contains("Tài xế đã nhận tối đa"));
    }

    @Test
    @DisplayName("Gán thủ công thất bại: Đơn COD làm nợ của tài xế vượt trần 3.000.000 ₫")
    void reassignShipper_Throws_WhenCodLimitExceeded() {
        rescueShipper.setCodDebt(new BigDecimal("2900000"));
        order.setFinalAmount(new BigDecimal("200000")); // 2.9m + 200k = 3.1m > 3m
        when(orderRepository.findLockedById(701)).thenReturn(Optional.of(order));
        when(shipperRepository.findById(102)).thenReturn(Optional.of(rescueShipper));
        when(userRepository.findById(201)).thenReturn(Optional.of(staff));
        when(deliveryAssignmentRepository.countOngoingOrdersByShipper(eq(102), any())).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                staffDispatchService.reassignShipper(701, 102, 201, "Test"));

        assertTrue(ex.getReason().contains("vượt mức tối đa"));
    }

    @Test
    @DisplayName("Danh sách sự cố không bị trùng lặp khi 1 đơn có nhiều bản ghi sự cố")
    void getExceptionOrders_DoesNotDuplicate_WhenMultipleIncidentsForSameOrder() {
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        OrderStatusHistory inc1 = new OrderStatusHistory();
        inc1.setHistoryId(1);
        inc1.setOrder(order);
        inc1.setCreatedAt(LocalDateTime.now().minusMinutes(20));
        inc1.setNote("[SỰ CỐ TRÊN ĐƯỜNG] Lần 1: Xe hỏng");

        OrderStatusHistory inc2 = new OrderStatusHistory();
        inc2.setHistoryId(2);
        inc2.setOrder(order);
        inc2.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        inc2.setNote("[SỰ CỐ TRÊN ĐƯỜNG] Lần 2: Mưa ngập đường");

        when(orderStatusHistoryRepository.findAll()).thenReturn(List.of(inc1, inc2));
        when(deliveryAssignmentRepository.findByOrderOrderId(701)).thenReturn(Optional.of(assignment));

        List<DispatchExceptionOrderResponse> exceptions = staffDispatchService.getExceptionOrders();

        assertEquals(1, exceptions.size());
        assertEquals("[SỰ CỐ TRÊN ĐƯỜNG] Lần 2: Mưa ngập đường", exceptions.get(0).incidentDetails());
    }

    @Test
    @DisplayName("Sự cố biến mất khỏi danh sách sau khi đã được Staff điều phối cứu viện")
    void getExceptionOrders_ExcludesIncident_WhenAlreadyRescuedByStaff() {
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        OrderStatusHistory inc = new OrderStatusHistory();
        inc.setHistoryId(1);
        inc.setOrder(order);
        inc.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        inc.setNote("[SỰ CỐ TRÊN ĐƯỜNG] Hỏng xe tại ngã tư");

        OrderStatusHistory rescue = new OrderStatusHistory();
        rescue.setHistoryId(2);
        rescue.setOrder(order);
        rescue.setCreatedAt(LocalDateTime.now().minusMinutes(15));
        rescue.setNote("Điều phối viên đã gán tài xế: Tran Van B (Rescue Shipper)");

        when(orderStatusHistoryRepository.findAll()).thenReturn(List.of(inc, rescue));

        List<DispatchExceptionOrderResponse> exceptions = staffDispatchService.getExceptionOrders();

        assertTrue(exceptions.isEmpty());
    }

    @Test
    @DisplayName("Đơn READY quá 15 phút chưa được lấy hàng xuất hiện cảnh báo PICKUP_TIMEOUT")
    void getExceptionOrders_DetectsPickupTimeout_WhenOver15Minutes() {
        Order readyOrder = new Order();
        readyOrder.setOrderId(888);
        readyOrder.setOrderStatus(OrderStatus.READY);
        readyOrder.setCreatedAt(LocalDateTime.now().minusMinutes(25));

        DeliveryAssignment staleAssignment = new DeliveryAssignment(12, readyOrder, originalShipper, staff, LocalDateTime.now().minusMinutes(20));

        when(orderRepository.findAll()).thenReturn(List.of(readyOrder));
        when(deliveryAssignmentRepository.findByOrderOrderId(888)).thenReturn(Optional.of(staleAssignment));
        when(orderStatusHistoryRepository.findAll()).thenReturn(Collections.emptyList());

        List<DispatchExceptionOrderResponse> exceptions = staffDispatchService.getExceptionOrders();

        assertEquals(1, exceptions.size());
        DispatchExceptionOrderResponse timeoutEx = exceptions.get(0);
        assertEquals("PICKUP_TIMEOUT", timeoutEx.exceptionType());
        assertTrue(timeoutEx.orderStatusLabel().contains("Quá giờ lấy hàng"));
    }
}
