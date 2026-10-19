package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.config.VnPayConfig;
import com.fu.SWP391_BetaFruit.dto.request.PaymentRequest;
import com.fu.SWP391_BetaFruit.dto.response.PaymentResponse;
import com.fu.SWP391_BetaFruit.dto.response.PaymentResultResponse;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.entity.PaymentTransaction;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.enums.PaymentStatus;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.PaymentTransactionRepository;
import com.fu.SWP391_BetaFruit.service.PaymentService;
import com.fu.SWP391_BetaFruit.strategy.PaymentStrategy;
import com.fu.SWP391_BetaFruit.strategy.PaymentStrategyFactory;
import com.fu.SWP391_BetaFruit.utils.VnPayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {
    // Note 2.1: IPN và return dùng chung transaction trừ kho, ghi nhận thanh toán và xác nhận đơn.
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PaymentStrategyFactory paymentStrategyFactory;

    @Autowired
    private VnPayConfig vnPayConfig;

    @Autowired
    private VnpayPaymentCompletionService completion;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse createPayment(PaymentRequest request) throws Exception {
        String txnRef = VnPayUtils.generateTransactionRef(request.getPaymentMethod(), request.getReferenceId());
        Order orderRef = orderRepository.findByOrderId(request.getReferenceId())
                .orElseThrow(() -> new Exception("Không tìm thấy đơn hàng."));
        if (!OrderStatus.PENDING.equals(orderRef.getOrderStatus())) {
            throw new Exception("Đơn hàng này đã được thanh toán hoặc không còn hợp lệ để giao dịch.");
        }
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(orderRef);
        transaction.setGateway(request.getPaymentMethod().name());
        transaction.setTransactionRef(txnRef);
        transaction.setAmount(request.getAmount());
        transaction.setStatus(PaymentStatus.PENDING);
        paymentTransactionRepository.save(transaction);
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(request.getPaymentMethod());
        return strategy.processPayment(request, txnRef);
    }

    @Override
    public Map<String, String> processVnpayIpn(Map<String, String> params) {
        try {
            if (!vnPayConfig.verifySignature(params)) return ipn("97", "Invalid Checksum");
            var result = completion.complete(params.get("vnp_TxnRef"), paidAmount(params), "00".equals(params.get("vnp_ResponseCode")));
            return switch (result.code()) {
                case "01" -> ipn("01", "Order not found");
                case "02" -> ipn("02", "Order already confirmed");
                case "04" -> ipn("04", "Invalid Amount");
                default -> ipn("00", "Confirm Success");
            };
        } catch (Exception ex) {
            // Completion has already rolled back; do not acknowledge partial stock/payment changes.
            log.error("VNPay IPN could not complete order for reference {}", params.get("vnp_TxnRef"), ex);
            return ipn("99", "Order processing failed; retry or reconcile payment");
        }
    }

    @Override
    public PaymentResultResponse processVnpayReturn(Map<String, String> params) {
        BigDecimal amount = paidAmount(params);
        VnpayPaymentCompletionService.Result result = null;
        String failure = null;
        try {
            if (!vnPayConfig.verifySignature(params)) {
                failure = "Chữ ký bảo mật không hợp lệ.";
            } else {
                result = completion.complete(params.get("vnp_TxnRef"), amount, "00".equals(params.get("vnp_ResponseCode")));
                failure = switch (result.code()) {
                    case "01" -> "Không tìm thấy giao dịch của đơn hàng.";
                    case "04" -> "Số tiền thanh toán không khớp giao dịch.";
                    case "02" -> result.paid() ? null : "Giao dịch đã được xử lý nhưng chưa được ghi nhận thanh toán thành công.";
                    default -> result.paid() ? null : VnPayUtils.translateVnpayResponseCode(params.get("vnp_ResponseCode"));
                };
            }
        } catch (Exception ex) {
            log.error("VNPay return could not complete order for reference {}", params.get("vnp_TxnRef"), ex);
            failure = "Chưa thể xử lý đơn hàng hoặc cập nhật tồn kho. Nếu VNPay đã thu tiền, vui lòng liên hệ hỗ trợ để đối soát giao dịch.";
        }
        boolean successful = failure == null && result != null && result.paid();
        var response = new PaymentResultResponse();
        Integer orderId = result == null ? null : result.orderId();
        if (result != null && result.amount() != null) amount = result.amount();
        if (amount == null) amount = BigDecimal.ZERO;
        response.setSuccess(successful);
        response.setOrderId(orderId);
        response.setOrderCode(orderId == null ? "---" : String.format("DH%06d", orderId));
        response.setTransactionRef(params.get("vnp_TxnRef"));
        response.setGatewayTransactionNo(params.get("vnp_TransactionNo"));
        response.setBankCode(params.get("vnp_BankCode"));
        response.setAmount(amount);
        response.setFormattedAmount(String.format("%,d ₫", amount.longValue()));
        response.setPayDate(VnPayUtils.formatVnPayDate(params.get("vnp_PayDate")));
        response.setResponseCode(params.get("vnp_ResponseCode"));
        response.setMessage(successful ? "Thanh toán VNPay thành công. Đơn hàng đã được xác nhận và trừ tồn kho."
                : "Chưa thể hoàn tất xử lý thanh toán. " + failure);
        response.setFailureReason(failure);
        response.setCustomerName(result == null || result.customerName() == null ? "Quý khách hàng" : result.customerName());
        response.setCustomerPhone(result == null || result.customerPhone() == null ? "" : result.customerPhone());
        response.setShippingAddress(result == null || result.shippingAddress() == null ? "" : result.shippingAddress());
        response.setPaymentMethod(PaymentMethod.VNPAY.name());
        return response;
    }

    private BigDecimal paidAmount(Map<String, String> params) {
        try {
            return new BigDecimal(params.get("vnp_Amount")).movePointLeft(2);
        } catch (NumberFormatException | NullPointerException ex) {
            return null;
        }
    }

    private Map<String, String> ipn(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }
}
