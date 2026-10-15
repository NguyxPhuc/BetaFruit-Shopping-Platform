package com.fu.SWP391_BetaFruit.dto.response;

import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {
    private Integer couponId;
    private Shop shop;
    private String couponCode;
    private CouponCategory couponCategory;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer usedCount;
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Calculated fields
    private boolean isExpired;
    private boolean isUpcoming;
    private boolean isOutOfStock;
    private boolean isValidNow;
    private int usagePercent;
    private String statusBadgeClass;
    private String statusLabel;

    public static CouponResponse fromEntity(Coupon coupon) {
        if (coupon == null) return null;

        LocalDateTime now = LocalDateTime.now();
        boolean expired = coupon.getEndDate() != null && now.isAfter(coupon.getEndDate());
        boolean upcoming = coupon.getStartDate() != null && now.isBefore(coupon.getStartDate());
        boolean outOfStock = coupon.getUsageLimit() != null && coupon.getUsedCount() != null
                && coupon.getUsedCount() >= coupon.getUsageLimit();
        boolean validNow = Boolean.TRUE.equals(coupon.getIsActive()) && !expired && !upcoming && !outOfStock;

        int percent = 0;
        if (coupon.getUsageLimit() != null && coupon.getUsageLimit() > 0
                && coupon.getUsedCount() != null && coupon.getUsedCount() > 0) {
            percent = Math.min(100, (int) Math.round(((double) coupon.getUsedCount() / coupon.getUsageLimit()) * 100));
        }

        String badgeClass;
        String label;
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            badgeClass = "bg-secondary";
            label = "Tạm dừng";
        } else if (expired) {
            badgeClass = "bg-dark";
            label = "Đã hết hạn";
        } else if (outOfStock) {
            badgeClass = "bg-danger";
            label = "Hết lượt";
        } else if (upcoming) {
            badgeClass = "bg-info text-dark";
            label = "Sắp diễn ra";
        } else {
            badgeClass = "bg-success";
            label = "Đang hoạt động";
        }

        return CouponResponse.builder()
                .couponId(coupon.getCouponId())
                .shop(coupon.getShop())
                .couponCode(coupon.getCouponCode())
                .couponCategory(coupon.getCouponCategory())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .isActive(coupon.getIsActive())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .isExpired(expired)
                .isUpcoming(upcoming)
                .isOutOfStock(outOfStock)
                .isValidNow(validNow)
                .usagePercent(percent)
                .statusBadgeClass(badgeClass)
                .statusLabel(label)
                .build();
    }
}
