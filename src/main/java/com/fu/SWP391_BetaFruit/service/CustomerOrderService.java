package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.CustomerOrderResponse;
import com.fu.SWP391_BetaFruit.dto.response.TierUpgradeResult;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;

public interface CustomerOrderService {

    Page<CustomerOrderResponse> getCustomerOrders(Integer customerId, OrderStatus status, int page, int size);

    CustomerOrderResponse getOrderDetail(Integer orderId, Integer customerId);

    CustomerOrderResponse getOrderForReview(Integer orderId, Integer customerId);

    TierUpgradeResult confirmReceived(Integer orderId, Integer customerId);

    void autoConfirmReceived(Integer orderId, LocalDateTime deliveredCutoff);
}
