package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.PaymentRequest;
import com.fu.SWP391_BetaFruit.dto.response.PaymentResponse;
import com.fu.SWP391_BetaFruit.dto.response.PaymentResultResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public interface PaymentService {
    @Transactional(rollbackFor = Exception.class)
    PaymentResponse createPayment(PaymentRequest request) throws Exception;

    @Transactional(rollbackFor = Exception.class)
    Map<String, String> processVnpayIpn(Map<String, String> params);

    PaymentResultResponse processVnpayReturn(Map<String, String> params);
}
