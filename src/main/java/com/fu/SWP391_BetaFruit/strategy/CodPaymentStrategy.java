package com.fu.SWP391_BetaFruit.strategy;

import com.fu.SWP391_BetaFruit.dto.request.PaymentRequest;
import com.fu.SWP391_BetaFruit.dto.response.PaymentResponse;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class CodPaymentStrategy implements PaymentStrategy {
    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.COD;
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request, String txnRef) throws Exception {
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setRequiresRedirect(false);
        paymentResponse.setMessage("Đặt hàng thành công. Quý khách sẽ thanh toán khi nhận hàng.");
        return paymentResponse;
    }
}
