package com.fu.SWP391_BetaFruit.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShipperOrderResponse(
        Integer orderId, String orderStatus, String customerName, String customerPhone,
        String shippingAddress, String shopName, String paymentMethod,
        BigDecimal finalAmount, BigDecimal codAmount, BigDecimal amountToCollect,
        BigDecimal codHeldAmount, String codRemittanceStatus, Boolean isCODRemitted,
        Boolean isSettled, LocalDateTime settledAt, boolean canMarkDelivered) {

    public String orderStatusLabel() {
        return statusLabel(orderStatus);
    }

    public static String statusLabel(String orderStatus) {
        if (orderStatus == null) return "Chưa xác định";
        return switch (orderStatus) {
            case "PENDING" -> "Chờ xác nhận";
            case "CONFIRMED" -> "Đã xác nhận";
            case "PREPARING" -> "Đang chuẩn bị hàng";
            case "READY" -> "Sẵn sàng giao";
            case "PICKED_UP" -> "Đã lấy hàng";
            case "DELIVERING" -> "Đang giao hàng";
            case "DELIVERED" -> "Đã giao - chờ hoàn tất";
            case "SUCCESS" -> "Đơn hoàn tất";
            case "FAILED" -> "Giao thất bại";
            case "CANCELLED" -> "Đã hủy";
            case "REJECTED" -> "Từ chối nhận đơn";
            default -> "Chưa xác định";
        };
    }
}
