package com.fu.SWP391_BetaFruit.dto.response;

import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableCheckoutCouponResponse {
    private Integer couponId;
    private String code;
    private String title;
    private CouponCategory category;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    private String scopeLabel;
    private boolean eligible;
    private String ineligibleReason;
    private BigDecimal estimatedDiscount;
    private String badgeType; // "FREESHIP" or "DISCOUNT"
    private LocalDateTime endDate;
    private String usageProgressText;
}
