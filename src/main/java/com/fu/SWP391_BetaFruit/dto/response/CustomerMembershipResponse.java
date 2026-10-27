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

    // Shopee Rewards: Tiêu chí đơn hàng & Chu kỳ (Cycle)
    private long completedOrdersCount;
    private int nextTierOrdersRequired;
    private int ordersNeeded;
    private double progressOrdersPercentage;
    private String cycleExpiryDate;

    // Shopee Rewards: Danh sách Voucher đặc quyền theo Cấp bậc
    private List<TierVoucherResponse> tierVouchers;

    // Constructor tương thích ngược cho các phương thức cũ
    public CustomerMembershipResponse(
            Integer userId, String fullName, String username, BigDecimal totalCumulativeSpend,
            Integer currentTierId, String currentTierName, BigDecimal currentDiscountPercent, BigDecimal currentMinSpent,
            boolean hasNextTier, Integer nextTierId, String nextTierName, BigDecimal nextMinSpent,
            BigDecimal amountNeeded, double progressPercentage, List<TierDetail> allTiers) {
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.totalCumulativeSpend = totalCumulativeSpend;
        this.currentTierId = currentTierId;
        this.currentTierName = currentTierName;
        this.currentDiscountPercent = currentDiscountPercent;
        this.currentMinSpent = currentMinSpent;
        this.hasNextTier = hasNextTier;
        this.nextTierId = nextTierId;
        this.nextTierName = nextTierName;
        this.nextMinSpent = nextMinSpent;
        this.amountNeeded = amountNeeded;
        this.progressPercentage = progressPercentage;
        this.allTiers = allTiers;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierDetail {
        private Integer tierId;
        private String tierName;
        private BigDecimal minSpentRequired;
        private BigDecimal discountPercent;
        private boolean isCurrent;
        private int minOrdersRequired;

        public TierDetail(Integer tierId, String tierName, BigDecimal minSpentRequired, BigDecimal discountPercent, boolean isCurrent) {
            this(tierId, tierName, minSpentRequired, discountPercent, isCurrent, 0);
        }
    }
}
