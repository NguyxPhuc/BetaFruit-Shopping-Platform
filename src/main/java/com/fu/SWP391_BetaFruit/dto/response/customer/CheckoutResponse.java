package com.fu.SWP391_BetaFruit.dto.response.customer;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutResponse(List<ShopGroup> shops, List<Integer> itemIds, long quantity,
                               BigDecimal subtotal, BigDecimal shippingFee, BigDecimal finalAmount) {
    public record ShopGroup(Integer shopId, String shopName, List<CartItemResponse> items,
                            BigDecimal subtotal, BigDecimal shippingFee, BigDecimal finalAmount) {}
    public record PlacedOrder(Integer orderId, String shopName, BigDecimal finalAmount) {}
}
