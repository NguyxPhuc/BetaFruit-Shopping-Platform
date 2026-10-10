package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.MembershipTierRequest;
import com.fu.SWP391_BetaFruit.dto.response.CustomerMembershipResponse;
import com.fu.SWP391_BetaFruit.entity.CustomerMembership;
import com.fu.SWP391_BetaFruit.entity.MembershipTier;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.repository.CustomerMembershipRepository;
import com.fu.SWP391_BetaFruit.repository.MembershipTierRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.MembershipTierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MembershipTierServiceImpl implements MembershipTierService {

    private final MembershipTierRepository membershipTierRepository;
    private final CustomerMembershipRepository customerMembershipRepository;
    private final UserRepository userRepository;

    // =========================================================================
    // 1. PHÍA CUSTOMER (BR-LY-01, BR-LY-02, BR-LY-03)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public CustomerMembershipResponse getCustomerMembershipData(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin người dùng với ID: " + userId));

        // Lấy thông tin tích lũy chi tiêu của customer (BR-LY-01: được tích lũy từ các đơn SUCCESS)
        CustomerMembership membership = customerMembershipRepository.findByUserId(userId).orElse(null);
        BigDecimal totalSpent = (membership != null && membership.getTotalCumulativeSpend() != null)
                ? membership.getTotalCumulativeSpend()
                : BigDecimal.ZERO;

        // Lấy tất cả các hạng thành viên được sắp xếp tăng dần theo MinSpentRequired (BR-LY-02)
        List<MembershipTier> sortedTiers = membershipTierRepository.findAllByOrderByMinSpentRequiredAsc();

        // Xác định hạng cao nhất mà Customer đạt được (BR-LY-03)
        MembershipTier currentTier = null;
        MembershipTier nextTier = null;

        for (int i = 0; i < sortedTiers.size(); i++) {
            MembershipTier tier = sortedTiers.get(i);
            if (totalSpent.compareTo(tier.getMinSpentRequired()) >= 0) {
                currentTier = tier; // Cập nhật hạng cao nhất thỏa mãn
            } else {
                // Hạng đầu tiên có MinSpentRequired > totalSpent chính là nextTier
                nextTier = tier;
                break;
            }
        }

        // Nếu database chưa có tier nào hoặc khách chưa đạt tier nào nhưng có tiers
        if (currentTier == null && !sortedTiers.isEmpty()) {
            currentTier = sortedTiers.get(0);
            if (sortedTiers.size() > 1) {
                nextTier = sortedTiers.get(1);
            }
        }

        // Tính tiến độ lên hạng kế tiếp (BR-LY-02)
        boolean hasNextTier = (nextTier != null);
        BigDecimal amountNeeded = BigDecimal.ZERO;
        double progressPercentage = 100.0;

        if (hasNextTier) {
            BigDecimal nextMinSpent = nextTier.getMinSpentRequired();
            amountNeeded = nextMinSpent.subtract(totalSpent);
            if (amountNeeded.compareTo(BigDecimal.ZERO) < 0) {
                amountNeeded = BigDecimal.ZERO;
            }

            if (nextMinSpent.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal progress = totalSpent.divide(nextMinSpent, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                progressPercentage = Math.min(100.0, progress.doubleValue());
            } else {
                progressPercentage = 100.0;
            }
        }

        // Chuẩn bị danh sách tất cả các hạng để xem quyền lợi tổng thể
        List<CustomerMembershipResponse.TierDetail> tierDetails = new ArrayList<>();
        for (MembershipTier t : sortedTiers) {
            boolean isCur = (currentTier != null && Objects.equals(t.getTierId(), currentTier.getTierId()));
            tierDetails.add(CustomerMembershipResponse.TierDetail.builder()
                    .tierId(t.getTierId())
                    .tierName(t.getTierName())
                    .minSpentRequired(t.getMinSpentRequired())
                    .discountPercent(t.getDiscountPercent())
                    .isCurrent(isCur)
                    .build());
        }

        return CustomerMembershipResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .totalCumulativeSpend(totalSpent)
                .currentTierId(currentTier != null ? currentTier.getTierId() : null)
                .currentTierName(currentTier != null ? currentTier.getTierName() : "Thành viên")
                .currentDiscountPercent(currentTier != null ? currentTier.getDiscountPercent() : BigDecimal.ZERO)
                .currentMinSpent(currentTier != null ? currentTier.getMinSpentRequired() : BigDecimal.ZERO)
                .hasNextTier(hasNextTier)
                .nextTierId(nextTier != null ? nextTier.getTierId() : null)
                .nextTierName(nextTier != null ? nextTier.getTierName() : null)
                .nextMinSpent(nextTier != null ? nextTier.getMinSpentRequired() : null)
                .amountNeeded(amountNeeded)
                .progressPercentage(progressPercentage)
                .allTiers(tierDetails)
                .build();
    }

    // =========================================================================
    // 2. PHÍA ADMIN: QUẢN LÝ TIERS (BR-LY-02)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<MembershipTier> getAllTiers() {
        return membershipTierRepository.findAllByOrderByMinSpentRequiredAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipTier getTierById(Integer id) {
        return membershipTierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hạng thành viên với mã ID: " + id));
    }

    @Override
    @Transactional
    public void createTier(MembershipTierRequest request) {
        validateTierRequest(request, null);

        MembershipTier tier = new MembershipTier();
        tier.setTierName(request.getTierName().trim());
        tier.setMinSpentRequired(request.getMinSpentRequired());
        tier.setDiscountPercent(request.getDiscountPercent());

        membershipTierRepository.save(tier);
    }

    @Override
    @Transactional
    public void updateTier(Integer id, MembershipTierRequest request) {
        MembershipTier tier = getTierById(id);
        validateTierRequest(request, id);

        tier.setTierName(request.getTierName().trim());
        tier.setMinSpentRequired(request.getMinSpentRequired());
        tier.setDiscountPercent(request.getDiscountPercent());

        membershipTierRepository.save(tier);
    }

    @Override
    @Transactional
    public void deleteTier(Integer id) {
        MembershipTier tier = getTierById(id);

        // Kiểm tra an toàn ràng buộc khóa ngoại: có khách hàng nào đang ở hạng này không
        if (customerMembershipRepository.existsByTier_TierId(id)) {
            throw new IllegalStateException("Không thể xóa hạng '" + tier.getTierName() + "' vì đang có khách hàng thuộc hạng này!");
        }

        membershipTierRepository.delete(tier);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminMembershipPageData() {
        List<MembershipTier> tiers = getAllTiers();
        Map<String, Object> data = new HashMap<>();
        data.put("tiers", tiers);
        data.put("totalTiers", tiers.size());
        data.put("newTier", new MembershipTierRequest());
        return data;
    }

    /**
     * Validate theo quy tắc nghiệp vụ BR-LY-02:
     * - Không âm (minSpentRequired >= 0, discountPercent >= 0 và <= 100)
     * - Tên hạng không được trùng
     * - Ngưỡng chi tiêu không được trùng với hạng khác
     */
    private void validateTierRequest(MembershipTierRequest request, Integer currentId) {
        if (request.getTierName() == null || request.getTierName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên hạng thành viên không được để trống!");
        }
        if (request.getMinSpentRequired() == null || request.getMinSpentRequired().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Ngưỡng chi tiêu yêu cầu phải không âm (>= 0)!");
        }
        if (request.getDiscountPercent() == null || request.getDiscountPercent().compareTo(BigDecimal.ZERO) < 0
                || request.getDiscountPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Tỷ lệ chiết khấu giảm giá phải từ 0% đến 100%!");
        }

        String trimmedName = request.getTierName().trim();
        List<MembershipTier> allTiers = membershipTierRepository.findAll();

        for (MembershipTier t : allTiers) {
            if (currentId != null && Objects.equals(t.getTierId(), currentId)) {
                continue; // Bỏ qua chính bản ghi đang cập nhật
            }
            if (t.getTierName().equalsIgnoreCase(trimmedName)) {
                throw new IllegalArgumentException("Tên hạng '" + trimmedName + "' đã tồn tại trong hệ thống!");
            }
            if (t.getMinSpentRequired().compareTo(request.getMinSpentRequired()) == 0) {
                throw new IllegalArgumentException("Ngưỡng chi tiêu " + request.getMinSpentRequired() + " ₫ đã trùng với hạng '" + t.getTierName() + "'!");
            }
        }
    }
}
