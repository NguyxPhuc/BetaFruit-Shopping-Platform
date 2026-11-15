package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.impl.OrderDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử Nghiệp vụ: Xác nhận giao hàng & Bắn thông báo cho Customer/Shipper")
class OrderDeliveryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShipperRepository shipperRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderDeliveryService orderDeliveryService;

    private User customer;
    private User shipperUser;
    private Shipper shipper;
    private Shop shop;
    private Order deliveringOrder;
    private DeliveryAssignment assignment;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setUserId(10);
        customer.setFullName("Customer Lan");

        shipperUser = new User();
        shipperUser.setUserId(20);
        shipperUser.setFullName("Shipper Tuan");

        shipper = new Shipper();
        shipper.setUserId(20);
        shipper.setUser(shipperUser);
        shipper.setCodDebt(new BigDecimal("500000"));

        shop = new Shop();
        shop.setShopId(1);
        shop.setCommissionRate(new BigDecimal("5.00"));

        deliveringOrder = new Order();
        deliveringOrder.setOrderId(888);
        deliveringOrder.setCustomer(customer);
        deliveringOrder.setShop(shop);
        deliveringOrder.setOrderStatus(OrderStatus.DELIVERING);
        deliveringOrder.setPaymentMethod(PaymentMethod.COD);
        deliveringOrder.setTotalAmount(new BigDecimal("300000"));
        deliveringOrder.setDiscountAmount(BigDecimal.ZERO);
        deliveringOrder.setShippingFee(BigDecimal.ZERO);
        deliveringOrder.setFinalAmount(new BigDecimal("300000"));

        assignment = new DeliveryAssignment();
        assignment.setOrder(deliveringOrder);
        assignment.setShipper(shipper);
    }

    @Test
    @DisplayName("Giao hàng thành công: Cập nhật DELIVERED, cộng nợ COD, và gửi thông báo cho Customer")
    void markDelivered_Success_SendsNotificationToCustomer() {
        when(orderRepository.findLockedById(888)).thenReturn(Optional.of(deliveringOrder));
        when(deliveryAssignmentRepository.findByOrderOrderId(888)).thenReturn(Optional.of(assignment));
        when(userRepository.getReferenceById(20)).thenReturn(shipperUser);
        when(shipperRepository.findById(20)).thenReturn(Optional.of(shipper));

        orderDeliveryService.markDelivered(888, 20, true);

        assertEquals(OrderStatus.DELIVERED, deliveringOrder.getOrderStatus());
        assertEquals(new BigDecimal("800000"), shipper.getCodDebt()); // 500k + 300k
        verify(orderStatusHistoryRepository).save(any());
        verify(shipperRepository).save(shipper);

        // Kiểm tra thông báo bắn cho Customer
        verify(notificationService).createBaseNotification(
                eq(customer),
                contains("888"),
                any(),
                eq(NotificationType.ORDER_STATUS),
                eq("/orders/888")
        );
    }

    @Test
    @DisplayName("Giao hàng thành công chạm cảnh báo nợ COD (>= 2.4tr): Bắn thêm SYSTEM_ALERT cho Shipper")
    void markDelivered_HighCodDebt_SendsWarningToShipper() {
        // Nợ cũ 2.2tr + đơn này 300k = 2.5tr (>= 2.4tr)
        shipper.setCodDebt(new BigDecimal("2200000"));

        when(orderRepository.findLockedById(888)).thenReturn(Optional.of(deliveringOrder));
        when(deliveryAssignmentRepository.findByOrderOrderId(888)).thenReturn(Optional.of(assignment));
        when(userRepository.getReferenceById(20)).thenReturn(shipperUser);
        when(shipperRepository.findById(20)).thenReturn(Optional.of(shipper));

        orderDeliveryService.markDelivered(888, 20, true);

        assertEquals(new BigDecimal("2500000"), shipper.getCodDebt());

        // Kiểm tra thông báo cảnh báo nợ gửi cho Shipper
        verify(notificationService).createBaseNotification(
                eq(shipperUser),
                contains("Cảnh báo"),
                any(),
                eq(NotificationType.SYSTEM_ALERT),
                eq("/shipper/orders/cod-history")
        );
    }
}
