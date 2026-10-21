package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.config.VnPayConfig;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentOrderStatusTest {
    PaymentServiceImpl service = new PaymentServiceImpl();
    PaymentTransactionRepository payments = mock(PaymentTransactionRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    VnPayConfig config = mock(VnPayConfig.class);
    Order order = new Order();
    PaymentTransaction payment = new PaymentTransaction();
    Map<String, String> params = Map.of("vnp_TxnRef", "VNPAY-1", "vnp_Amount", "10000000",
            "vnp_ResponseCode", "00", "vnp_TransactionStatus", "00");

    @BeforeEach void setup() throws Exception {
        ReflectionTestUtils.setField(service, "orderRepository", orders);
        ReflectionTestUtils.setField(service, "paymentTransactionRepository", payments);
        ReflectionTestUtils.setField(service, "vnPayConfig", config);
        order.setOrderId(1);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setOrder(order);
        payment.setGateway("VNPAY");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(new BigDecimal("100000"));
        when(payments.findByTransactionRef("VNPAY-1")).thenReturn(Optional.of(payment));
        when(config.verifySignature(params)).thenReturn(true);
    }

    @Test void ipnMarksPaidAndKeepsOrderPending() {
        assertEquals("00", service.processVnpayIpn(params).get("RspCode"));
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
    }

    @Test void returnMarksPaidAndKeepsOrderPending() {
        assertTrue(service.processVnpayReturn(params).isSuccess());
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
    }

    @Test void callbacksAfterShopConfirmationDoNotChangeOrderStatus() {
        service.processVnpayReturn(params);
        order.setOrderStatus(OrderStatus.CONFIRMED);
        assertEquals("02", service.processVnpayIpn(params).get("RspCode"));
        assertTrue(service.processVnpayReturn(params).isSuccess());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
    }

    @Test void invalidIpnSignatureDoesNotChangeTransactionOrOrder() throws Exception {
        when(config.verifySignature(params)).thenReturn(false);
        assertEquals("97", service.processVnpayIpn(params).get("RspCode"));
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
        verify(payments, never()).save(any());
    }

    @Test void wrongIpnAmountCannotMarkPaid() {
        payment.setAmount(new BigDecimal("200000"));
        assertEquals("04", service.processVnpayIpn(params).get("RspCode"));
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
    }

    @Test void cancelledReturnKeepsOrderPending() throws Exception {
        var failed = new java.util.HashMap<>(params);
        failed.put("vnp_ResponseCode", "24");
        when(config.verifySignature(failed)).thenReturn(true);
        assertFalse(service.processVnpayReturn(failed).isSuccess());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
    }
}
