package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.MembershipTierRequest;
import com.fu.SWP391_BetaFruit.dto.response.CustomerMembershipResponse;
import com.fu.SWP391_BetaFruit.entity.MembershipTier;
import com.fu.SWP391_BetaFruit.dto.response.TierUpgradeResult;
import com.fu.SWP391_BetaFruit.dto.response.MembershipDiscountResult;
import java.math.BigDecimal;

import java.util.List;
import java.util.Map;

public interface MembershipTierService {

    // Phía Customer: BR-LY-01, BR-LY-02, BR-LY-03
    CustomerMembershipResponse getCustomerMembershipData(Integer userId);

    // Phía Admin: Quản lý tiers (BR-LY-02: không âm, không trùng, tăng dần)
    List<MembershipTier> getAllTiers();
    MembershipTier getTierById(Integer id);
    void createTier(MembershipTierRequest request);
    void updateTier(Integer id, MembershipTierRequest request);
    void deleteTier(Integer id);
    Map<String, Object> getAdminMembershipPageData();

    // Phía Nghiệp vụ Lõi (Shopee Loyalty Engine): FE-09.1, FE-11.2
    TierUpgradeResult processOrderCompleted(Integer customerId, Integer orderId, BigDecimal orderFinalAmount);

    // Phía Tiêu thụ Đặc quyền (Checkout Benefit): FE-09.2
    MembershipDiscountResult calculateMembershipDiscount(Integer customerId, BigDecimal subtotal);
}

