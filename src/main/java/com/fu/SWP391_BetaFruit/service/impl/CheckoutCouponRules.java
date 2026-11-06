package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.entity.MembershipTier;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.TierCoupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * Kiểm tra thẩm quyền theo Hạng thành viên (TierCoupon).
     * Nếu mã thuộc gói đặc quyền của một hoặc nhiều Hạng, khách hàng phải đạt hạng đó (hoặc hạng cao hơn).
     */
    public static void validateTierEligibility(Coupon coupon, MembershipTier customerTier, List<TierCoupon> tierCoupons) {
        if (tierCoupons == null || tierCoupons.isEmpty()) {
            return; // Mã công khai, không giới hạn theo hạng
        }

        Set<Integer> allowedTierIds = tierCoupons.stream()
                .map(tc -> tc.getTier() != null ? tc.getTier().getTierId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        BigDecimal minRequiredSpent = tierCoupons.stream()
                .map(tc -> tc.getTier() != null ? tc.getTier().getMinSpentRequired() : null)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        boolean matchesDirectTier = customerTier != null
                && customerTier.getTierId() != null
                && allowedTierIds.contains(customerTier.getTierId());

        boolean isHigherTier = customerTier != null
                && customerTier.getMinSpentRequired() != null
                && customerTier.getMinSpentRequired().compareTo(minRequiredSpent) >= 0;

        if (!matchesDirectTier && !isHigherTier) {
            String requiredTierNames = tierCoupons.stream()
                    .map(tc -> tc.getTier() != null ? tc.getTier().getTierName() : "")
                    .filter(name -> name != null && !name.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));
            String currentTierName = (customerTier != null && customerTier.getTierName() != null)
                    ? customerTier.getTierName() : "Thành viên";
            throw new IllegalArgumentException("Mã giảm giá này chỉ dành riêng cho hội viên [" + requiredTierNames
                    + "] trở lên. Hạng hiện tại của bạn là [" + currentTierName + "].");
        }
    }

    /**
     * Phân giải định mức số lượt sử dụng tối đa theo Hạng thành viên của khách hàng.
     */
    public static int resolveTierQuota(List<TierCoupon> tierCoupons, MembershipTier customerTier, int defaultLimit) {
        if (tierCoupons == null || tierCoupons.isEmpty() || customerTier == null) {
            return defaultLimit;
        }

        return tierCoupons.stream()
                .filter(tc -> tc.getTier() != null && Objects.equals(tc.getTier().getTierId(), customerTier.getTierId()))
                .map(TierCoupon::getEffectiveUsageQuota)
                .findFirst()
                .orElse(defaultLimit);
    }

    /**
     * Kiểm tra giới hạn số lần sử dụng mã trên mỗi tài khoản.
     */
    public static void validateUserUsageLimit(long usedCount, int maxLimitPerUser) {
        if (usedCount >= maxLimitPerUser) {
            throw new IllegalArgumentException("Bạn đã sử dụng hết " + maxLimitPerUser
                    + " lượt cho mã ưu đãi này.");
        }
    }

    /**
     * Chống gian lận: Chủ Shop không được tự dùng mã do chính Shop mình phát hành.
     */
    public static void validateShopOwnerFraud(Coupon coupon, Shop shop, Integer customerId) {
        if (coupon != null && coupon.getShop() != null && customerId != null) {
            Integer ownerId = (shop != null && shop.getOwner() != null)
                    ? shop.getOwner().getUserId()
                    : (coupon.getShop().getOwner() != null ? coupon.getShop().getOwner().getUserId() : null);
            if (Objects.equals(ownerId, customerId)) {
                throw new IllegalArgumentException("Chủ cửa hàng không thể sử dụng mã giảm giá của chính cửa hàng mình.");
            }
        }
    }
}
