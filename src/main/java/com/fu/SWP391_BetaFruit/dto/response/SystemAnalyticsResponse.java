package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAnalyticsResponse {
    // 1. Total revenue
    private BigDecimal totalRevenue;           // Tổng doanh thu toàn sàn (VNĐ)
    private BigDecimal totalPlatformFee;       // Tổng phí sàn thu được (VNĐ)
    private BigDecimal averageOrderValue;      // Giá trị đơn trung bình (AOV)

    // 2. Total orders
    private long totalOrders;                  // Tổng số lượng đơn hàng
    private long completedOrders;              // Số đơn hoàn tất
    private long processingOrders;             // Số đơn đang xử lý / đang giao
    private long cancelledOrders;              // Số đơn đã hủy / thất bại

    // 3. Total shop owners
    private long totalShopOwners;              // Tổng số chủ cửa hàng
    private long approvedShops;                // Số gian hàng đang hoạt động

    // 4. Total customers
    private long totalCustomers;               // Tổng số khách hàng
    private long totalUsers;                   // Tổng người dùng toàn hệ thống

    // 5. Top 5 best selling products
    @Builder.Default
    private List<TopSellingProductResponse> topSellingProducts = new ArrayList<>();
}

