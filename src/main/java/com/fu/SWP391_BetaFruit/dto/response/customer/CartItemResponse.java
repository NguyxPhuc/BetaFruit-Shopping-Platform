package com.fu.SWP391_BetaFruit.dto.response.customer;

import java.math.BigDecimal;

public record CartItemResponse(Integer itemId, Integer productId, String productName,
        String variantName, String shopName, int quantity, Integer stockQuantity,
        BigDecimal unitPrice, BigDecimal subtotal, boolean available, String warning) {
}
