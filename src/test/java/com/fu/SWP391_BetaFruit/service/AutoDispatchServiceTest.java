package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import com.fu.SWP391_BetaFruit.repository.DeliveryAssignmentRepository;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.ShipperRepository;
import com.fu.SWP391_BetaFruit.service.impl.AutoDispatchServiceImpl;
import com.fu.SWP391_BetaFruit.service.impl.DeliveryAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoDispatchServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShipperRepository shipperRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private DeliveryAssignmentService deliveryAssignmentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AutoDispatchServiceImpl autoDispatchService;

    private Order readyOrder;
    private Shop shop;
    private User shopOwner;

    @BeforeEach
    void setUp() {
        shopOwner = new User();
        shopOwner.setUserId(1);
        shopOwner.setUsername("shopowner");

        shop = new Shop();
        shop.setShopId(1);
        shop.setShopName("Vườn Trái Cây Hữu Cơ");
        shop.setOwner(shopOwner);

        readyOrder = new Order();
        readyOrder.setOrderId(501);
        readyOrder.setShop(shop);
        readyOrder.setOrderStatus(OrderStatus.READY);
        readyOrder.setPaymentMethod(PaymentMethod.COD);
        readyOrder.setFinalAmount(new BigDecimal("300000"));
    }

    private Shipper createShipper(int id, String name, UserStatus status, BigDecimal codDebt) {
        User u = new User();
        u.setUserId(id);
        u.setFullName(name);
        u.setStatus(status);

        Shipper s = new Shipper();
        s.setUserId(id);
        s.setUser(u);
        s.setCodDebt(codDebt);
        return s;
    }

    @Test
    @DisplayName("Auto-dispatch thành công: Chọn Shipper tối ưu (Active, ít tải nhất, nợ COD an toàn)")
    void autoDispatch_Success_SelectsBestShipper() {
        Shipper shipperBest = createShipper(101, "Shipper Tối Ưu", UserStatus.ACTIVE, new BigDecimal("500000"));
        Shipper shipperBusy = createShipper(102, "Shipper Đang Bận", UserStatus.ACTIVE, new BigDecimal("200000"));
        Shipper shipperInactive = createShipper(103, "Shipper Nghỉ Việc", UserStatus.DEACTIVATED, BigDecimal.ZERO);
        Shipper shipperDebtLimit = createShipper(104, "Shipper Nợ Cao", UserStatus.ACTIVE, new BigDecimal("2900000"));

        when(orderRepository.findLockedById(501)).thenReturn(Optional.of(readyOrder));
        when(deliveryAssignmentRepository.findByOrderOrderId(501)).thenReturn(Optional.empty());
        when(shipperRepository.findAll()).thenReturn(List.of(shipperBest, shipperBusy, shipperInactive, shipperDebtLimit));

        // ShipperBest có 1 đơn đang giao
        when(deliveryAssignmentRepository.countOngoingOrdersByShipper(eq(101), any())).thenReturn(1L);
        // ShipperBusy có 4 đơn đang giao
        when(deliveryAssignmentRepository.countOngoingOrdersByShipper(eq(102), any())).thenReturn(4L);
        // ShipperDebtLimit có nợ 2.9m + 300k = 3.2m (> 3m trần COD) -> Bị loại
        when(deliveryAssignmentRepository.countOngoingOrdersByShipper(eq(104), any())).thenReturn(0L);

        DeliveryAssignment mockAssignment = new DeliveryAssignment(99, readyOrder, shipperBest, shopOwner, null);
        when(deliveryAssignmentService.assign(501, 101, 1)).thenReturn(mockAssignment);

        Optional<DeliveryAssignment> result = autoDispatchService.autoDispatchOrder(501);

        assertTrue(result.isPresent());
        assertEquals(101, result.get().getShipper().getUserId());

        verify(deliveryAssignmentService).assign(501, 101, 1);
        verify(notificationService).createBaseNotification(eq(shipperBest.getUser()), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Auto-dispatch trả về empty khi tất cả Shipper đều quá tải (>= 5 đơn)")
    void autoDispatch_Fails_WhenAllShippersOverloaded() {
        Shipper s1 = createShipper(101, "Shipper 1", UserStatus.ACTIVE, BigDecimal.ZERO);
        when(orderRepository.findLockedById(501)).thenReturn(Optional.of(readyOrder));
        when(deliveryAssignmentRepository.findByOrderOrderId(501)).thenReturn(Optional.empty());
        when(shipperRepository.findAll()).thenReturn(List.of(s1));
        when(deliveryAssignmentRepository.countOngoingOrdersByShipper(eq(101), any())).thenReturn(5L); // 5 đơn -> Quá tải

        Optional<DeliveryAssignment> result = autoDispatchService.autoDispatchOrder(501);

        assertTrue(result.isEmpty());
        verify(deliveryAssignmentService, never()).assign(any(), any(), any());
    }

    @Test
    @DisplayName("Auto-dispatch trả về empty khi đơn COD khiến Shipper vượt trần 3.000.000 ₫")
    void autoDispatch_Fails_WhenAllShippersExceedCodCeiling() {
        // Đơn 500k, Shipper hiện nợ 2.7m -> 3.2m > 3m
        readyOrder.setFinalAmount(new BigDecimal("500000"));
        Shipper s1 = createShipper(101, "Shipper 1", UserStatus.ACTIVE, new BigDecimal("2700000"));

        when(orderRepository.findLockedById(501)).thenReturn(Optional.of(readyOrder));
        when(deliveryAssignmentRepository.findByOrderOrderId(501)).thenReturn(Optional.empty());
        when(shipperRepository.findAll()).thenReturn(List.of(s1));
        when(deliveryAssignmentRepository.countOngoingOrdersByShipper(eq(101), any())).thenReturn(1L);

        Optional<DeliveryAssignment> result = autoDispatchService.autoDispatchOrder(501);

        assertTrue(result.isEmpty());
        verify(deliveryAssignmentService, never()).assign(any(), any(), any());
    }

    @Test
    @DisplayName("Auto-dispatch bỏ qua khi đơn hàng đã được phân công trước đó")
    void autoDispatch_Skips_WhenAlreadyAssigned() {
        when(orderRepository.findLockedById(501)).thenReturn(Optional.of(readyOrder));
        when(deliveryAssignmentRepository.findByOrderOrderId(501)).thenReturn(Optional.of(new DeliveryAssignment()));

        Optional<DeliveryAssignment> result = autoDispatchService.autoDispatchOrder(501);

        assertTrue(result.isEmpty());
        verify(shipperRepository, never()).findAll();
        verify(deliveryAssignmentService, never()).assign(any(), any(), any());
    }
}
