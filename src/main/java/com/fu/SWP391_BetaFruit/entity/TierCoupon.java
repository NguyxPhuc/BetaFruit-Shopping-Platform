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

    public TierCoupon(MembershipTier tier, Coupon coupon) {
        this.tier = tier;
        this.coupon = coupon;
    }
}
