package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.DispatchExceptionOrderResponse;
import com.fu.SWP391_BetaFruit.dto.response.DispatchShipperCandidateResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
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

import java.math.BigDecimal;
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
}
