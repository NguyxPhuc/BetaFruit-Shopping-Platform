package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.PaymentRequest;
import com.fu.SWP391_BetaFruit.dto.response.PaymentResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckoutPaymentServiceTest {
    OrderRepository orders = mock(OrderRepository.class);
    PaymentTransactionRepository transactions = mock(PaymentTransactionRepository.class);
    PaymentService gateway = mock(PaymentService.class);
    CheckoutPaymentService service = new CheckoutPaymentService(orders, transactions, gateway);
    Order order = new Order();

    @BeforeEach void setup() throws Exception {
        User customer = new User(); customer.setUserId(7);
        order.setOrderId(101); order.setCustomer(customer);
        order.setPaymentMethod(PaymentMethod.VNPAY); order.setOrderStatus(OrderStatus.PENDING);
        order.setFinalAmount(new BigDecimal("123456"));
        when(orders.findLockedById(101)).thenReturn(Optional.of(order));
        when(orders.findByOrderId(101)).thenReturn(Optional.of(order));
        when(gateway.createPayment(any())).thenReturn(new PaymentResponse(true, "https://sandbox.vnpayment.vn/test", ""));
    }

    @Test void usesStoredAmountAndDoesNotConfirmOrder() throws Exception {
        assertEquals("https://sandbox.vnpayment.vn/test", service.start(7, 101, "127.0.0.1", "http://localhost:8080"));
        var request = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(gateway).createPayment(request.capture());
        assertEquals(order.getFinalAmount(), request.getValue().getAmount());
        assertEquals(PaymentMethod.VNPAY, request.getValue().getPaymentMethod());
        assertEquals("http://localhost:8080", request.getValue().getContextPath());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
    }

    @Test void foreignCustomerCodAndCancelledOrderCannotPay() {
        assertThrows(IllegalArgumentException.class, () -> service.start(8, 101, "", ""));
        order.setPaymentMethod(PaymentMethod.COD);
        assertThrows(IllegalArgumentException.class, () -> service.start(7, 101, "", ""));
        order.setPaymentMethod(PaymentMethod.VNPAY); order.setOrderStatus(OrderStatus.CANCELLED);
        assertThrows(IllegalArgumentException.class, () -> service.start(7, 101, "", ""));
        verifyNoInteractions(gateway);
    }

    @Test void paidOrderCannotBeChargedAgainAndReceiptShowsPaid() {
        PaymentTransaction paid = new PaymentTransaction(); paid.setGateway("VNPAY"); paid.setStatus(PaymentStatus.PAID);
        when(transactions.findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(101)).thenReturn(List.of(paid));
        assertThrows(IllegalArgumentException.class, () -> service.start(7, 101, "", ""));
        assertEquals(new CheckoutPaymentService.State(true, false), service.states(7, List.of(101)).get(101));
        verifyNoInteractions(gateway);
    }
}
