package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShopOrderDetailServiceTests {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);
    private final OrderItemRepository items = mock(OrderItemRepository.class);
    private final DeliveryAssignmentRepository deliveries = mock(DeliveryAssignmentRepository.class);
    private final PaymentTransactionRepository payments = mock(PaymentTransactionRepository.class);
    private final ShopOrderServiceImpl service = new ShopOrderServiceImpl(
            orders, history, mock(UserRepository.class), items, deliveries, payments);

    @Test
    void unavailableOrOtherOwnersOrderDoesNotLoadRelatedData() {
        when(orders.findDetailForShopOwner(12, 3)).thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class,
                () -> service.getOrderDetailForShopOwner(12, 3));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(items, history, deliveries, payments);
    }

    @Test
    void usesOrderSnapshotsAndStoredNetReceivedWithOptionalDataAbsent() {
        User customer = new User();
        customer.setUserId(7);
        customer.setFullName("Customer");
        customer.setPhone("0901234567");
        Shop shop = new Shop();
        shop.setShopId(2);
        shop.setShopName("Shop");
        Order order = new Order();
        order.setOrderId(12);
        order.setCustomer(customer);
        order.setShop(shop);
        order.setSnapshotShippingAddress("Address at checkout");
        order.setFinalAmount(new BigDecimal("100000.00"));
        order.setShopNetReceived(new BigDecimal("85000.25"));
        order.setIsSettled(false);
        order.setPlatformFee(new BigDecimal("4250.00"));
        order.setIsCODRemitted(true);
        OrderItem item = new OrderItem();
        item.setOrderItemId(1);
        item.setSnapshotProductName("Product at checkout");
        item.setSnapshotVariantName("Variant at checkout");
        item.setQuantity(3);
        item.setPriceSnapshot(new BigDecimal("12500.50"));
        item.setCostSnapshot(new BigDecimal("8000.00"));
        // No current product/variant is needed to render the purchased item.
        when(orders.findDetailForShopOwner(12, 3)).thenReturn(Optional.of(order));
        when(items.findByOrderOrderIdOrderByOrderItemIdAsc(12)).thenReturn(List.of(item));
        when(history.findByOrderOrderIdOrderByCreatedAtAscHistoryIdAsc(12)).thenReturn(List.of());
        when(deliveries.findByOrderOrderId(12)).thenReturn(Optional.empty());
        when(payments.findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(12)).thenReturn(List.of());

        var result = service.getOrderDetailForShopOwner(12, 3);

        assertEquals(new BigDecimal("85000.25"), result.summary().shopNetReceived());
        assertEquals("Address at checkout", result.snapshotShippingAddress());
        assertEquals("Product at checkout", result.items().getFirst().productName());
        assertEquals("Variant at checkout", result.items().getFirst().variantName());
        assertEquals(new BigDecimal("37501.50"), result.items().getFirst().lineTotal());
        assertEquals(new BigDecimal("4250.00"), result.platformFee());
        assertEquals(Boolean.TRUE, result.isCODRemitted());
        assertEquals(new BigDecimal("8000.00"), result.items().getFirst().costSnapshot());
        assertNull(result.couponCode());
        assertNull(result.delivery());
        assertTrue(result.payments().isEmpty());
        assertTrue(result.statusHistory().isEmpty());
    }
}
