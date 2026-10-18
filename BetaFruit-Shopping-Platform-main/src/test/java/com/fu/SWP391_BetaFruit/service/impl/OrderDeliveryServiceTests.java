package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderDeliveryServiceTests {
    final OrderRepository orders = mock(OrderRepository.class);
    final DeliveryAssignmentRepository assignments = mock(DeliveryAssignmentRepository.class);
    final OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);
    final OrderDeliveryService service = new OrderDeliveryService(orders, assignments, history, mock(UserRepository.class));

    Order fixture(OrderStatus status) {
        Shop shop = new Shop();
        shop.setShopId(2);
        shop.setAvailableBalance(new BigDecimal("100"));
        shop.setCommissionRate(new BigDecimal("5"));
        Order order = new Order();
        order.setOrderStatus(status);
        order.setShop(shop);
        order.setTotalAmount(new BigDecimal("1000"));
        order.setFinalAmount(new BigDecimal("1200"));
        order.setPaymentMethod(PaymentMethod.COD);
        Shipper shipper = new Shipper();
        shipper.setUserId(7);
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setShipper(shipper);
        when(orders.findLockedById(12)).thenReturn(Optional.of(order));
        when(assignments.findByOrderOrderId(12)).thenReturn(Optional.of(assignment));
        return order;
    }

    @Test void deliveryHoldsMoneyAndRepeatedConfirmationDoesNotResetTimer() {
        Order order = fixture(OrderStatus.DELIVERING);
        service.markDelivered(12, 7, true);
        assertEquals(OrderStatus.DELIVERED, order.getOrderStatus());
        assertEquals(new BigDecimal("950.00"), order.getShopNetReceived());
        assertEquals(new BigDecimal("100"), order.getShop().getAvailableBalance());
        assertFalse(order.getIsSettled());
        assertFalse(order.getIsCODRemitted());
        assertNull(order.getSettledAt());
        service.markDelivered(12, 7, true);
        verify(history, times(1)).save(any());
    }

    @Test void anotherShipperCannotConfirmDelivery() {
        Order order = fixture(OrderStatus.DELIVERING);
        assertThrows(ResponseStatusException.class, () -> service.markDelivered(12, 8, true));
        assertEquals(OrderStatus.DELIVERING, order.getOrderStatus());
        verifyNoInteractions(history);
    }

    @Test void onlineDeliveryDoesNotChangeRemittanceOrCreditShopBalance() {
        Order order = fixture(OrderStatus.DELIVERING);
        order.setPaymentMethod(PaymentMethod.ONLINE);
        order.setIsCODRemitted(null);
        service.markDelivered(12, 7, true);
        assertNull(order.getIsCODRemitted());
        assertFalse(order.getIsSettled());
        assertNull(order.getSettledAt());
        assertEquals(new BigDecimal("100"), order.getShop().getAvailableBalance());
    }

    @Test void invalidCODAmountAndPrematureRemittanceCannotConfirmDelivery() {
        Order order = fixture(OrderStatus.DELIVERING);
        order.setFinalAmount(null);
        assertThrows(ResponseStatusException.class, () -> service.markDelivered(12, 7, true));
        order.setFinalAmount(BigDecimal.TEN);
        order.setIsCODRemitted(true);
        assertThrows(ResponseStatusException.class, () -> service.markDelivered(12, 7, true));
        assertEquals(OrderStatus.DELIVERING, order.getOrderStatus());
        verifyNoInteractions(history);
    }

    @Test void onlyDeliveringOrdersCanBeDelivered() {
        for (OrderStatus status : OrderStatus.values()) {
            if (status == OrderStatus.DELIVERING || status == OrderStatus.DELIVERED) continue;
            Order order = fixture(status);
            assertThrows(ResponseStatusException.class, () -> service.markDelivered(12, 7, true));
            assertEquals(status, order.getOrderStatus());
        }
        verifyNoInteractions(history);
    }

    @Test void fullDeliveryFlowNeverCreditsShopAndCannotSkipOrReverseSteps() {
        Order order = fixture(OrderStatus.READY);
        assertThrows(ResponseStatusException.class, () -> service.startDelivering(12, 7));
        assertThrows(ResponseStatusException.class, () -> service.markDelivered(12, 7, true));
        service.markPickedUp(12, 7);
        assertEquals(OrderStatus.PICKED_UP, order.getOrderStatus());
        assertThrows(ResponseStatusException.class, () -> service.markPickedUp(12, 7));
        service.startDelivering(12, 7);
        assertEquals(OrderStatus.DELIVERING, order.getOrderStatus());
        assertThrows(ResponseStatusException.class, () -> service.markPickedUp(12, 7));
        assertThrows(ResponseStatusException.class, () -> service.markDelivered(12, 7, false));
        assertEquals(OrderStatus.DELIVERING, order.getOrderStatus());
        service.markDelivered(12, 7, true);
        assertEquals(OrderStatus.DELIVERED, order.getOrderStatus());
        assertEquals(new BigDecimal("100"), order.getShop().getAvailableBalance());
        assertFalse(order.getIsSettled());
        assertFalse(order.getIsCODRemitted());
        assertNull(order.getSettledAt());
        verify(history, times(3)).save(any());
    }

    @Test void eachStepChecksAssignmentAndRejectsOtherStates() {
        for (var status : OrderStatus.values()) {
            fixture(status);
            assertThrows(ResponseStatusException.class, () -> service.markPickedUp(12, 8));
            assertThrows(ResponseStatusException.class, () -> service.startDelivering(12, 8));
            if (status != OrderStatus.READY) {
                assertThrows(ResponseStatusException.class, () -> service.markPickedUp(12, 7));
            }
            if (status != OrderStatus.PICKED_UP) {
                assertThrows(ResponseStatusException.class, () -> service.startDelivering(12, 7));
            }
        }
        verifyNoInteractions(history);
    }
}
