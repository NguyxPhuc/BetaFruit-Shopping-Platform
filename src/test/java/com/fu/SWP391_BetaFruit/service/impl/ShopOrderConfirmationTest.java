package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShopOrderConfirmationTest {
    OrderRepository orders = mock(OrderRepository.class);
    OrderStatusHistoryRepository histories = mock(OrderStatusHistoryRepository.class);
    UserRepository users = mock(UserRepository.class);
    PaymentTransactionRepository payments = mock(PaymentTransactionRepository.class);
    OrderStockService stock = mock(OrderStockService.class);
    ShopOrderServiceImpl service = new ShopOrderServiceImpl(orders, histories, users,
            mock(OrderItemRepository.class), mock(DeliveryAssignmentRepository.class), payments, stock);
    Order order = new Order();

    @BeforeEach void setup() {
        order.setOrderId(1);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.COD);
        when(orders.findByOrderIdAndShopOwnerUserId(1, 7)).thenReturn(Optional.of(order));
    }

    @Test void codConfirmsWithoutCheckingPayment() {
        service.confirmOrder(1, 7);
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(stock).deduct(order, 7);
        verify(histories).save(any(OrderStatusHistory.class));
        verifyNoInteractions(payments);
    }

    @Test void unpaidVnpayCannotConfirm() {
        order.setPaymentMethod(PaymentMethod.VNPAY);
        for (PaymentStatus status : List.of(PaymentStatus.PENDING, PaymentStatus.FAILED)) {
            PaymentTransaction payment = new PaymentTransaction();
            payment.setGateway("VNPAY");
            payment.setStatus(status);
            when(payments.findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(1)).thenReturn(List.of(payment));
            assertThrows(ResponseStatusException.class, () -> service.confirmOrder(1, 7));
        }
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
        verifyNoInteractions(stock, histories);
    }

    @Test void missingPaymentOrPaidCodCannotAuthorizeVnpay() {
        order.setPaymentMethod(PaymentMethod.VNPAY);
        when(payments.findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(1)).thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> service.confirmOrder(1, 7));
        PaymentTransaction payment = new PaymentTransaction();
        payment.setGateway("COD");
        payment.setStatus(PaymentStatus.PAID);
        when(payments.findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(1)).thenReturn(List.of(payment));
        assertThrows(ResponseStatusException.class, () -> service.confirmOrder(1, 7));
        verifyNoInteractions(stock, histories);
    }

    @Test void paidVnpayCanConfirmEvenIfAnotherAttemptFailed() {
        order.setPaymentMethod(PaymentMethod.VNPAY);
        PaymentTransaction paid = new PaymentTransaction();
        paid.setGateway("VNPAY");
        paid.setStatus(PaymentStatus.PAID);
        PaymentTransaction failed = new PaymentTransaction();
        failed.setGateway("VNPAY");
        failed.setStatus(PaymentStatus.FAILED);
        when(payments.findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(1)).thenReturn(List.of(paid, failed));
        service.confirmOrder(1, 7);
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        verify(stock).deduct(order, 7);
    }

    @Test void insufficientStockKeepsOrderPending() {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Không đủ tồn kho"))
                .when(stock).deduct(order, 7);
        assertThrows(ResponseStatusException.class, () -> service.confirmOrder(1, 7));
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
        verifyNoInteractions(histories);
    }

    @Test void confirmingTwiceCannotDeductTwice() {
        service.confirmOrder(1, 7);
        assertThrows(ResponseStatusException.class, () -> service.confirmOrder(1, 7));
        verify(stock, times(1)).deduct(order, 7);
        verify(histories, times(1)).save(any(OrderStatusHistory.class));
    }
}
