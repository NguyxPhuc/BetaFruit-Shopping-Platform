package com.fu.SWP391_BetaFruit.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ShopCartResponse(Integer shopId, String shopName, List<CartItemResponse> items,
        long totalQuantity, BigDecimal totalAmount, boolean valid) {
}
