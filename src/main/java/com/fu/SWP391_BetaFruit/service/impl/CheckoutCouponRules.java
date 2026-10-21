package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Checkout validation only; coupon creation and management remain unchanged. */
public final class CheckoutCouponRules {
    private CheckoutCouponRules() {}

    public static BigDecimal discount(Coupon coupon, CouponCategory category,
                                      BigDecimal merchandiseTotal, BigDecimal eligibleAmount,
                                      LocalDateTime now) {
        if (coupon.getCouponCategory() != category)
            throw new IllegalArgumentException("Mã giảm giá không đúng loại sản phẩm/phí vận chuyển.");
        if (!Boolean.TRUE.equals(coupon.getIsActive()))
            throw new IllegalArgumentException("Mã giảm giá đang tạm dừng.");
        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate()))
            throw new IllegalArgumentException("Mã giảm giá chưa đến thời gian sử dụng.");
        if (coupon.getEndDate() != null && now.isAfter(coupon.getEndDate()))
            throw new IllegalArgumentException("Mã giảm giá đã hết hạn.");
        int used = coupon.getUsedCount() == null ? 0 : coupon.getUsedCount();
        if (coupon.getUsageLimit() != null && used >= coupon.getUsageLimit())
            throw new IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng.");
        if (coupon.getMinOrderValue() != null && merchandiseTotal.compareTo(coupon.getMinOrderValue()) < 0)
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu của mã giảm giá.");
        BigDecimal value = coupon.getDiscountValue();
        if (value == null || value.signum() <= 0 || eligibleAmount.signum() < 0)
            throw new IllegalArgumentException("Mức giảm giá không hợp lệ.");
        BigDecimal amount;
        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            if (value.compareTo(new BigDecimal("100")) > 0
                    || coupon.getMaxDiscountAmount() == null || coupon.getMaxDiscountAmount().signum() <= 0)
                throw new IllegalArgumentException("Mã phần trăm thiếu mức giảm tối đa hợp lệ.");
            amount = eligibleAmount.multiply(value).movePointLeft(2).min(coupon.getMaxDiscountAmount());
        } else if (coupon.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            amount = value;
        } else {
            throw new IllegalArgumentException("Hình thức giảm giá không hợp lệ.");
        }
        // VND: round discounts down to whole dong; never discount above the eligible charge.
        return amount.min(eligibleAmount).setScale(0, RoundingMode.DOWN);
    }
}
