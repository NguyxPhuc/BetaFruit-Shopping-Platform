package com.fu.SWP391_BetaFruit.strategy;


import com.fu.SWP391_BetaFruit.dto.request.PaymentRequest;
import com.fu.SWP391_BetaFruit.dto.response.PaymentResponse;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;

public interface PaymentStrategy {
    PaymentMethod getPaymentMethod();

    PaymentResponse processPayment(PaymentRequest request, String txnRef) throws Exception;
}
