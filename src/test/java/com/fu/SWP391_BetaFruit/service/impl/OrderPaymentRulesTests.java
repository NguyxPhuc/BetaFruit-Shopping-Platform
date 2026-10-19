package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class OrderPaymentRulesTests {
    Order order(PaymentMethod method, OrderStatus status) {
        Order order = new Order();
        order.setPaymentMethod(method);
        order.setOrderStatus(status);
        order.setFinalAmount(new BigDecimal("115000"));
        order.setTotalAmount(new BigDecimal("100000"));
        order.setShopNetReceived(new BigDecimal("85000"));
        return order;
    }

    @Test void codCollectsFinalCustomerTotalRatherThanProductPriceOrShopNet() {
        Order order = order(PaymentMethod.COD, OrderStatus.DELIVERING);
        assertEquals(new BigDecimal("115000"), OrderPaymentRules.amountToCollect(order));
        assertEquals(BigDecimal.ZERO, OrderPaymentRules.codHeldAmount(order));
        assertEquals("NOT_COLLECTED", OrderPaymentRules.remittanceStatus(order));
    }

    @Test void onlineOrdersNeverRequireCashOrRemittanceEvenIfFlagIsFalse() {
        for (var status : OrderStatus.values()) {
            Order order = order(PaymentMethod.VNPAY, status);
            assertEquals(BigDecimal.ZERO, OrderPaymentRules.amountToCollect(order));
            assertEquals(BigDecimal.ZERO, OrderPaymentRules.codHeldAmount(order));
            assertEquals("NOT_APPLICABLE", OrderPaymentRules.remittanceStatus(order));
        }
    }

    @Test void deliveredCodIsHeldUntilRemittedIndependentlyOfShopSettlement() {
        for (var status : new OrderStatus[]{OrderStatus.DELIVERED, OrderStatus.SUCCESS}) {
            Order order = order(PaymentMethod.COD, status);
            order.setIsSettled(true);
            order.setSettledAt(LocalDateTime.of(2026, 10, 15, 12, 0));
            order.setIsCodRemitted(null); // Legacy null is not evidence of remittance.
            assertEquals(BigDecimal.ZERO, OrderPaymentRules.amountToCollect(order));
            assertEquals(new BigDecimal("115000"), OrderPaymentRules.codHeldAmount(order));
            assertEquals("PENDING_REMITTANCE", OrderPaymentRules.remittanceStatus(order));
            order.setIsCodRemitted(true);
            assertEquals(BigDecimal.ZERO, OrderPaymentRules.codHeldAmount(order));
            assertEquals("REMITTED", OrderPaymentRules.remittanceStatus(order));
            assertTrue(order.getIsSettled());
        }
    }

    @Test void failedCancelledAndUnacceptedOrdersDoNotAskForCash() {
        for (var status : new OrderStatus[]{OrderStatus.PENDING, OrderStatus.CONFIRMED,
                OrderStatus.PREPARING, OrderStatus.FAILED, OrderStatus.CANCELLED, OrderStatus.REJECTED}) {
            Order order = order(PaymentMethod.COD, status);
            assertEquals(BigDecimal.ZERO, OrderPaymentRules.amountToCollect(order));
            assertEquals(BigDecimal.ZERO, OrderPaymentRules.codHeldAmount(order));
        }
    }

    @Test void invalidPaymentDataDoesNotSilentlyBecomeAnOnlineOrder() {
        Order order = order(null, OrderStatus.DELIVERING);
        assertThrows(ResponseStatusException.class, () -> OrderPaymentRules.amountToCollect(order));
        order.setPaymentMethod(PaymentMethod.COD);
        order.setFinalAmount(null);
        assertThrows(ResponseStatusException.class, () -> OrderPaymentRules.codAmount(order));
        order.setFinalAmount(BigDecimal.ONE.negate());
        assertThrows(ResponseStatusException.class, () -> OrderPaymentRules.codAmount(order));
    }
}
