package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.entity.OrderStatusHistory;
import com.fu.SWP391_BetaFruit.entity.PaymentTransaction;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.enums.PaymentStatus;
import com.fu.SWP391_BetaFruit.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;

/** Both authenticated VNPay callbacks use this transaction; lock order before payment and stock. */
@Service
public class VnpayPaymentCompletionService {
    private final OrderRepository orders;
    private final PaymentTransactionRepository payments;
    private final OrderStatusHistoryRepository history;
    private final OrderStockService stock;

    public VnpayPaymentCompletionService(OrderRepository orders, PaymentTransactionRepository payments,
            OrderStatusHistoryRepository history, OrderStockService stock) {
        this.orders = orders; this.payments = payments; this.history = history; this.stock = stock;
    }

    public record Result(String code, boolean paid, Integer orderId, BigDecimal amount,
                         String customerName, String customerPhone, String shippingAddress) { }

    @Transactional
    public Result complete(String reference, BigDecimal amount, boolean successful) {
        Integer orderId = payments.findOrderIdByTransactionRef(reference).orElse(null);
        if (orderId == null) return new Result("01", false, null, amount, null, null, null);
        Order order = orders.findLockedById(orderId).orElseThrow();
        PaymentTransaction payment = payments.findLockedByTransactionRef(reference).orElseThrow();
        if (order.getPaymentMethod() != PaymentMethod.VNPAY || !"VNPAY".equals(payment.getGateway())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch không thuộc đơn VNPay");
        }
        if (amount == null || payment.getAmount() == null || amount.compareTo(payment.getAmount()) != 0) {
            return result("04", false, order, payment);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return result("02", payment.getStatus() == PaymentStatus.PAID, order, payment);
        }
        if (successful) {
            if (order.getOrderStatus() == OrderStatus.PENDING) {
                OrderPricing.couponSource(order);
                stock.deduct(order, order.getCustomer().getUserId());
                order.setOrderStatus(OrderStatus.CONFIRMED);
                history.save(new OrderStatusHistory(null, order, order.getCustomer(), OrderStatus.PENDING,
                        OrderStatus.CONFIRMED, "VNPay thanh toán thành công, đã trừ tồn kho", null));
            } else if (order.getOrderStatus() != OrderStatus.CONFIRMED
                    && order.getOrderStatus() != OrderStatus.PREPARING
                    && order.getOrderStatus() != OrderStatus.READY) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Không thể xác nhận thanh toán cho trạng thái đơn hiện tại; cần đối soát giao dịch");
            }
            // Shop may have confirmed/deducted first. Never deduct again or move its state backwards.
            payment.setStatus(PaymentStatus.PAID);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }
        return result("00", successful, order, payment);
    }

    private Result result(String code, boolean paid, Order order, PaymentTransaction payment) {
        var customer = order.getCustomer();
        return new Result(code, paid, order.getOrderId(), payment.getAmount(),
                customer == null ? null : customer.getFullName(), customer == null ? null : customer.getPhone(),
                order.getSnapshotShippingAddress());
    }
}
