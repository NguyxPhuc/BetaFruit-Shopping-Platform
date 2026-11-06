package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.MembershipTierRequest;
import com.fu.SWP391_BetaFruit.dto.response.CustomerMembershipResponse;
import com.fu.SWP391_BetaFruit.dto.response.TierUpgradeResult;
import com.fu.SWP391_BetaFruit.dto.response.TierVoucherResponse;
import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.entity.CustomerMembership;
import com.fu.SWP391_BetaFruit.entity.MembershipTier;
import com.fu.SWP391_BetaFruit.entity.TierCoupon;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.CouponRepository;
import com.fu.SWP391_BetaFruit.repository.CustomerMembershipRepository;
import com.fu.SWP391_BetaFruit.repository.MembershipTierRepository;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.TierCouponRepository;
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
    private final TierCouponRepository tierCouponRepository;

    private static final List<OrderStatus> COMPLETED_ORDER_STATUSES = List.of(OrderStatus.RECEIVED, OrderStatus.SUCCESS);

    // =========================================================================
    // 1. PHÍA CUSTOMER (BR-LY-01, BR-LY-02, BR-LY-03 & TIÊU CHÍ KÉP SHOPEE)
    // =========================================================================
    @Override
    @Transactional
    public CustomerMembership syncCustomerMembershipTier(Integer userId) {
        if (userId == null) {
            return null;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        CustomerMembership membership = customerMembershipRepository.findByUserId(userId).orElse(null);
        BigDecimal totalSpent = (membership != null && membership.getTotalCumulativeSpend() != null)
                ? membership.getTotalCumulativeSpend()
                : BigDecimal.ZERO;

        long completedOrders = (orderRepository != null)
                ? orderRepository.countByCustomer_UserIdAndOrderStatusIn(userId, COMPLETED_ORDER_STATUSES)
                : 0L;

        List<MembershipTier> sortedTiers = membershipTierRepository.findAllByOrderByMinSpentRequiredAsc();
        if (sortedTiers.isEmpty()) {
            return membership;
        }

        MembershipTier qualifiedTier = sortedTiers.get(0);
        for (MembershipTier tier : sortedTiers) {
            int minOrders = (tier.getMinOrdersRequired() != null) ? tier.getMinOrdersRequired() : 0;
            BigDecimal tierMinSpent = (tier.getMinSpentRequired() != null) ? tier.getMinSpentRequired() : BigDecimal.ZERO;
            boolean spendPassed = totalSpent.compareTo(tierMinSpent) >= 0;
            boolean ordersPassed = (minOrders > 0) && (completedOrders >= minOrders);

            if (spendPassed || ordersPassed) {
                qualifiedTier = tier;
            } else {
                break;
            }
        }

        if (membership == null) {
            CustomerMembership newMembership = new CustomerMembership();
            newMembership.setUser(user);
            newMembership.setTier(qualifiedTier);
            newMembership.setTotalCumulativeSpend(totalSpent);
            return customerMembershipRepository.save(newMembership);
        } else if (membership.getTier() == null || !Objects.equals(membership.getTier().getTierId(), qualifiedTier.getTierId())) {
            membership.setTier(qualifiedTier);
            return customerMembershipRepository.save(membership);
        }
        return membership;
    }

    @Override
    @Transactional
    public void syncAllCustomersMembershipTier() {
        List<CustomerMembership> allMemberships = customerMembershipRepository.findAll();
        for (CustomerMembership cm : allMemberships) {
            if (cm.getUser() != null && cm.getUser().getUserId() != null) {
                syncCustomerMembershipTier(cm.getUser().getUserId());
            }
        }
    }

    @Override
    @Transactional
    public CustomerMembershipResponse getCustomerMembershipData(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin người dùng với ID: " + userId));

        // Tự động đối soát và đồng bộ hạng chuẩn xác vào Database
        CustomerMembership membership = syncCustomerMembershipTier(userId);
        if (membership == null) {
            membership = customerMembershipRepository.findByUserId(userId).orElse(null);
        }
        BigDecimal totalSpent = (membership != null && membership.getTotalCumulativeSpend() != null)
                ? membership.getTotalCumulativeSpend()
                : BigDecimal.ZERO;

        // Đếm tổng số đơn hoàn tất của khách theo thời gian thực (Shopee Rewards)
        long completedOrders = orderRepository.countByCustomer_UserIdAndOrderStatusIn(userId, COMPLETED_ORDER_STATUSES);

        // Lấy tất cả các hạng thành viên được sắp xếp tăng dần theo MinSpentRequired (BR-LY-02)
        List<MembershipTier> sortedTiers = membershipTierRepository.findAllByOrderByMinSpentRequiredAsc();

        // Xác định hạng cao nhất mà Customer đạt được theo TIÊU CHÍ KÉP SHOPEE (BR-LY-03):
        // Thỏa mãn Đạt mốc chi tiêu HOẶC Đạt mốc số đơn hoàn tất
        MembershipTier currentTier = null;
        MembershipTier nextTier = null;
        int currentTierIndex = -1;

        for (int i = 0; i < sortedTiers.size(); i++) {
            MembershipTier tier = sortedTiers.get(i);
            int minOrders = (tier.getMinOrdersRequired() != null) ? tier.getMinOrdersRequired() : 0;
            BigDecimal tierMinSpent = (tier.getMinSpentRequired() != null) ? tier.getMinSpentRequired() : BigDecimal.ZERO;
            boolean spendPassed = totalSpent.compareTo(tierMinSpent) >= 0;
            boolean ordersPassed = (minOrders > 0) && (completedOrders >= minOrders);

            if (spendPassed || ordersPassed) {
                currentTier = tier; // Cập nhật hạng cao nhất thỏa mãn
                currentTierIndex = i;
            } else {
                // Hạng đầu tiên chưa đạt chính là nextTier
                nextTier = tier;
                break;
            }
        }

        // Tự động đồng bộ lại vào DB nếu có sự thay đổi
        if (membership != null && currentTier != null) {
            if (membership.getTier() == null || !Objects.equals(membership.getTier().getTierId(), currentTier.getTierId())) {
                membership.setTier(currentTier);
                customerMembershipRepository.save(membership);
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
            nextTierRequiredOrders = (nextTier.getMinOrdersRequired() != null) ? nextTier.getMinOrdersRequired() : 0;
            ordersNeeded = Math.max(0, nextTierRequiredOrders - (int) completedOrders);
            if (nextTierRequiredOrders > 0) {
                progressOrdersPercentage = Math.min(100.0, ((double) completedOrders / nextTierRequiredOrders) * 100.0);
            } else {
                progressOrdersPercentage = 100.0;
            }

            BigDecimal nextMinSpent = (nextTier.getMinSpentRequired() != null) ? nextTier.getMinSpentRequired() : BigDecimal.ZERO;
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
                    BigDecimal.ZERO,
                    isCur,
                    (t.getMinOrdersRequired() != null) ? t.getMinOrdersRequired() : 0
            ));
        }

        return CustomerMembershipResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .totalCumulativeSpend(totalSpent)
                .currentTierId(currentTier != null ? currentTier.getTierId() : null)
                .currentTierName(currentTier != null ? currentTier.getTierName() : "Thành viên")
                .currentDiscountPercent(BigDecimal.ZERO)
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
                .tierVouchers(getExclusiveVouchersForTier(user.getUserId(), currentTierIndex, currentTier))
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
        tier.setMinOrdersRequired(request.getMinOrdersRequired() != null ? request.getMinOrdersRequired() : 0);

        MembershipTier saved = membershipTierRepository.save(tier);
        syncAllCustomersMembershipTier();

    }

    @Override
    @Transactional
    public void updateTier(Integer id, MembershipTierRequest request) {
        MembershipTier tier = getTierById(id);
        validateTierRequest(request, id);

        tier.setTierName(request.getTierName().trim());
        tier.setMinSpentRequired(request.getMinSpentRequired());
        tier.setMinOrdersRequired(request.getMinOrdersRequired() != null ? request.getMinOrdersRequired() : 0);

        MembershipTier saved = membershipTierRepository.save(tier);
        syncAllCustomersMembershipTier();

    }

    @Override
    @Transactional
    public void deleteTier(Integer id) {
        MembershipTier tier = getTierById(id);

        // Kiểm tra an toàn ràng buộc khóa ngoại: có khách hàng nào đang ở hạng này không
        if (customerMembershipRepository.existsByTier_TierId(id)) {
            throw new IllegalStateException("Không thể xóa hạng '" + tier.getTierName() + "' vì đang có khách hàng thuộc hạng này!");
        }

        if (tierCouponRepository != null) {
            tierCouponRepository.deleteByTier_TierId(id);
        }
        membershipTierRepository.delete(tier);

    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminMembershipPageData() {
        List<MembershipTier> tiers = getAllTiers();
        List<Coupon> platformCoupons = getAvailablePlatformCoupons();

        Map<Integer, List<Coupon>> tierCouponsMap = new HashMap<>();
        Map<Integer, Set<Integer>> tierCouponIdsMap = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> tierCouponQuotaMap = new HashMap<>();

        for (MembershipTier tier : tiers) {
            List<TierCoupon> links = (tierCouponRepository != null)
                    ? tierCouponRepository.findByTier_TierId(tier.getTierId())
                    : Collections.emptyList();
            List<Coupon> assigned = links.stream()
                    .map(TierCoupon::getCoupon)
                    .filter(Objects::nonNull)
                    .toList();
            tierCouponsMap.put(tier.getTierId(), assigned);

            Set<Integer> ids = new HashSet<>();
            Map<Integer, Integer> quotaMap = new HashMap<>();
            for (TierCoupon tc : links) {
                if (tc.getCoupon() != null && tc.getCoupon().getCouponId() != null) {
                    ids.add(tc.getCoupon().getCouponId());
                    quotaMap.put(tc.getCoupon().getCouponId(), tc.getEffectiveUsageQuota());
                }
            }
            tierCouponIdsMap.put(tier.getTierId(), ids);
            tierCouponQuotaMap.put(tier.getTierId(), quotaMap);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("tiers", tiers);
        data.put("totalTiers", tiers.size());
        data.put("newTier", new MembershipTierRequest());
        data.put("platformCoupons", platformCoupons);
        data.put("tierCouponsMap", tierCouponsMap);
        data.put("tierCouponIdsMap", tierCouponIdsMap);
        data.put("tierCouponQuotaMap", tierCouponQuotaMap);
        return data;
    }

    // =========================================================================
    // QUẢN LÝ GÓI VOUCHER ĐẶC QUYỀN HỘI VIÊN (SHOPEE REWARDS N-N BUNDLE)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public List<Coupon> getAvailablePlatformCoupons() {
        if (couponRepository == null) return Collections.emptyList();
        return couponRepository.findByShopIsNullAndIsActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Coupon> getCouponsForTier(Integer tierId) {
        if (tierCouponRepository == null || tierId == null) return Collections.emptyList();
        return tierCouponRepository.findByTier_TierId(tierId).stream()
                .map(TierCoupon::getCoupon)
                .filter(c -> c != null && Boolean.TRUE.equals(c.getIsActive()))
                .toList();
    }

    @Override
    @Transactional
    public void updateTierCoupons(Integer tierId, List<Integer> couponIds) {
        updateTierCoupons(tierId, couponIds, Collections.emptyMap());
    }

    @Override
    @Transactional
    public void updateTierCoupons(Integer tierId, List<Integer> couponIds, Map<Integer, Integer> quotaMap) {
        MembershipTier tier = membershipTierRepository.findById(tierId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hạng thành viên với ID: " + tierId));

        if (tierCouponRepository != null) {
            tierCouponRepository.deleteByTier_TierId(tierId);
        }

        if (couponIds == null || couponIds.isEmpty()) {
            log.info("Đã xóa toàn bộ voucher trong gói của hạng {}", tier.getTierName());
            return;
        }

        List<Coupon> selectedCoupons = couponRepository.findAllById(couponIds);
        List<TierCoupon> newLinks = new ArrayList<>();
        for (Coupon coupon : selectedCoupons) {
            if (coupon.getShop() != null) {
                throw new IllegalArgumentException("Mã [" + coupon.getCouponCode() + "] là mã riêng của Shop, không thể đưa vào gói quyền lợi Hạng của Sàn!");
            }
            Integer quota = (quotaMap != null) ? quotaMap.get(coupon.getCouponId()) : null;
            if (quota == null || quota <= 0) {
                String name = (tier.getTierName() != null) ? tier.getTierName().toLowerCase() : "";
                if (name.contains("kim cương") || name.contains("diamond") || name.contains("vip")) {
                    quota = 3;
                } else if (name.contains("vàng") || name.contains("gold")) {
                    quota = 2;
                } else {
                    quota = 1;
                }
            }
            newLinks.add(new TierCoupon(tier, coupon, quota));
        }

        if (tierCouponRepository != null) {
            tierCouponRepository.saveAll(newLinks);
        }
        log.info("Cập nhật thành công gói voucher cho hạng {}: {} mã", tier.getTierName(), newLinks.size());
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
        long completedOrders = orderRepository.countByCustomer_UserIdAndOrderStatusIn(customerId, COMPLETED_ORDER_STATUSES);

        // Lấy danh sách Tiers sắp xếp tăng dần theo MinSpent
        List<MembershipTier> sortedTiers = membershipTierRepository.findAllByOrderByMinSpentRequiredAsc();

        MembershipTier currentTierInDb = membership.getTier();
        MembershipTier qualifiedTier = null;

        // Quét tìm hạng cao nhất theo tiêu chí kép:
        // Đạt chi tiêu tối thiểu HOẶC đạt số đơn hoàn tất tối thiểu
        for (int i = 0; i < sortedTiers.size(); i++) {
            MembershipTier t = sortedTiers.get(i);
            int minOrders = (t.getMinOrdersRequired() != null) ? t.getMinOrdersRequired() : 0;
            boolean spendPassed = newSpend.compareTo(t.getMinSpentRequired()) >= 0;
            boolean ordersPassed = (minOrders > 0) && (completedOrders >= minOrders);

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

            // Lấy danh sách Voucher đặc quyền thực tế từ cấu hình gói của hạng này
            List<TierCoupon> tierCouponMappings = (tierCouponRepository != null && qualifiedTier.getTierId() != null)
                    ? tierCouponRepository.findByTier_TierId(qualifiedTier.getTierId())
                    : Collections.emptyList();

            List<String> activeCouponCodes = (tierCouponMappings != null)
                    ? tierCouponMappings.stream()
                            .map(TierCoupon::getCoupon)
                            .filter(c -> c != null && Boolean.TRUE.equals(c.getIsActive()))
                            .map(Coupon::getCouponCode)
                            .filter(code -> code != null && !code.trim().isEmpty())
                            .toList()
                    : Collections.emptyList();

            String welcomeCoupon = null;
            String voucherNotice;
            if (!activeCouponCodes.isEmpty()) {
                welcomeCoupon = String.join(", ", activeCouponCodes);
                voucherNotice = String.format(" Nhận ngay Voucher độc quyền (%s) tại trang Hội viên!", welcomeCoupon);
            } else {
                voucherNotice = " Khám phá ngay các quyền lợi ưu đãi tại trang Hội viên!";
            }

            String customerName = (user.getFullName() != null && !user.getFullName().trim().isEmpty())
                    ? user.getFullName()
                    : user.getUsername();

            String title = "🎉 Chúc mừng bạn đã thăng hạng " + qualifiedTier.getTierName() + "!";
            String message = String.format(
                    "Chúc mừng %s đã đạt mốc hội viên %s của BetaFruit!%s",
                    customerName,
                    qualifiedTier.getTierName(),
                    voucherNotice
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
                    .newDiscountPercent(BigDecimal.ZERO)
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
     * - Không âm (minSpentRequired >= 0, minOrdersRequired >= 0)
     * - Tên hạng không được trùng
     * - Ngưỡng chi tiêu không được trùng với hạng khác
     * - Quy tắc lũy tiến: Hạng chi tiêu cao hơn phải có số đơn yêu cầu >= hạng thấp hơn
     * - Bảo vệ hạng cơ sở (0 ₫, 0 đơn) dành cho khách hàng mới
     */
    private void validateTierRequest(MembershipTierRequest request, Integer currentId) {
        if (request.getTierName() == null || request.getTierName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên hạng thành viên không được để trống!");
        }
        if (request.getMinSpentRequired() == null || request.getMinSpentRequired().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("⚠️ Mức chi tiêu tích lũy yêu cầu phải lớn hơn hoặc bằng 0 ₫.");
        }
        if (request.getMinOrdersRequired() == null || request.getMinOrdersRequired() < 0) {
            throw new IllegalArgumentException("⚠️ Số đơn hàng hoàn tất yêu cầu phải lớn hơn hoặc bằng 0 đơn.");
        }

        String trimmedName = request.getTierName().trim();
        List<MembershipTier> allTiers = membershipTierRepository.findAll();

        // 1. Kiểm tra bảo vệ Hạng cơ sở (0 ₫ và 0 đơn) khi cập nhật
        if (currentId != null) {
            MembershipTier existingTier = getTierById(currentId);
            if (existingTier.getMinSpentRequired().compareTo(BigDecimal.ZERO) == 0
                    && (existingTier.getMinOrdersRequired() == null || existingTier.getMinOrdersRequired() == 0)
                    && (request.getMinSpentRequired().compareTo(BigDecimal.ZERO) > 0 || request.getMinOrdersRequired() > 0)) {
                boolean hasOtherZeroTier = allTiers.stream()
                        .anyMatch(t -> !Objects.equals(t.getTierId(), currentId)
                                && t.getMinSpentRequired().compareTo(BigDecimal.ZERO) == 0
                                && (t.getMinOrdersRequired() == null || t.getMinOrdersRequired() == 0));
                if (!hasOtherZeroTier) {
                    throw new IllegalArgumentException("⚠️ Bắt buộc phải duy trì ít nhất 1 hạng cơ sở với mức chi tiêu 0 ₫ và 0 đơn cho thành viên mới.");
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
            // Quy tắc lũy tiến số đơn: Hạng có chi tiêu yêu cầu cao hơn phải có số đơn yêu cầu >= hạng thấp hơn
            if (t.getMinOrdersRequired() != null && request.getMinOrdersRequired() != null) {
                if (t.getMinSpentRequired().compareTo(request.getMinSpentRequired()) > 0
                        && t.getMinOrdersRequired() < request.getMinOrdersRequired()) {
                    throw new IllegalArgumentException("Hạng '" + trimmedName + "' có chi tiêu yêu cầu thấp hơn nhưng số đơn yêu cầu lại cao hơn hạng '" + t.getTierName() + "' (" + request.getMinOrdersRequired() + " > " + t.getMinOrdersRequired() + " đơn)!");
                }
                if (t.getMinSpentRequired().compareTo(request.getMinSpentRequired()) < 0
                        && t.getMinOrdersRequired() > request.getMinOrdersRequired()) {
                    throw new IllegalArgumentException("Hạng '" + trimmedName + "' có chi tiêu yêu cầu cao hơn nhưng số đơn yêu cầu lại thấp hơn hạng '" + t.getTierName() + "' (" + request.getMinOrdersRequired() + " < " + t.getMinOrdersRequired() + " đơn)!");
                }
            }
        }
    }
    /**
     * Lấy danh sách Voucher đặc quyền theo cấp bậc hiện tại (Shopee Rewards Style)
     */
    private List<TierVoucherResponse> getExclusiveVouchersForTier(Integer userId, int tierIndex, MembershipTier currentTier) {
        if (tierIndex <= 0 || currentTier == null || currentTier.getTierId() == null || tierCouponRepository == null) {
            return Collections.emptyList();
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<TierCoupon> configuredList = tierCouponRepository.findByTier_TierId(currentTier.getTierId());
        if (configuredList == null || configuredList.isEmpty()) {
            return Collections.emptyList();
        }

        List<TierVoucherResponse> vouchers = new ArrayList<>();
        for (TierCoupon tc : configuredList) {
            Coupon c = tc.getCoupon();
            if (c != null && Boolean.TRUE.equals(c.getIsActive())) {
                long usedCount = 0;
                if (userId != null && orderRepository != null && c.getCouponId() != null) {
                    usedCount = orderRepository.countValidUsedCouponByCustomer(userId, c.getCouponId());
                }
                int totalQuota = tc.getEffectiveUsageQuota();
                long remainingQuota = Math.max(0, totalQuota - usedCount);
                boolean isUsed = (remainingQuota <= 0);

                vouchers.add(mapCouponToTierVoucherResponse(c, currentTier, dtf, isUsed, totalQuota, usedCount, remainingQuota));
            }
        }
        return vouchers;
    }

    private TierVoucherResponse mapCouponToTierVoucherResponse(Coupon c, MembershipTier currentTier, DateTimeFormatter dtf, boolean isUsed, int totalQuota, long usedCount, long remainingQuota) {
        BigDecimal discountVal = (c.getDiscountValue() != null) ? c.getDiscountValue() : BigDecimal.ZERO;
        String title;
        if (c.getDiscountType() == DiscountType.PERCENT) {
            title = "Giảm " + discountVal.stripTrailingZeros().toPlainString() + "%";
        } else if (c.getCouponCategory() == CouponCategory.SHIPPING) {
            title = "Freeship " + formatVoucherMoney(discountVal);
        } else {
            title = "Giảm " + formatVoucherMoney(discountVal);
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
        String quotaBadge = "x" + totalQuota + " lượt";
        String usageProgress = isUsed
                ? "Đã dùng hết (" + totalQuota + "/" + totalQuota + " lượt)"
                : "Còn lại " + remainingQuota + "/" + totalQuota + " lượt";

        return TierVoucherResponse.builder()
                .couponCode(c.getCouponCode())
                .title(title)
                .description("Voucher đặc quyền toàn sàn Shopee Rewards")
                .minOrderValueText(minSpend)
                .maxDiscountText(maxDiscount)
                .expiryDateText(expiry)
                .badgeType(badge)
                .targetTierName(currentTier != null ? currentTier.getTierName() : "")
                .isAvailable(Boolean.TRUE.equals(c.getIsActive()))
                .isUsed(isUsed)
                .totalQuota(totalQuota)
                .usedCount(usedCount)
                .remainingQuota(remainingQuota)
                .quotaBadgeText(quotaBadge)
                .usageProgressText(usageProgress)
                .build();
    }

    private String formatVoucherMoney(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return String.format(Locale.GERMANY, "%,.0f ₫", amount);
    }
}
