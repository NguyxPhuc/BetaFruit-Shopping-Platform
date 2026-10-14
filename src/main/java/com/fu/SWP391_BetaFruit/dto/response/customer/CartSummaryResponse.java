package com.fu.SWP391_BetaFruit.dto.response.customer;

import java.math.BigDecimal;
import java.util.List;

public record CartSummaryResponse(List<CartItemResponse> items, long totalQuantity,
        BigDecimal totalAmount, boolean valid) {
}
