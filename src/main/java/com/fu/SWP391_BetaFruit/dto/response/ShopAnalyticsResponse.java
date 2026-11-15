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
public class ShopAnalyticsResponse {
    // Thông tin cửa hàng
    private Integer shopId;
    private String shopName;
    private String ownerName;
    private String ownerPhone;
    private String shopStatus;
    private BigDecimal availableBalance;

    // Doanh số & Tài chính
    private BigDecimal totalRevenue;           // Tổng doanh số (GMV)
    private BigDecimal totalPlatformFee;       // Phí sàn trích nộp
    private BigDecimal netRevenue;             // Doanh thu thực nhận
    private BigDecimal averageOrderValue;      // Giá trị trung bình mỗi đơn (AOV)

    // Đơn hàng
    private long totalOrders;                  // Tổng số đơn hàng của gian hàng
    private long completedOrders;              // Đơn hoàn tất thành công
    private long processingOrders;             // Đơn đang giao / đang xử lý
    private long cancelledOrders;              // Đơn đã hủy
    private double successRate;                // Tỷ lệ hoàn tất đơn (%)

    // Kho hàng & Khách hàng
    private long productCount;                 // Số loại mặt hàng hoa quả
    private long totalStockQuantity;           // Tổng tồn kho hiện tại
    private long lowStockCount;                // Số loại hoa quả sắp hết hàng (tồn <= 5)
    private long totalCustomers;               // Tổng số khách hàng đã mua của shop

    // Top 5 mặt hàng hoa quả bán chạy nhất
    @Builder.Default
    private List<TopSellingProductResponse> topSellingProducts = new ArrayList<>();

    // Danh sách toàn bộ mặt hàng hoa quả của gian hàng
    @Builder.Default
    private List<TopSellingProductResponse> allProducts = new ArrayList<>();
}

