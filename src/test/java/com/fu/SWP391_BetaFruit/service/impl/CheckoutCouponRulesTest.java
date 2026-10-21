package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutCouponRulesTest {
    final LocalDateTime now = LocalDateTime.of(2026, 10, 22, 12, 0);

    Coupon coupon() {
        Coupon c = new Coupon();
        c.setCouponCategory(CouponCategory.PRODUCT);
        c.setDiscountType(DiscountType.PERCENT);
        c.setDiscountValue(new BigDecimal("10"));
        c.setMaxDiscountAmount(new BigDecimal("20000"));
        c.setIsActive(true); c.setUsageLimit(10); c.setUsedCount(0);
        c.setStartDate(now.minusDays(1)); c.setEndDate(now.plusDays(1));
        return c;
    }

    BigDecimal apply(Coupon c, CouponCategory category, String subtotal, String base) {
        return CheckoutCouponRules.discount(c, category, new BigDecimal(subtotal), new BigDecimal(base), now);
    }

    @Test void percentUsesCorrectBaseAndCapAndWholeDong() {
        Coupon c = coupon();
        assertEquals(new BigDecimal("20000"), apply(c, CouponCategory.PRODUCT, "300000", "300000"));
        assertEquals(new BigDecimal("1000"), apply(c, CouponCategory.PRODUCT, "10009", "10009"));
        c.setCouponCategory(CouponCategory.SHIPPING);
        assertEquals(new BigDecimal("1500"), apply(c, CouponCategory.SHIPPING, "300000", "15000"));
    }

    @Test void fixedShippingDiscountCannotExceedShippingAndMinimumUsesMerchandise() {
        Coupon c = coupon(); c.setCouponCategory(CouponCategory.SHIPPING);
        c.setDiscountType(DiscountType.FIXED_AMOUNT); c.setDiscountValue(new BigDecimal("30000"));
        c.setMinOrderValue(new BigDecimal("200000"));
        assertEquals(new BigDecimal("15000"), apply(c, CouponCategory.SHIPPING, "300000", "15000"));
        assertThrows(IllegalArgumentException.class, () -> apply(c, CouponCategory.SHIPPING, "100000", "15000"));
    }

    @Test void rejectsWrongCategoryPausedUpcomingExpiredAndExhausted() {
        Coupon c = coupon();
        assertThrows(IllegalArgumentException.class, () -> apply(c, CouponCategory.SHIPPING, "300000", "15000"));
        c.setIsActive(false);
        assertThrows(IllegalArgumentException.class, () -> apply(c, CouponCategory.PRODUCT, "300000", "300000"));
        c.setIsActive(true); c.setStartDate(now.plusMinutes(1));
        assertThrows(IllegalArgumentException.class, () -> apply(c, CouponCategory.PRODUCT, "300000", "300000"));
        c.setStartDate(now.minusDays(1)); c.setEndDate(now.minusMinutes(1));
        assertThrows(IllegalArgumentException.class, () -> apply(c, CouponCategory.PRODUCT, "300000", "300000"));
        c.setEndDate(now.plusDays(1)); c.setUsedCount(10);
        assertThrows(IllegalArgumentException.class, () -> apply(c, CouponCategory.PRODUCT, "300000", "300000"));
    }
}
