package com.fu.SWP391_BetaFruit.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A compact row for the shop owner's order-list screen. */
public record ShopOrderListItemResponse(
        Integer orderId,
        String orderCode,
        String customerName,
        BigDecimal finalAmount,
        BigDecimal receivedAmount,
        BigDecimal shopNetReceived,
        String orderStatus,
        String orderStatusLabel,
        boolean canConfirm,
        boolean canStartPreparing,
        boolean canMarkReady,
        boolean canCancel,
        LocalDateTime createdAt
) {
}
