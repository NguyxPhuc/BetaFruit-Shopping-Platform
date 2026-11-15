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
    // Thông tin Shop đang được chọn lọc (nếu lọc theo shop)
    private Integer selectedShopId;
    private String selectedShopName;
    private String selectedShopOwner;
    private String selectedShopStatus;

    // 1. Total revenue
    private BigDecimal totalRevenue;           // Tổng doanh thu (VNĐ)
    private BigDecimal totalPlatformFee;       // Tổng phí sàn thu được (VNĐ)
    private BigDecimal netRevenue;             // Doanh thu thực nhận sau khi trừ phí sàn (VNĐ)
    private BigDecimal averageOrderValue;      // Giá trị đơn trung bình (AOV)

    // 2. Total orders
    private long totalOrders;                  // Tổng số lượng đơn hàng
    private long completedOrders;              // Số đơn hoàn tất
    private long processingOrders;             // Số đơn đang xử lý / đang giao
    private long cancelledOrders;              // Số đơn đã hủy / thất bại
    private double successRate;                // Tỷ lệ giao thành công (%)

    // 3. Total shop owners / Shop metrics
    private long totalShopOwners;              // Tổng số chủ cửa hàng (khi xem toàn sàn)
    private long approvedShops;                // Số gian hàng đang hoạt động
    private long shopProductCount;             // Số lượng hoa quả của shop (khi lọc theo shop)

    // 4. Total customers
    private long totalCustomers;               // Tổng số khách hàng (toàn sàn hoặc số khách mua tại shop)
    private long totalUsers;                   // Tổng người dùng toàn hệ thống

    // 5. Top 5 best selling products
    @Builder.Default
    private List<TopSellingProductResponse> topSellingProducts = new ArrayList<>();

    // 6. Bảng danh sách thống kê chi tiết từng gian hàng
    @Builder.Default
    private List<ShopAnalyticsSummaryResponse> shopSummaries = new ArrayList<>();
}
