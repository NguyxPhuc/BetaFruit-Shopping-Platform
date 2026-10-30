package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO đại diện cho 1 dòng dữ liệu trong Bảng xếp hạng hiệu quả từng mã (Leaderboard Table).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponLeaderboardRowResponse {
    private Integer couponId;
    private String couponCode;
    private String sponsorType;                 // "Sàn tài trợ" hoặc Tên Cửa Hàng
    private String categoryName;                // "Vận chuyển" hoặc "Sản phẩm"
    private Integer usedCount;                  // Số lượt đã dùng
    private Integer usageLimit;                 // Giới hạn số lượt
    private Integer usagePercentage;            // Tiến độ sử dụng (%)
    private BigDecimal discountTotal;           // Tổng chi phí đã giảm cho khách
    private BigDecimal revenueTotal;            // Tổng doanh thu mang lại
    private BigDecimal returnOnInvestment;      // Tỷ số hoàn vốn (ROI)
    private String performanceStatus;           // "Đang chạy", "Hết lượt", "Đã hết hạn", "Tạm dừng"
    private String statusBadgeClass;            // CSS class badge (bg-success, bg-danger...)
}
