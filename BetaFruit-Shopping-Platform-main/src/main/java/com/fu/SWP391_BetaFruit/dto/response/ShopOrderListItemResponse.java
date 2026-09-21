package com.fu.SWP391_BetaFruit.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A compact row for the shop owner's order-list screen. */
public record ShopOrderListItemResponse(
        Integer orderId,
        String orderCode,
        String customerName,
        String customerAvatarUrl,
        BigDecimal finalAmount,
        String orderStatus,
        String orderStatusLabel,
        LocalDateTime createdAt
) {
}
