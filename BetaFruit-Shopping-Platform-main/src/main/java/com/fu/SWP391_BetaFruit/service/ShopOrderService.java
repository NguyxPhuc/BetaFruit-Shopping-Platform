package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.ShopOrderListItemResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopOrderDetailResponse;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import org.springframework.data.domain.Page;

public interface ShopOrderService {
    ShopOrderDetailResponse getOrderDetailForShopOwner(
            Integer orderId, Integer ownerId);

    Page<ShopOrderListItemResponse> getOrdersForShopOwner(
            Integer ownerId, String keyword, OrderStatus status, String sortBy, String sortDirection, int page, int size);

    void confirmOrder(Integer orderId, Integer ownerId);

    void startPreparingOrder(Integer orderId, Integer ownerId);

    void markOrderReady(Integer orderId, Integer ownerId);

    void cancelOrder(Integer orderId, Integer ownerId);
}
