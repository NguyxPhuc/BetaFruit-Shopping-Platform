package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopAnalyticsSummaryResponse {
    private Integer shopId;
    private String shopName;
    private String ownerName;
    private String ownerPhone;
    private String status;
    private BigDecimal totalRevenue;      // Tổng doanh số bán (VND)
    private BigDecimal totalPlatformFee;  // Phí sàn thu được từ shop (VND)
    private BigDecimal netRevenue;        // Doanh thu thực nhận sau khi trừ phí sàn (VND)
    private long totalOrders;             // Tổng số đơn hàng
    private long completedOrders;         // Số đơn hoàn tất
    private long cancelledOrders;         // Số đơn hủy / thất bại
    private double successRate;           // Tỷ lệ giao thành công (%)
    private long productCount;            // Số lượng hoa quả đang kinh doanh
}

