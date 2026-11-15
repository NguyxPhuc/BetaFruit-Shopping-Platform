package com.fu.SWP391_BetaFruit.dto.response;

import com.fu.SWP391_BetaFruit.dto.request.CouponDashboardFilterRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO tổng hợp toàn bộ dữ liệu cần thiết để render màn hình Voucher Analytics Dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponDashboardResponse {
    private CouponSummaryResponse summary;
    private List<CouponLeaderboardRowResponse> leaderboard;
    private List<CouponDailyTrendResponse> dailyTrends;
    private CouponCategoryDistributionResponse categoryDistribution;
    private CouponPaymentMethodResponse paymentMethodDistribution;
    private List<CouponTopFruitProductResponse> topFruitProducts;
    private List<CouponTierPerformanceResponse> tierPerformances;

    // Bộ lọc đang được áp dụng
    private CouponDashboardFilterRequest filter;

    // Danh sách các cảnh báo thông minh (Smart Anomaly & Budget Alerts)
    private List<String> anomalyAlerts;

    // Các chuỗi JSON chuẩn bị sẵn cho Chart.js render nhanh ở client
    private String trendLabelsJson;
    private String trendRevenueJson;
    private String trendDiscountJson;
}
