package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;

/** Cash collection, COD remittance and shop settlement are separate facts. */
public final class OrderPaymentRules {
    private OrderPaymentRules() {}

    public static BigDecimal codAmount(Order order) {
        if (order.getPaymentMethod() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng thiếu phương thức thanh toán");
        }
        if (order.getPaymentMethod() != PaymentMethod.COD) return BigDecimal.ZERO;
        return OrderPricing.customerAmount(order);
    }

    public static boolean delivered(Order order) {
        return order.getOrderStatus() == OrderStatus.DELIVERED || order.getOrderStatus() == OrderStatus.SUCCESS;
    }

    public static boolean awaitingDelivery(Order order) {
        return order.getOrderStatus() == OrderStatus.READY
                || order.getOrderStatus() == OrderStatus.PICKED_UP
                || order.getOrderStatus() == OrderStatus.DELIVERING;
    }

    public static String remittanceStatus(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.COD) return "NOT_APPLICABLE";
        if (Boolean.TRUE.equals(order.getIsCodRemitted())) return "REMITTED";
        if (delivered(order)) return "PENDING_REMITTANCE";
        return "NOT_COLLECTED";
    }

    public static BigDecimal amountToCollect(Order order) {
        BigDecimal amount = codAmount(order);
        return awaitingDelivery(order) && !Boolean.TRUE.equals(order.getIsCodRemitted())
                ? amount : BigDecimal.ZERO;
    }

    public static BigDecimal codHeldAmount(Order order) {
        BigDecimal amount = codAmount(order);
        return delivered(order) && !Boolean.TRUE.equals(order.getIsCodRemitted())
                ? amount : BigDecimal.ZERO;
    }
}
