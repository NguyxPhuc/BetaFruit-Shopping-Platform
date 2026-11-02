package com.fu.SWP391_BetaFruit.dto.response;

import lombok.Builder;
import com.fu.SWP391_BetaFruit.enums.ReviewVisibility;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record CustomerOrderResponse(
        Integer orderId,
        String orderCode,
        Integer shopId,
        String shopName,
        String orderStatus,
        String orderStatusLabel,
        String paymentMethod,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal finalAmount,
        String snapshotShippingAddress,
        LocalDateTime createdAt,
        boolean canConfirmReceived,
        boolean isSuccess,
        List<OrderItemResponse> items
) {
    public record OrderItemResponse(
            Integer orderItemId,
            Integer productId,
            String productName,
            String variantName,
            Integer quantity,
            BigDecimal price,
            BigDecimal subtotal,
            Integer reviewId,
            Integer rating,
            String comment,
            ReviewVisibility reviewVisibility,
            boolean reviewCanEdit
    ) {}
}
