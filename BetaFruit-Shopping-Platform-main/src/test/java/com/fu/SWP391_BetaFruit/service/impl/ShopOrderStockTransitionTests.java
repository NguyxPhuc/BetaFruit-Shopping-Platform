package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShopOrderStockTransitionTests {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);
    private final OrderStockService stock = mock(OrderStockService.class);
    private final ShopOrderServiceImpl service = new ShopOrderServiceImpl(orders, history,
            mock(UserRepository.class), mock(OrderItemRepository.class),
            mock(DeliveryAssignmentRepository.class), mock(PaymentTransactionRepository.class), stock);

    private Order order(OrderStatus status) {
        Order order = new Order();
        order.setOrderId(12);
        order.setOrderStatus(status);
        when(orders.findByOrderIdAndShopOwnerUserId(12, 7)).thenReturn(Optional.of(order));
        return order;
    }

    @Test void confirmThenCancelDeductsAndRestoresExactlyOnce() {
        Order order = order(OrderStatus.PENDING);
        service.confirmOrder(12, 7);
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        assertThrows(ResponseStatusException.class, () -> service.confirmOrder(12, 7));
        service.cancelOrder(12, 7);
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertThrows(ResponseStatusException.class, () -> service.cancelOrder(12, 7));
        verify(stock).deduct(order, 7);
        verify(stock).restore(order, 7);
        verifyNoMoreInteractions(stock);
        verify(history, times(2)).save(any());
    }

    @Test void cancellingPendingOrderDoesNotAddStock() {
        Order order = order(OrderStatus.PENDING);
        service.cancelOrder(12, 7);
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        verifyNoInteractions(stock);
    }

    @Test void preparingAndReadyDoNotDeductAgainAndCancellationRestores() {
        Order order = order(OrderStatus.CONFIRMED);
        service.startPreparingOrder(12, 7);
        verifyNoInteractions(stock);
        service.markOrderReady(12, 7);
        verifyNoInteractions(stock);
        service.cancelOrder(12, 7);
        verify(stock).restore(order, 7);
        verifyNoMoreInteractions(stock);
    }

    @Test void preparingOrderCanBeCancelledAndRestored() {
        Order order = order(OrderStatus.PREPARING);
        service.cancelOrder(12, 7);
        verify(stock).restore(order, 7);
    }

    @Test void shortageLeavesStatusAndHistoryUnchanged() {
        Order order = order(OrderStatus.PENDING);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Không đủ tồn kho"))
                .when(stock).deduct(order, 7);
        assertThrows(ResponseStatusException.class, () -> service.confirmOrder(12, 7));
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
        verifyNoInteractions(history);
    }

    @Test void stockRefundFailureLeavesOrderUncancelled() {
        Order order = order(OrderStatus.CONFIRMED);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT)).when(stock).restore(order, 7);
        assertThrows(ResponseStatusException.class, () -> service.cancelOrder(12, 7));
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verifyNoInteractions(history);
    }

    @Test void unavailableOrderAndDisallowedStatesCannotChangeStock() {
        when(orders.findByOrderIdAndShopOwnerUserId(12, 7)).thenReturn(Optional.empty());
        var missing = assertThrows(ResponseStatusException.class, () -> service.confirmOrder(12, 7));
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        for (var status : new OrderStatus[]{OrderStatus.PICKED_UP, OrderStatus.DELIVERING,
                OrderStatus.DELIVERED, OrderStatus.SUCCESS, OrderStatus.FAILED, OrderStatus.CANCELLED, OrderStatus.REJECTED}) {
            order(status);
            assertThrows(ResponseStatusException.class, () -> service.confirmOrder(12, 7));
            assertThrows(ResponseStatusException.class, () -> service.cancelOrder(12, 7));
        }
        verifyNoInteractions(stock, history);
    }
}
