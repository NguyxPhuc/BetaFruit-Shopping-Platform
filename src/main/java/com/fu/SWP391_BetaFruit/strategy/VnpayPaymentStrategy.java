package com.fu.SWP391_BetaFruit.strategy;

import com.fu.SWP391_BetaFruit.config.VnPayConfig;
import com.fu.SWP391_BetaFruit.dto.request.PaymentRequest;
import com.fu.SWP391_BetaFruit.dto.response.PaymentResponse;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VnpayPaymentStrategy implements PaymentStrategy {

    @Autowired
    private VnPayConfig vnPayConfig;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.VNPAY;
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request,String txnRef) throws Exception {
        long amountForVnPay = request.getAmount().longValue() * 100;
        String paymentUrl = vnPayConfig.buildPaymentUrl(txnRef, amountForVnPay, request.getClientIpAddress(), request.getContextPath());
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setRequiresRedirect(true);
        paymentResponse.setPaymentUrl(paymentUrl);
        paymentResponse.setMessage("Chuyển hướng đến cổng thanh toán.");
        return paymentResponse;
    }
}
