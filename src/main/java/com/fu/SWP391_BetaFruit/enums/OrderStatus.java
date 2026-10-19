package com.fu.SWP391_BetaFruit.enums;

public enum OrderStatus {
        PENDING,       // Chờ xác nhận
        CONFIRMED,     // Đã xác nhận
        PREPARING,     // Đang chuẩn bị hàng
        READY,         // Đã sẵn sàng giao cho Shipper
        PICKED_UP,     // Shipper đã lấy hàng
        DELIVERING,    // Đang giao hàng
        SUCCESS,       // Giao thành công
        FAILED,        // Giao thất bại
        CANCELLED,     // Đã hủy (bởi khách/shop)
        REJECTED       // Từ chối nhận đơn{
}
