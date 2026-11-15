package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO báo cáo hiệu quả sử dụng voucher đặc quyền theo cấp bậc Hội viên (Shopee Rewards - TierCoupon).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponTierPerformanceResponse {
    private Integer tierId;                     // Mã hạng thành viên
    private String tierName;                    // Tên hạng (Đồng, Bạc, Vàng, Kim Cương)
    private long memberCount;                   // Số lượng khách hàng thuộc hạng
    private long usedVoucherCount;              // Số lượt voucher của hạng đã được dùng
    private BigDecimal revenueTotal;            // Doanh số do khách hạng này mang lại
    private String tierBadgeClass;              // CSS class màu sắc hạng (silver, gold, diamond)
}
