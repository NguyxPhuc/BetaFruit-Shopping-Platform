package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.MembershipTierRequest;
import com.fu.SWP391_BetaFruit.dto.response.CustomerMembershipResponse;
import com.fu.SWP391_BetaFruit.dto.response.MembershipDiscountResult;
import com.fu.SWP391_BetaFruit.dto.response.TierUpgradeResult;
import com.fu.SWP391_BetaFruit.dto.response.TierVoucherResponse;
import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.entity.CustomerMembership;
import com.fu.SWP391_BetaFruit.entity.MembershipTier;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.CouponRepository;
import com.fu.SWP391_BetaFruit.repository.CustomerMembershipRepository;
import com.fu.SWP391_BetaFruit.repository.MembershipTierRepository;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.MembershipTierService;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipTierServiceImpl implements MembershipTierService {

    private final MembershipTierRepository membershipTierRepository;
    private final CustomerMembershipRepository customerMembershipRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final CouponRepository couponRepository;

    // =========================================================================
    // 1. PHÍA CUSTOMER (BR-LY-01, BR-LY-02, BR-LY-03 & TIÊU CHÍ KÉP SHOPEE)
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

        // Đếm tổng số đơn hoàn tất của khách theo thời gian thực (Shopee Rewards)
        long completedOrders = orderRepository.countByCustomer_UserIdAndOrderStatus(userId, OrderStatus.SUCCESS);

        // Lấy tất cả các hạng thành viên được sắp xếp tăng dần theo MinSpentRequired (BR-LY-02)
        List<MembershipTier> sortedTiers = membershipTierRepository.findAllByOrderByMinSpentRequiredAsc();

        // Xác định hạng cao nhất mà Customer đạt được theo TIÊU CHÍ KÉP SHOPEE (BR-LY-03):
        // Thỏa mãn Đạt mốc chi tiêu HOẶC Đạt mốc số đơn hoàn tất
        MembershipTier currentTier = null;
        MembershipTier nextTier = null;
        int currentTierIndex = -1;

        for (int i = 0; i < sortedTiers.size(); i++) {
            MembershipTier tier = sortedTiers.get(i);
            int minOrders = getRequiredOrdersForTierIndex(i);
            boolean spendPassed = totalSpent.compareTo(tier.getMinSpentRequired()) >= 0;
            boolean ordersPassed = completedOrders >= minOrders;

            if (spendPassed || ordersPassed) {
                currentTier = tier; // Cập nhật hạng cao nhất thỏa mãn
                currentTierIndex = i;
            } else {
                // Hạng đầu tiên chưa đạt chính là nextTier
                nextTier = tier;
                break;
            }
        }

        // Nếu database chưa có tier nào hoặc khách chưa đạt tier nào nhưng có tiers
        if (currentTier == null && !sortedTiers.isEmpty()) {
            currentTier = sortedTiers.get(0);
            currentTierIndex = 0;
            if (sortedTiers.size() > 1) {
                nextTier = sortedTiers.get(1);
            }
        }

        // Tính tiến độ lên hạng kế tiếp (BR-LY-02 & Shopee Dual Progress)
        boolean hasNextTier = (nextTier != null);
        BigDecimal amountNeeded = BigDecimal.ZERO;
        double progressPercentage = 100.0;
        int nextTierRequiredOrders = 0;
        int ordersNeeded = 0;
        double progressOrdersPercentage = 100.0;

        if (hasNextTier) {
            int nextTierIndex = currentTierIndex + 1;
            nextTierRequiredOrders = getRequiredOrdersForTierIndex(nextTierIndex);
            ordersNeeded = Math.max(0, nextTierRequiredOrders - (int) completedOrders);
            if (nextTierRequiredOrders > 0) {
                progressOrdersPercentage = Math.min(100.0, ((double) completedOrders / nextTierRequiredOrders) * 100.0);
            } else {
                progressOrdersPercentage = 100.0;
            }

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
        for (int i = 0; i < sortedTiers.size(); i++) {
            MembershipTier t = sortedTiers.get(i);
            boolean isCur = (currentTier != null && Objects.equals(t.getTierId(), currentTier.getTierId()));
            tierDetails.add(new CustomerMembershipResponse.TierDetail(
                    t.getTierId(),
                    t.getTierName(),
                    t.getMinSpentRequired(),
                    t.getDiscountPercent(),
                    isCur,
                    getRequiredOrdersForTierIndex(i)
            ));
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
                .completedOrdersCount(completedOrders)
                .nextTierOrdersRequired(nextTierRequiredOrders)
                .ordersNeeded(ordersNeeded)
                .progressOrdersPercentage(progressOrdersPercentage)
                .cycleExpiryDate(calculateCycleExpiryDate())
                .tierVouchers(getExclusiveVouchersForTier(currentTierIndex, currentTier))
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

        MembershipTier saved = membershipTierRepository.save(tier);
        syncTierVoucher(saved);
    }

    @Override
    @Transactional
    public void updateTier(Integer id, MembershipTierRequest request) {
        MembershipTier tier = getTierById(id);
        validateTierRequest(request, id);

        tier.setTierName(request.getTierName().trim());
        tier.setMinSpentRequired(request.getMinSpentRequired());
        tier.setDiscountPercent(request.getDiscountPercent());

        MembershipTier saved = membershipTierRepository.save(tier);
        syncTierVoucher(saved);
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
        deactivateTierVoucher(tier);
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

    // =========================================================================
    // 3. SHOPEE LOYALTY ENGINE: TỰ ĐỘNG TÍCH LŨY, NÂNG HẠNG & ĐẶC QUYỀN
    // =========================================================================
    @Override
    @Transactional
    public TierUpgradeResult processOrderCompleted(Integer customerId, Integer orderId, BigDecimal orderFinalAmount) {
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId không được để trống!");
        }
        if (orderFinalAmount == null || orderFinalAmount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Đơn hàng #{} có số tiền finalAmount không hợp lệ: {}", orderId, orderFinalAmount);
            orderFinalAmount = BigDecimal.ZERO;
        }

        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + customerId));

        // Lấy hoặc khởi tạo CustomerMembership
        CustomerMembership membership = customerMembershipRepository.findByUserId(customerId)
                .orElseGet(() -> {
                    MembershipTier defaultTier = membershipTierRepository.findTopByOrderByMinSpentRequiredAsc();
                    CustomerMembership newMember = new CustomerMembership();
                    newMember.setUser(user);
                    newMember.setTier(defaultTier);
                    newMember.setTotalCumulativeSpend(BigDecimal.ZERO);
                    return customerMembershipRepository.save(newMember);
                });

        BigDecimal oldSpend = membership.getTotalCumulativeSpend() != null ? membership.getTotalCumulativeSpend() : BigDecimal.ZERO;
        BigDecimal newSpend = oldSpend.add(orderFinalAmount);
        membership.setTotalCumulativeSpend(newSpend);

        // Đếm tổng số đơn hoàn tất của khách (OrderStatus.SUCCESS)
        long completedOrders = orderRepository.countByCustomer_UserIdAndOrderStatus(customerId, OrderStatus.SUCCESS);

        // Lấy danh sách Tiers sắp xếp tăng dần theo MinSpent
        List<MembershipTier> sortedTiers = membershipTierRepository.findAllByOrderByMinSpentRequiredAsc();

        MembershipTier currentTierInDb = membership.getTier();
        MembershipTier qualifiedTier = null;

        // Quét tìm hạng cao nhất theo tiêu chí kép chuẩn Shopee Rewards:
        // Đạt chi tiêu tối thiểu HOẶC đạt số đơn hoàn tất tối thiểu
        for (int i = 0; i < sortedTiers.size(); i++) {
            MembershipTier t = sortedTiers.get(i);
            int minOrders = getRequiredOrdersForTierIndex(i);
            boolean spendPassed = newSpend.compareTo(t.getMinSpentRequired()) >= 0;
            boolean ordersPassed = completedOrders >= minOrders;

            if (spendPassed || ordersPassed) {
                qualifiedTier = t; // Cập nhật hạng cao nhất thỏa mãn
            }
        }

        if (qualifiedTier == null && !sortedTiers.isEmpty()) {
            qualifiedTier = sortedTiers.get(0);
        }

        // Kiểm tra xem có diễn ra sự kiện THĂNG HẠNG không (QualifiedTier cao hơn CurrentTier)
        boolean isUpgraded = false;
        MembershipTier previousTier = currentTierInDb;

        if (qualifiedTier != null) {
            if (currentTierInDb == null) {
                isUpgraded = true;
            } else if (qualifiedTier.getMinSpentRequired().compareTo(currentTierInDb.getMinSpentRequired()) > 0) {
                isUpgraded = true;
            }
        }

        if (isUpgraded && qualifiedTier != null) {
            membership.setTier(qualifiedTier);
            customerMembershipRepository.save(membership);

            // Bắn thông báo thăng hạng theo chuẩn Shopee Rewards
            String welcomeCoupon;
            if (qualifiedTier.getTierName() != null && qualifiedTier.getTierName().contains("Kim Cương")) {
                welcomeCoupon = "DIAMOND_VIP100K";
            } else if (qualifiedTier.getTierName() != null && qualifiedTier.getTierName().contains("Vàng")) {
                welcomeCoupon = "GOLD_DISCOUNT50K";
            } else if (qualifiedTier.getTierName() != null && qualifiedTier.getTierName().contains("Bạc")) {
                welcomeCoupon = "SILVER_FREESHIP";
            } else {
                welcomeCoupon = "LOYALTY" + qualifiedTier.getDiscountPercent().intValue();
            }

            String title = "🎉 Chúc mừng bạn đã thăng hạng " + qualifiedTier.getTierName() + "!";
            String message = String.format(
                    "Chúc mừng %s đã đạt mốc hội viên %s của BetaFruit! "
                    + "Đặc quyền chiết khấu %s%% đã kích hoạt thành công. Nhận ngay Voucher độc quyền '%s' tại trang Hội viên!",
                    user.getFullName() != null ? user.getFullName() : user.getUsername(),
                    qualifiedTier.getTierName(),
                    qualifiedTier.getDiscountPercent(),
                    welcomeCoupon
            );

            try {
                notificationService.createBaseNotification(
                        user,
                        title,
                        message,
                        NotificationType.PROMOTION,
                        "/membership"
                );
            } catch (Exception e) {
                log.error("Lỗi khi bắn thông báo thăng hạng cho user {}: {}", customerId, e.getMessage());
            }

            log.info("Khách hàng #{} ({}) đã thăng hạng thành công: {} -> {}",
                    customerId, user.getUsername(),
                    previousTier != null ? previousTier.getTierName() : "Chưa có hạng",
                    qualifiedTier.getTierName());

            return TierUpgradeResult.builder()
                    .upgraded(true)
                    .previousTierId(previousTier != null ? previousTier.getTierId() : null)
                    .previousTierName(previousTier != null ? previousTier.getTierName() : null)
                    .newTierId(qualifiedTier.getTierId())
                    .newTierName(qualifiedTier.getTierName())
                    .newDiscountPercent(qualifiedTier.getDiscountPercent())
                    .cumulativeSpend(newSpend)
                    .completedOrdersCount(completedOrders)
                    .welcomeMessage(message)
                    .welcomeCouponCode(welcomeCoupon)
                    .build();
        } else {
            // Không thăng hạng, chỉ lưu chi tiêu mới
            customerMembershipRepository.save(membership);
            return TierUpgradeResult.notUpgraded(
                    currentTierInDb != null ? currentTierInDb.getTierId() : null,
                    currentTierInDb != null ? currentTierInDb.getTierName() : "Thành viên",
                    newSpend,
                    completedOrders
            );
        }
    }

    // =========================================================================
    // 4. TIÊU THỤ ĐẶC QUYỀN TẠI CHECKOUT (FE-09.2)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public MembershipDiscountResult calculateMembershipDiscount(Integer customerId, BigDecimal subtotal) {
        if (customerId == null) {
            throw new IllegalArgumentException("CustomerId không được để trống!");
        }
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return MembershipDiscountResult.builder()
                    .customerId(customerId)
                    .discountPercent(BigDecimal.ZERO)
                    .subtotal(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(BigDecimal.ZERO)
                    .build();
        }

        CustomerMembership membership = customerMembershipRepository.findByUserId(customerId).orElse(null);
        MembershipTier tier = (membership != null) ? membership.getTier() : null;

        if (tier == null) {
            tier = membershipTierRepository.findTopByOrderByMinSpentRequiredAsc();
        }

        BigDecimal discountPercent = (tier != null && tier.getDiscountPercent() != null)
                ? tier.getDiscountPercent()
                : BigDecimal.ZERO;

        BigDecimal discountAmount = subtotal.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal finalAmount = subtotal.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        return MembershipDiscountResult.builder()
                .customerId(customerId)
                .tierId(tier != null ? tier.getTierId() : null)
                .tierName(tier != null ? tier.getTierName() : "Thành viên")
                .discountPercent(discountPercent)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }

    /**
     * Mốc số đơn hàng hoàn thành tối thiểu tương ứng theo vị trí bậc hạng (chuẩn Shopee Rewards):
     * - Bậc 1 (Đồng): 0 đơn
     * - Bậc 2 (Bạc): 3 đơn
     * - Bậc 3 (Vàng): 10 đơn
     * - Bậc 4 (Kim Cương): 25 đơn
     */
    private int getRequiredOrdersForTierIndex(int index) {
        return switch (index) {
            case 0 -> 0;   // Đồng
            case 1 -> 3;   // Bạc
            case 2 -> 10;  // Vàng
            default -> 25; // Kim Cương trở lên
        };
    }

    /**
     * Tính ngày hết hạn chu kỳ 6 tháng theo chuẩn Shopee Rewards:
     * - Nếu hiện tại ở nửa đầu năm (Tháng 1 - 6) -> Hết hạn vào 30/06
     * - Nếu hiện tại ở nửa cuối năm (Tháng 7 - 12) -> Hết hạn vào 31/12
     */
    private String calculateCycleExpiryDate() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        return (now.getMonthValue() <= 6)
                ? String.format("30/06/%d", year)
                : String.format("31/12/%d", year);
    }

    /**
     * Validate theo quy tắc nghiệp vụ BR-LY-02:
     * - Không âm (minSpentRequired >= 0, discountPercent >= 0 và <= 100)
     * - Tên hạng không được trùng
     * - Ngưỡng chi tiêu không được trùng với hạng khác
     * - Quy tắc lũy tiến: Hạng chi tiêu cao hơn phải có tỷ lệ giảm giá >= hạng thấp hơn
     * - Bảo vệ hạng cơ sở (0 ₫) dành cho khách hàng mới
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

        // 1. Kiểm tra bảo vệ Hạng cơ sở (0 ₫) khi cập nhật
        if (currentId != null) {
            MembershipTier existingTier = getTierById(currentId);
            if (existingTier.getMinSpentRequired().compareTo(BigDecimal.ZERO) == 0
                    && request.getMinSpentRequired().compareTo(BigDecimal.ZERO) > 0) {
                boolean hasOtherZeroTier = allTiers.stream()
                        .anyMatch(t -> !Objects.equals(t.getTierId(), currentId) && t.getMinSpentRequired().compareTo(BigDecimal.ZERO) == 0);
                if (!hasOtherZeroTier) {
                    throw new IllegalArgumentException("Hệ thống bắt buộc phải duy trì ít nhất 1 hạng thành viên cơ sở với ngưỡng chi tiêu 0 ₫ dành cho khách hàng mới!");
                }
            }
        }

        // 2. Kiểm tra trùng lặp và tính lũy tiến hợp lý
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
            // Quy tắc lũy tiến:
            if (t.getMinSpentRequired().compareTo(request.getMinSpentRequired()) > 0
                    && t.getDiscountPercent().compareTo(request.getDiscountPercent()) < 0) {
                throw new IllegalArgumentException("Hạng '" + trimmedName + "' có chi tiêu yêu cầu thấp hơn hạng '" + t.getTierName() + "' nhưng tỷ lệ giảm giá lại cao hơn (" + request.getDiscountPercent() + "% > " + t.getDiscountPercent() + "%)!");
            }
            if (t.getMinSpentRequired().compareTo(request.getMinSpentRequired()) < 0
                    && t.getDiscountPercent().compareTo(request.getDiscountPercent()) > 0) {
                throw new IllegalArgumentException("Hạng '" + trimmedName + "' có chi tiêu yêu cầu cao hơn hạng '" + t.getTierName() + "' nhưng tỷ lệ giảm giá lại thấp hơn (" + request.getDiscountPercent() + "% < " + t.getDiscountPercent() + "%)!");
            }
        }
    }

    /**
     * Tự động tạo hoặc cập nhật Voucher đặc quyền hội viên (Sàn tài trợ) khi Admin cấu hình hạng
     */
    private void syncTierVoucher(MembershipTier tier) {
        if (tier == null || couponRepository == null) {
            return;
        }

        String tierName = (tier.getTierName() != null) ? tier.getTierName().toLowerCase().trim() : "";
        String couponCode;
        BigDecimal maxCap;
        int usageLimit;

        if (tierName.contains("kim cương") || tierName.contains("diamond")) {
            couponCode = "DIAMOND_VIP";
            maxCap = new BigDecimal("100000");
            usageLimit = 8;
        } else if (tierName.contains("vàng") || tierName.contains("gold")) {
            couponCode = "GOLD_VIP";
            maxCap = new BigDecimal("50000");
            usageLimit = 4;
        } else if (tierName.contains("bạc") || tierName.contains("silver")) {
            couponCode = "SILVER_VIP";
            maxCap = new BigDecimal("20000");
            usageLimit = 2;
        } else if (tier.getDiscountPercent() != null && tier.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            couponCode = "TIER_" + (tier.getTierId() != null ? tier.getTierId() : System.currentTimeMillis() % 1000) + "_VIP";
            maxCap = new BigDecimal("30000");
            usageLimit = 2;
        } else {
            return;
        }

        BigDecimal discount = (tier.getDiscountPercent() != null) ? tier.getDiscountPercent() : BigDecimal.ZERO;
        boolean isActive = discount.compareTo(BigDecimal.ZERO) > 0;

        try {
            Optional<Coupon> opt = couponRepository.findByCouponCodeIgnoreCase(couponCode);
            Coupon coupon = opt.orElseGet(Coupon::new);

            coupon.setCouponCode(couponCode);
            coupon.setShop(null); // Quy ước Sàn tài trợ (Platform coupon, không trừ tiền Shop)
            coupon.setCouponCategory(CouponCategory.PRODUCT);
            coupon.setDiscountType(DiscountType.PERCENT);
            coupon.setDiscountValue(discount);
            coupon.setMinOrderValue(new BigDecimal("100000"));
            coupon.setMaxDiscountAmount(maxCap);
            coupon.setUsageLimit(usageLimit);
            if (coupon.getUsedCount() == null) {
                coupon.setUsedCount(0);
            }
            coupon.setIsActive(isActive);
            if (coupon.getStartDate() == null) {
                coupon.setStartDate(LocalDateTime.now().minusDays(1));
            }
            if (coupon.getEndDate() == null) {
                coupon.setEndDate(LocalDateTime.now().plusYears(1));
            }

            couponRepository.save(coupon);
            log.info("Đã đồng bộ Coupon VIP sàn tài trợ cho hạng [{}]: Code={}, Discount={}% (isActive={})",
                    tier.getTierName(), couponCode, discount, isActive);
        } catch (Exception e) {
            log.warn("Lỗi khi đồng bộ Coupon VIP cho hạng {}: {}", tier.getTierName(), e.getMessage());
        }
    }

    private void deactivateTierVoucher(MembershipTier tier) {
        if (tier == null || couponRepository == null) return;
        try {
            String tierName = (tier.getTierName() != null) ? tier.getTierName().toLowerCase().trim() : "";
            String code = null;
            if (tierName.contains("kim cương") || tierName.contains("diamond")) code = "DIAMOND_VIP";
            else if (tierName.contains("vàng") || tierName.contains("gold")) code = "GOLD_VIP";
            else if (tierName.contains("bạc") || tierName.contains("silver")) code = "SILVER_VIP";
            else if (tier.getTierId() != null) code = "TIER_" + tier.getTierId() + "_VIP";

            if (code != null) {
                couponRepository.findByCouponCodeIgnoreCase(code).ifPresent(c -> {
                    c.setIsActive(false);
                    couponRepository.save(c);
                });
            }
        } catch (Exception e) {
            log.warn("Không thể vô hiệu hóa voucher khi xóa hạng {}: {}", tier.getTierName(), e.getMessage());
        }
    }

    /**
     * Lấy danh sách Voucher đặc quyền theo cấp bậc hiện tại (Shopee Rewards Style)
     */
    private List<TierVoucherResponse> getExclusiveVouchersForTier(int tierIndex, MembershipTier currentTier) {
        if (tierIndex <= 0 || currentTier == null) {
            return Collections.emptyList();
        }

        List<String> targetCodes = new ArrayList<>();
        if (tierIndex >= 3) { // Hạng Kim Cương
            targetCodes.add("DIAMOND_FREESHIP");
            targetCodes.add("DIAMOND_VIP");
            targetCodes.add("DIAMOND_VIP100K");
            targetCodes.add("DIAMOND_PERCENT15");
            targetCodes.add("GOLD_FREESHIP");
            targetCodes.add("GOLD_VIP");
            targetCodes.add("GOLD_DISCOUNT50K");
        } else if (tierIndex == 2) { // Hạng Vàng
            targetCodes.add("GOLD_FREESHIP");
            targetCodes.add("GOLD_VIP");
            targetCodes.add("GOLD_DISCOUNT50K");
            targetCodes.add("SILVER_FREESHIP");
            targetCodes.add("SILVER_VIP");
        } else if (tierIndex == 1) { // Hạng Bạc
            targetCodes.add("SILVER_FREESHIP");
            targetCodes.add("SILVER_VIP");
        }

        List<TierVoucherResponse> vouchers = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (String code : targetCodes) {
            Optional<Coupon> opt = (couponRepository != null) ? couponRepository.findByCouponCodeIgnoreCase(code) : Optional.empty();
            if (opt.isPresent()) {
                Coupon c = opt.get();
                String title;
                if (c.getDiscountType() == DiscountType.PERCENT) {
                    title = "Giảm " + c.getDiscountValue().stripTrailingZeros().toPlainString() + "%";
                } else if (c.getCouponCategory() == CouponCategory.SHIPPING) {
                    title = "Freeship " + formatVoucherMoney(c.getDiscountValue());
                } else {
                    title = "Giảm " + formatVoucherMoney(c.getDiscountValue());
                }

                String minSpend = (c.getMinOrderValue() != null)
                        ? "Đơn tối thiểu " + formatVoucherMoney(c.getMinOrderValue())
                        : "Không giới hạn đơn tối thiểu";

                String maxDiscount = (c.getMaxDiscountAmount() != null)
                        ? "Tối đa " + formatVoucherMoney(c.getMaxDiscountAmount())
                        : null;

                String expiry = (c.getEndDate() != null)
                        ? "HSD: " + c.getEndDate().format(dtf)
                        : "HSD: Đến hết chu kỳ";

                String badge = (c.getCouponCategory() == CouponCategory.SHIPPING) ? "FREESHIP" : "GIẢM GIÁ";

                vouchers.add(TierVoucherResponse.builder()
                        .couponCode(c.getCouponCode())
                        .title(title)
                        .description("Voucher đặc quyền toàn sàn Shopee Rewards")
                        .minOrderValueText(minSpend)
                        .maxDiscountText(maxDiscount)
                        .expiryDateText(expiry)
                        .badgeType(badge)
                        .targetTierName(currentTier.getTierName())
                        .isAvailable(Boolean.TRUE.equals(c.getIsActive()))
                        .build());
            } else {
                vouchers.add(createFallbackVoucher(code, currentTier));
            }
        }
        return vouchers;
    }

    private String formatVoucherMoney(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return String.format(Locale.GERMANY, "%,.0f ₫", amount);
    }

    private TierVoucherResponse createFallbackVoucher(String code, MembershipTier currentTier) {
        String tierName = (currentTier != null) ? currentTier.getTierName() : "";
        return switch (code) {
            case "SILVER_FREESHIP" -> TierVoucherResponse.builder()
                    .couponCode("SILVER_FREESHIP")
                    .title("Freeship 30.000 ₫")
                    .description("Miễn phí vận chuyển toàn sàn cho Hạng Bạc")
                    .minOrderValueText("Đơn tối thiểu 150.000 ₫")
                    .expiryDateText("HSD: Đến hết chu kỳ")
                    .badgeType("FREESHIP")
                    .targetTierName(tierName)
                    .isAvailable(true)
                    .build();
            case "SILVER_VIP" -> TierVoucherResponse.builder()
                    .couponCode("SILVER_VIP")
                    .title("Giảm " + (currentTier != null && currentTier.getDiscountPercent() != null ? currentTier.getDiscountPercent().stripTrailingZeros().toPlainString() : "3") + "%")
                    .description("Voucher VIP toàn sàn cho Hạng Bạc (Sàn tài trợ)")
                    .minOrderValueText("Đơn tối thiểu 100.000 ₫")
                    .maxDiscountText("Tối đa 20.000 ₫")
                    .expiryDateText("HSD: Đến hết chu kỳ")
                    .badgeType("GIẢM GIÁ")
                    .targetTierName(tierName)
                    .isAvailable(true)
                    .build();
            case "GOLD_FREESHIP" -> TierVoucherResponse.builder()
                    .couponCode("GOLD_FREESHIP")
                    .title("Freeship 40.000 ₫")
                    .description("Miễn phí vận chuyển toàn sàn cho Hạng Vàng")
                    .minOrderValueText("Đơn tối thiểu 200.000 ₫")
                    .expiryDateText("HSD: Đến hết chu kỳ")
                    .badgeType("FREESHIP")
                    .targetTierName(tierName)
                    .isAvailable(true)
                    .build();
            case "GOLD_VIP" -> TierVoucherResponse.builder()
                    .couponCode("GOLD_VIP")
                    .title("Giảm " + (currentTier != null && currentTier.getDiscountPercent() != null ? currentTier.getDiscountPercent().stripTrailingZeros().toPlainString() : "6") + "%")
                    .description("Voucher VIP toàn sàn cho Hạng Vàng (Sàn tài trợ)")
                    .minOrderValueText("Đơn tối thiểu 100.000 ₫")
                    .maxDiscountText("Tối đa 50.000 ₫")
                    .expiryDateText("HSD: Đến hết chu kỳ")
                    .badgeType("GIẢM GIÁ")
                    .targetTierName(tierName)
                    .isAvailable(true)
                    .build();
            case "GOLD_DISCOUNT50K" -> TierVoucherResponse.builder()
                    .couponCode("GOLD_DISCOUNT50K")
                    .title("Giảm 50.000 ₫")
                    .description("Voucher giảm giá hoa quả toàn sàn cho Hạng Vàng")
                    .minOrderValueText("Đơn tối thiểu 300.000 ₫")
                    .expiryDateText("HSD: Đến hết chu kỳ")
                    .badgeType("GIẢM GIÁ")
                    .targetTierName(tierName)
                    .isAvailable(true)
                    .build();
            case "DIAMOND_VIP" -> TierVoucherResponse.builder()
                    .couponCode("DIAMOND_VIP")
                    .title("Giảm " + (currentTier != null && currentTier.getDiscountPercent() != null ? currentTier.getDiscountPercent().stripTrailingZeros().toPlainString() : "10") + "%")
                    .description("Voucher VIP toàn sàn cho Hạng Kim Cương (Sàn tài trợ)")
                    .minOrderValueText("Đơn tối thiểu 100.000 ₫")
                    .maxDiscountText("Tối đa 100.000 ₫")
                    .expiryDateText("HSD: Đến hết chu kỳ")
                    .badgeType("GIẢM GIÁ")
                    .targetTierName(tierName)
                    .isAvailable(true)
                    .build();
            case "DIAMOND_FREESHIP" -> TierVoucherResponse.builder()
                    .couponCode("DIAMOND_FREESHIP")
                    .title("Freeship 50.000 ₫")
                    .description("Miễn phí vận chuyển toàn sàn VIP Kim Cương")
                    .minOrderValueText("Đơn tối thiểu 150.000 ₫")
                    .expiryDateText("HSD: Đến hết chu kỳ")
                    .badgeType("FREESHIP")
                    .targetTierName(tierName)
                    .isAvailable(true)
                    .build();
            case "DIAMOND_VIP100K" -> TierVoucherResponse.builder()
                    .couponCode("DIAMOND_VIP100K")
                    .title("Giảm 100.000 ₫")
                    .description("Voucher VIP giảm trực tiếp vào đơn hàng")
                    .minOrderValueText("Đơn tối thiểu 500.000 ₫")
                    .expiryDateText("HSD: Đến hết chu kỳ")
                    .badgeType("GIẢM GIÁ")
                    .targetTierName(tierName)
                    .isAvailable(true)
                    .build();
            case "DIAMOND_PERCENT15" -> TierVoucherResponse.builder()
                    .couponCode("DIAMOND_PERCENT15")
                    .title("Giảm 15%")
                    .description("Đặc quyền thượng đỉnh Shopee Rewards")
                    .minOrderValueText("Đơn tối thiểu 600.000 ₫")
                    .maxDiscountText("Tối đa 150.000 ₫")
                    .expiryDateText("HSD: Đến hết chu kỳ")
                    .badgeType("GIẢM GIÁ")
                    .targetTierName(tierName)
                    .isAvailable(true)
                    .build();
            default -> TierVoucherResponse.builder()
                    .couponCode(code)
                    .title("Ưu đãi đặc quyền")
                    .description("Voucher toàn sàn")
                    .minOrderValueText("Áp dụng theo điều kiện")
                    .expiryDateText("HSD: Đến hết chu kỳ")
                    .badgeType("GIẢM GIÁ")
                    .targetTierName(tierName)
                    .isAvailable(true)
                    .build();
        };
    }
}
