package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Kết quả xử lý thăng hạng thành viên sau khi đơn hàng hoàn tất.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TierUpgradeResult {
    private boolean upgraded;
    private Integer previousTierId;
    private String previousTierName;
    private Integer newTierId;
    private String newTierName;
    private BigDecimal newDiscountPercent;
    private BigDecimal cumulativeSpend;
    private long completedOrdersCount;
    private String welcomeMessage;
    private String welcomeCouponCode;

    public static TierUpgradeResult notUpgraded(Integer currentTierId, String currentTierName,
                                                BigDecimal cumulativeSpend, long completedOrdersCount) {
        return TierUpgradeResult.builder()
                .upgraded(false)
                .previousTierId(currentTierId)
                .previousTierName(currentTierName)
                .newTierId(currentTierId)
                .newTierName(currentTierName)
                .cumulativeSpend(cumulativeSpend)
                .completedOrdersCount(completedOrdersCount)
                .build();
    }
}
