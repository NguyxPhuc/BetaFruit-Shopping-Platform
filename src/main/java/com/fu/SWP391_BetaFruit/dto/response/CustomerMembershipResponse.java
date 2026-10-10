package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerMembershipResponse {
    // Thông tin khách hàng & chi tiêu tích lũy
    private Integer userId;
    private String fullName;
    private String username;
    private BigDecimal totalCumulativeSpend;

    // Hạng hiện tại (BR-LY-03)
    private Integer currentTierId;
    private String currentTierName;
    private BigDecimal currentDiscountPercent;
    private BigDecimal currentMinSpent;

    // Tiến độ lên hạng tiếp theo (BR-LY-02)
    private boolean hasNextTier;
    private Integer nextTierId;
    private String nextTierName;
    private BigDecimal nextMinSpent;
    private BigDecimal amountNeeded;
    private double progressPercentage;

    // Danh sách toàn bộ các hạng để xem quyền lợi tổng thể
    private List<TierDetail> allTiers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TierDetail {
        private Integer tierId;
        private String tierName;
        private BigDecimal minSpentRequired;
        private BigDecimal discountPercent;
        private boolean isCurrent;
    }
}
