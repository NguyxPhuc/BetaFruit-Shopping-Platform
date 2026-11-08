package com.fu.SWP391_BetaFruit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "TierCoupon", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_Tier_Coupon", columnNames = {"TierId", "CouponId"})
})
public class TierCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TierId", nullable = false)
    private MembershipTier tier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CouponId", nullable = false)
    private Coupon coupon;

    @Column(name = "UsageQuota")
    private Integer usageQuota;

    public TierCoupon(MembershipTier tier, Coupon coupon) {
        this.tier = tier;
        this.coupon = coupon;
        this.usageQuota = 1;
    }

    public TierCoupon(Integer id, MembershipTier tier, Coupon coupon) {
        this.id = id;
        this.tier = tier;
        this.coupon = coupon;
        this.usageQuota = 1;
    }

    public TierCoupon(MembershipTier tier, Coupon coupon, Integer usageQuota) {
        this.tier = tier;
        this.coupon = coupon;
        this.usageQuota = (usageQuota != null && usageQuota > 0) ? usageQuota : 1;
    }

    public int getEffectiveUsageQuota() {
        if (usageQuota != null && usageQuota > 0) {
            return usageQuota;
        }
        if (tier != null && tier.getTierName() != null) {
            String name = tier.getTierName().toLowerCase();
            if (name.contains("kim cương") || name.contains("diamond") || name.contains("vip")) {
                return 3;
            } else if (name.contains("vàng") || name.contains("gold")) {
                return 2;
            }
        }
        return 1;
    }
}
