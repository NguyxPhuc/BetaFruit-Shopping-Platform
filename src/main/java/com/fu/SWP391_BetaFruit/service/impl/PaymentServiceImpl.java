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
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PaymentStrategyFactory paymentStrategyFactory;

    @Autowired
    private VnPayConfig vnPayConfig;

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
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> processVnpayIpn(Map<String, String> params) {
        Map<String, String> response = new HashMap<>();
        try {
            if (!vnPayConfig.verifySignature(params)) {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return response;
            }
            String txnRef = params.get("vnp_TxnRef");
            PaymentTransaction transaction = paymentTransactionRepository.findByTransactionRef(txnRef)
                    .orElse(null);
            if (transaction == null) {
                response.put("RspCode", "01");
                response.put("Message", "Order not found");
                return response;
            }
            if (!PaymentStatus.PENDING.equals(transaction.getStatus())) {
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return response;
            }
            long vnpAmount = Long.parseLong(params.get("vnp_Amount")) / 100;
            if (vnpAmount != transaction.getAmount().longValue()) {
                response.put("RspCode", "04");
                response.put("Message", "Invalid Amount");
                return response;
            }
            String responseCode = params.get("vnp_ResponseCode");
            Order order = transaction.getOrder();
            if ("00".equals(responseCode)) {
                transaction.setStatus(PaymentStatus.PAID);
                // Cập nhật trạng thái đơn hàng
                order.setOrderStatus(OrderStatus.CONFIRMED);
            } else {
                transaction.setStatus(PaymentStatus.FAILED);
            }
            paymentTransactionRepository.save(transaction);
            orderRepository.save(order);
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
        } catch (Exception e) {
            response.put("RspCode", "99");
            response.put("Message", "Unknown Error");
        }
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResultResponse processVnpayReturn(Map<String, String> params) {
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String payDateRaw = params.get("vnp_PayDate");
        BigDecimal amount = BigDecimal.ZERO;
        try {
            if (params.containsKey("vnp_Amount") && params.get("vnp_Amount") != null) {
                amount = new BigDecimal(params.get("vnp_Amount")).divide(new BigDecimal(100));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean isValidSignature = false;
        try {
            isValidSignature = vnPayConfig.verifySignature(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean isSuccess = isValidSignature && "00".equals(responseCode);
        String failureReason = null;
        String friendlyMessage;
        if (!isValidSignature) {
            isSuccess = false;
            failureReason = "Chữ ký bảo mật không hợp lệ.";
            friendlyMessage = "Giao dịch không an toàn. Xin vui lòng không tiếp tục giao dịch.";
        } else if (isSuccess) {
            friendlyMessage = "Giao dịch thanh toán trực tuyến qua cổng VNPay đã hoàn tất thành công.";
        } else {
            failureReason = VnPayUtils.translateVnpayResponseCode(responseCode);
            friendlyMessage = "Thanh toán chưa hoàn tất. " + failureReason;
        }

        Integer orderId = null;
        String orderCode = txnRef != null ? txnRef : "---";
        String customerName = "Quý khách hàng";
        String customerPhone = "";
        String shippingAddress = "";
        PaymentTransaction transaction = null;
        if (txnRef != null) {
            transaction = paymentTransactionRepository.findByTransactionRef(txnRef).orElse(null);
        }
        if (transaction != null) {
            Order order = transaction.getOrder();
            if (order != null) {
                orderId = order.getOrderId();
                orderCode = String.format("DH%06d", orderId);
                shippingAddress = order.getSnapshotShippingAddress() != null ? order.getSnapshotShippingAddress() : "";
                if (order.getCustomer() != null) {
                    customerName = order.getCustomer().getFullName() != null ? order.getCustomer().getFullName()
                            : customerName;
                    customerPhone = order.getCustomer().getPhone() != null ? order.getCustomer().getPhone() : "";
                }
            }
            if (PaymentStatus.PENDING.equals(transaction.getStatus())) {
                if (isSuccess) {
                    transaction.setStatus(PaymentStatus.PAID);
                    if (order != null)
                        order.setOrderStatus(OrderStatus.CONFIRMED);
                } else {
                    transaction.setStatus(PaymentStatus.FAILED);
                }
                paymentTransactionRepository.save(transaction);
                if (order != null) {
                    orderRepository.save(order);
                }
            } else if (PaymentStatus.PAID.equals(transaction.getStatus())) {
                isSuccess = true;
            }
            if (amount.compareTo(java.math.BigDecimal.ZERO) == 0 && transaction.getAmount() != null) {
                amount = transaction.getAmount();
            }
        }
        PaymentResultResponse paymentResultResponse = new PaymentResultResponse();
        paymentResultResponse.setSuccess(isSuccess);
        paymentResultResponse.setOrderId(orderId);
        paymentResultResponse.setOrderCode(orderCode);
        paymentResultResponse.setTransactionRef(txnRef);
        paymentResultResponse.setGatewayTransactionNo(transactionNo);
        paymentResultResponse.setBankCode(bankCode);
        paymentResultResponse.setAmount(amount);
        paymentResultResponse.setFormattedAmount(String.format("%,d ₫", amount.longValue()));
        paymentResultResponse.setPayDate(VnPayUtils.formatVnPayDate(payDateRaw));
        paymentResultResponse.setResponseCode(responseCode);
        paymentResultResponse.setMessage(friendlyMessage);
        paymentResultResponse.setFailureReason(failureReason);
        paymentResultResponse.setCustomerName(customerName);
        paymentResultResponse.setCustomerPhone(customerPhone);
        paymentResultResponse.setShippingAddress(shippingAddress);
        paymentResultResponse.setPaymentMethod(PaymentMethod.VNPAY.name());
        return paymentResultResponse;
    }

}
