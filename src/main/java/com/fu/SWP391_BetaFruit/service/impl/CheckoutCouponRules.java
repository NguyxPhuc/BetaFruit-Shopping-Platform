package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Checkout validation only; coupon creation and management remain unchanged. */
public final class CheckoutCouponRules {
    private CheckoutCouponRules() {}

    public static void validate(Coupon coupon, BigDecimal merchandiseTotal, LocalDateTime now) {
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
    }
}
