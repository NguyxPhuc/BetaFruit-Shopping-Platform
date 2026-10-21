package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.PaymentRequest;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.PaymentTransactionRepository;
import com.fu.SWP391_BetaFruit.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CheckoutPaymentService {
    private final OrderRepository orders;
    private final PaymentTransactionRepository transactions;
    private final PaymentService payments;

    public record State(boolean paid, boolean canPay) {}

    @Transactional(readOnly = true)
    public Map<Integer, State> states(Integer customerId, List<Integer> orderIds) {
        Map<Integer, State> result = new LinkedHashMap<>();
        for (Integer id : orderIds) {
            Order order = owned(orders.findByOrderId(id).orElseThrow(
                    () -> new IllegalArgumentException("Không tìm thấy đơn hàng.")), customerId);
            boolean paid = paid(order);
            result.put(id, new State(paid, order.getPaymentMethod() == PaymentMethod.VNPAY
                    && order.getOrderStatus() == OrderStatus.PENDING && !paid));
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public String start(Integer customerId, Integer orderId, String ipAddress, String baseUrl) throws Exception {
        Order order = owned(orders.findLockedById(orderId).orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy đơn hàng.")), customerId);
        if (order.getPaymentMethod() != PaymentMethod.VNPAY || order.getOrderStatus() != OrderStatus.PENDING)
            throw new IllegalArgumentException("Đơn hàng không còn hợp lệ để thanh toán VNPAY.");
        if (paid(order)) throw new IllegalArgumentException("Đơn hàng đã được thanh toán thành công.");
        // The amount and method come from the stored order, never from the browser.
        var response = payments.createPayment(new PaymentRequest(orderId, order.getFinalAmount(),
                PaymentMethod.VNPAY, ipAddress, baseUrl));
        if (!response.isRequiresRedirect() || response.getPaymentUrl() == null || response.getPaymentUrl().isBlank())
            throw new IllegalStateException("Không tạo được liên kết thanh toán VNPAY.");
        return response.getPaymentUrl();
    }

    private Order owned(Order order, Integer customerId) {
        if (customerId == null || order.getCustomer() == null
                || !Objects.equals(order.getCustomer().getUserId(), customerId))
            throw new IllegalArgumentException("Không tìm thấy đơn hàng của bạn.");
        return order;
    }

    private boolean paid(Order order) {
        return transactions.findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(order.getOrderId()).stream()
                .anyMatch(tx -> "VNPAY".equalsIgnoreCase(tx.getGateway()) && tx.getStatus() == PaymentStatus.PAID);
    }
}
