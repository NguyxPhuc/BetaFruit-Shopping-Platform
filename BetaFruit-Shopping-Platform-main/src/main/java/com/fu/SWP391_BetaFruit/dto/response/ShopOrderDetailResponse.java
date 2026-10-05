package com.fu.SWP391_BetaFruit.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Order-time snapshots and explicitly selected data for the shop detail screen. */
public record ShopOrderDetailResponse(
        ShopOrderListItemResponse summary,
        Integer shopId,
        String shopName,
        Integer customerId,
        String customerPhone,
        String snapshotShippingAddress,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        String paymentMethod,
        Boolean isSettled,
        LocalDateTime settledAt,
        String couponCode,
        List<Item> items,
        List<StatusHistory> statusHistory,
        Delivery delivery,
        List<Payment> payments,
        BigDecimal platformFee,
        Boolean isCODRemitted,
        BigDecimal totalCost,
        BigDecimal estimatedProfit
) {
    public record Item(Integer orderItemId, String productName, String variantName,
                       Integer quantity, BigDecimal unitPrice, BigDecimal lineTotal,
                       BigDecimal costSnapshot) {}

    public record StatusHistory(Integer historyId, String statusFrom, String statusFromLabel,
                                String statusTo, String statusToLabel, String note,
                                String updatedByName, LocalDateTime createdAt) {}

    public record Delivery(Integer assignmentId, Integer shipperId, String shipperName,
                           String shipperPhone, LocalDateTime assignedAt) {}

    public record Payment(Integer transactionId, String gateway, String transactionRef,
                          BigDecimal amount, String status, LocalDateTime createdAt) {}
}
