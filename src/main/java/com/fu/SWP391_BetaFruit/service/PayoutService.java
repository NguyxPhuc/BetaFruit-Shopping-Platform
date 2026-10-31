package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.PayoutCreateRequest;
import com.fu.SWP391_BetaFruit.dto.request.ProcessPayoutRequest;
import com.fu.SWP391_BetaFruit.dto.response.PayoutDetailResponse;
import com.fu.SWP391_BetaFruit.dto.response.PayoutResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopBalanceResponse;
import com.fu.SWP391_BetaFruit.enums.PayoutStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PayoutService {
    ShopBalanceResponse getShopBalance(Integer userId) throws Exception;

    void requestPayout(Integer userId, PayoutCreateRequest request) throws Exception;

    List<PayoutResponse> getAllPayoutRequestsByShop(Integer userId) throws Exception;

    PayoutDetailResponse getPayoutDetailByShop(Integer userId, Integer requestId) throws Exception;

    @Transactional(readOnly = true)
    List<PayoutResponse> getAllPayoutRequests(Integer id) throws Exception;

    @Transactional(readOnly = true)
    PayoutDetailResponse getPayoutDetail(Integer requestId, Integer userId) throws Exception;

    @Transactional(readOnly = true)
    List<PayoutResponse> getPayoutRequestsByStatus(PayoutStatus status);

    void processPayout(Integer requestId, Integer userId, ProcessPayoutRequest processPayoutRequest) throws Exception;
}
