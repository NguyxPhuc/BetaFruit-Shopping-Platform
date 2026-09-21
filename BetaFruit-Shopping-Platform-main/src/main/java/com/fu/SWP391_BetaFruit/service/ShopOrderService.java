package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.ShopOrderListItemResponse;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import org.springframework.data.domain.Page;

public interface ShopOrderService {
    Page<ShopOrderListItemResponse> getOrdersForShopOwner(
            Integer ownerId, String keyword, OrderStatus status, int page, int size);
}
