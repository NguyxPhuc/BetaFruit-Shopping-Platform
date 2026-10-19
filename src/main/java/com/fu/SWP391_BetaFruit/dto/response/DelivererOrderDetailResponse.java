package com.fu.SWP391_BetaFruit.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DelivererOrderDetailResponse(ShipperOrderResponse summary,
        LocalDateTime createdAt, LocalDateTime assignedAt,
        BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal shippingFee,
        List<Item> items, List<StatusHistory> history,
        boolean canPickUp, boolean canStartDelivering) {
    public record Item(String productName, String variantName, Integer quantity,
                       BigDecimal unitPrice, BigDecimal lineTotal) {}
    public record StatusHistory(String statusLabel, String actor, String note, LocalDateTime createdAt) {}
}
