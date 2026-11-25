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

    public record State(boolean paid, boolean canPay, String statusLabel) {}

    /**
     * Tác dụng: Đọc các giao dịch VNPAY của từng đơn thuộc khách, trả nhãn thanh toán và cờ canPay để màn
     * checkout-success bật/tắt nút thanh toán.
     * Màn hình / luồng: Màn checkout-success và thao tác thanh toán lại từ đơn hàng khách;
     * CheckoutController/CustomerOrderController gọi.
     */
    @Transactional(readOnly = true)
    public Map<Integer, State> states(Integer customerId, List<Integer> orderIds) {
        Map<Integer, State> result = new LinkedHashMap<>();
        for (Integer id : orderIds) {
            Order order = owned(orders.findByOrderId(id).orElseThrow(
                    () -> new IllegalArgumentException("Không tìm thấy đơn hàng.")), customerId);
            List<com.fu.SWP391_BetaFruit.entity.PaymentTransaction> orderTransactions =
                    transactions.findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(id).stream()
                            .filter(tx -> "VNPAY".equalsIgnoreCase(tx.getGateway())).toList();
            boolean paid = orderTransactions.stream().anyMatch(tx -> tx.getStatus() == PaymentStatus.PAID);
            boolean canPay = order.getPaymentMethod() == PaymentMethod.VNPAY
                    && order.getOrderStatus() == OrderStatus.PENDING && !paid;
            PaymentStatus latestStatus = orderTransactions.isEmpty() ? null
                    : orderTransactions.getLast().getStatus();
            String statusLabel = paid ? "Đã thanh toán"
                    : canPay && latestStatus != PaymentStatus.FAILED ? "Chưa thanh toán"
                    : latestStatus == PaymentStatus.FAILED ? "Thanh toán thất bại"
                    : canPay ? "Chưa thanh toán" : "Chưa hoàn tất thanh toán";
            result.put(id, new State(paid, canPay, statusLabel));
        }
        return result;
    }

    /**
     * Tác dụng: Khóa đơn thuộc khách, chỉ cho thanh toán VNPAY khi còn PENDING và chưa PAID; gọi
     * PaymentService.createPayment bằng số tiền đã lưu để lấy URL chuyển sang VNPAY.
     * Màn hình / luồng: Màn checkout-success và thao tác thanh toán lại từ đơn hàng khách;
     * CheckoutController/CustomerOrderController gọi.
     */
    @Transactional(rollbackFor = Exception.class)
    public String start(Integer customerId, Integer orderId, String ipAddress, String baseUrl) throws Exception {
        Order order = owned(orders.findLockedById(orderId).orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy đơn hàng.")), customerId);
        if (order.getPaymentMethod() != PaymentMethod.VNPAY || order.getOrderStatus() != OrderStatus.PENDING)
            throw new IllegalArgumentException("Đơn hàng không còn hợp lệ để thanh toán VNPAY.");
        if (paid(order)) throw new IllegalArgumentException("Đơn hàng đã được thanh toán thành công.");
        // The amount and method come from the stored order, never from the browser.
        var response = payments.createPayment(new PaymentRequest(orderId, OrderPricing.customerAmount(order),
                PaymentMethod.VNPAY, ipAddress, baseUrl));
        if (!response.isRequiresRedirect() || response.getPaymentUrl() == null || response.getPaymentUrl().isBlank())
            throw new IllegalStateException("Không tạo được liên kết thanh toán VNPAY.");
        return response.getPaymentUrl();
    }

    /**
     * Tác dụng: Hàm nội bộ của states/start: kiểm tra đơn thuộc khách hiện tại trước khi đọc trạng thái
     * hoặc tạo thanh toán.
     * Màn hình / luồng: Màn checkout-success và thao tác thanh toán lại từ đơn hàng khách;
     * CheckoutController/CustomerOrderController gọi.
     */
    private Order owned(Order order, Integer customerId) {
        if (customerId == null || order.getCustomer() == null
                || !Objects.equals(order.getCustomer().getUserId(), customerId))
            throw new IllegalArgumentException("Không tìm thấy đơn hàng của bạn.");
        return order;
    }

    /**
     * Tác dụng: Kiểm tra đơn đã có giao dịch VNPAY PAID chưa; start dùng để chặn thanh toán lại đơn đã trả tiền.
     * Màn hình / luồng: Màn checkout-success và thao tác thanh toán lại từ đơn hàng khách;
     * CheckoutController/CustomerOrderController gọi.
     */
    private boolean paid(Order order) {
        return transactions.findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(order.getOrderId()).stream()
                .anyMatch(tx -> "VNPAY".equalsIgnoreCase(tx.getGateway()) && tx.getStatus() == PaymentStatus.PAID);
    }
}
