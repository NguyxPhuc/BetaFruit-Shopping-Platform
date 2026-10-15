package com.fu.SWP391_BetaFruit.dto.response;

import com.fu.SWP391_BetaFruit.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUsageResponse {
    private CouponResponse coupon;
    private long totalUsed;
    private long usageLimit;
    private long remainingLimit;
    private int usagePercentage;
    private BigDecimal totalDiscountSponsored;
    private Page<Order> orders;
}
