package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.CustomerOrderResponse;
import com.fu.SWP391_BetaFruit.dto.response.TierUpgradeResult;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.repository.DeliveryAssignmentRepository;
import com.fu.SWP391_BetaFruit.repository.OrderItemRepository;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.OrderStatusHistoryRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.impl.CustomerOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
class CustomerOrderConfirmTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipTierService membershipTierService;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CustomerOrderServiceImpl customerOrderService;

    private User customer;
    private Shop shop;
    private Order deliveredOrder;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setUserId(10);
        customer.setUsername("customer10");
        customer.setFullName("Nguyen Van Khach");

        shop = new Shop();
        shop.setShopId(1);
        shop.setShopName("Shop Hoa Qua Sach");

        deliveredOrder = new Order();
        deliveredOrder.setOrderId(1001);
        deliveredOrder.setCustomer(customer);
        deliveredOrder.setShop(shop);
        deliveredOrder.setOrderStatus(OrderStatus.DELIVERED);
        deliveredOrder.setTotalAmount(new BigDecimal("200000"));
        deliveredOrder.setDiscountAmount(BigDecimal.ZERO);
        deliveredOrder.setShippingFee(new BigDecimal("20000"));
        deliveredOrder.setFinalAmount(new BigDecimal("220000"));
        deliveredOrder.setPaymentMethod(PaymentMethod.COD);
        deliveredOrder.setSnapshotShippingAddress("123 Xuan Thuy, Cau Giay, Ha Noi");
    }

    @Test
    @DisplayName("Xác nhận nhận hàng thành công khi đơn ở trạng thái DELIVERED")
    void confirmReceived_Success_WhenDelivered() {
        when(orderRepository.findLockedById(1001)).thenReturn(Optional.of(deliveredOrder));
        when(userRepository.getReferenceById(10)).thenReturn(customer);

        TierUpgradeResult mockUpgrade = TierUpgradeResult.builder()
                .upgraded(true)
                .newTierId(2)
                .newTierName("Bạc")
                .newDiscountPercent(new BigDecimal("5.00"))
                .welcomeCouponCode("SILVER_FREESHIP")
                .build();

        when(membershipTierService.processOrderCompleted(eq(10), eq(1001), eq(new BigDecimal("220000"))))
                .thenReturn(mockUpgrade);

        TierUpgradeResult result = customerOrderService.confirmReceived(1001, 10);

        assertNotNull(result);
        assertTrue(result.isUpgraded());
        assertEquals("Bạc", result.getNewTierName());
        assertEquals(OrderStatus.SUCCESS, deliveredOrder.getOrderStatus());

        verify(orderRepository).save(deliveredOrder);
        verify(orderStatusHistoryRepository).save(any());
        verify(membershipTierService).processOrderCompleted(10, 1001, new BigDecimal("220000"));
        verify(notificationService).createBaseNotification(eq(customer), any(), any(), any(), eq("/orders/1001"));
    }

    @Test
    @DisplayName("Ném lỗi FORBIDDEN nếu khách hàng khác cố xác nhận đơn không phải của mình")
    void confirmReceived_ThrowsForbidden_WhenNotOwner() {
        when(orderRepository.findLockedById(1001)).thenReturn(Optional.of(deliveredOrder));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                customerOrderService.confirmReceived(1001, 999) // userId khác 10
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(orderRepository, never()).save(any());
        verify(membershipTierService, never()).processOrderCompleted(any(), any(), any());
    }

    @Test
    @DisplayName("Ném lỗi CONFLICT nếu đơn hàng chưa ở trạng thái DELIVERED")
    void confirmReceived_ThrowsConflict_WhenNotDelivered() {
        deliveredOrder.setOrderStatus(OrderStatus.DELIVERING); // Đang giao
        when(orderRepository.findLockedById(1001)).thenReturn(Optional.of(deliveredOrder));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                customerOrderService.confirmReceived(1001, 10)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(orderRepository, never()).save(any());
        verify(membershipTierService, never()).processOrderCompleted(any(), any(), any());
    }

    @Test
    @DisplayName("Ném lỗi NOT_FOUND nếu mã đơn hàng không tồn tại")
    void confirmReceived_ThrowsNotFound_WhenOrderNotExists() {
        when(orderRepository.findLockedById(9999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                customerOrderService.confirmReceived(9999, 10)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Lấy danh sách đơn hàng của khách hàng phân trang chính xác")
    void getCustomerOrders_ReturnsCorrectPage() {
        when(orderRepository.findByCustomer_UserId(eq(10), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deliveredOrder)));
        when(orderItemRepository.findByOrderOrderIdOrderByOrderItemIdAsc(1001))
                .thenReturn(Collections.emptyList());

        Page<CustomerOrderResponse> page = customerOrderService.getCustomerOrders(10, null, 0, 10);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        CustomerOrderResponse response = page.getContent().get(0);
        assertEquals(1001, response.orderId());
        assertEquals("BF001001", response.orderCode());
        assertEquals("Shop Hoa Qua Sach", response.shopName());
        assertTrue(response.canConfirmReceived());
    }
}
