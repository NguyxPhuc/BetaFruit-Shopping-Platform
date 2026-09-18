package com.fu.SWP391_BetaFruit.entity;

import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Coupon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CouponId")
    private Integer couponId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopId")
    private Shop shop;

    @Column(name = "CouponCode", nullable = false, unique = true, length = 50)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "CouponCategory", length = 20)
    private CouponCategory couponCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "DiscountType", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "DiscountValue", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "MinOrderValue", precision = 18, scale = 2)
    private BigDecimal minOrderValue;

    @Column(name = "MaxDiscountAmount", precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "UsageLimit")
    private Integer usageLimit;

    @Column(name = "UsedCount")
    private Integer usedCount;

    @Column(name = "IsActive")
    private Boolean isActive;

    @Column(name = "StartDate")
    private LocalDateTime startDate;

    @Column(name = "EndDate")
    private LocalDateTime endDate;
}
