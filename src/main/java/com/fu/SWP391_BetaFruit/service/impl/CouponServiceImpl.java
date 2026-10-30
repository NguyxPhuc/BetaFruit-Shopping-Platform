package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.CouponRequest;
import com.fu.SWP391_BetaFruit.dto.response.CouponResponse;
import com.fu.SWP391_BetaFruit.dto.response.*;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.entity.MembershipTier;
import com.fu.SWP391_BetaFruit.entity.TierCoupon;
import com.fu.SWP391_BetaFruit.repository.*;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import com.fu.SWP391_BetaFruit.repository.CouponRepository;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;
    private final OrderItemRepository orderItemRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final CustomerMembershipRepository customerMembershipRepository;
    private final TierCouponRepository tierCouponRepository;

    // =========================================================================
    // SHOP OWNER METHODS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getShopCouponsPageData(Integer userId, String keyword, String status, int page, int size) {
        Shop shop = getShopForUser(userId);

        if (page < 1) page = 1;
        if (size < 1) size = 8;

        String cleanKeyword = (keyword != null) ? keyword.trim().toLowerCase() : "";
        String cleanStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "ALL";

        Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "couponId"));
        Page<Coupon> allCouponsPage;

        if (!cleanKeyword.isEmpty()) {
            allCouponsPage = couponRepository.findByShop_ShopIdAndCouponCodeContainingIgnoreCase(shop.getShopId(), cleanKeyword, pageable);
        } else {
            allCouponsPage = couponRepository.findByShop_ShopId(shop.getShopId(), pageable);
        }

        List<CouponResponse> allResponses = allCouponsPage.getContent().stream()
                .map(CouponResponse::fromEntity)
                .collect(Collectors.toList());

        // Calculate KPI stats
        long totalCount = allResponses.size();
        long activeCount = allResponses.stream().filter(CouponResponse::isValidNow).count();
        long expiredCount = allResponses.stream().filter(CouponResponse::isExpired).count();
        long outOfStockCount = allResponses.stream().filter(CouponResponse::isOutOfStock).count();

        // Filter by status tab
        List<CouponResponse> filteredList = allResponses.stream().filter(c -> {
            switch (cleanStatus) {
                case "ACTIVE":
                    return c.isValidNow();
                case "PAUSED":
                    return !Boolean.TRUE.equals(c.getIsActive());
                case "UPCOMING":
                    return c.isUpcoming();
                case "EXPIRED":
                    return c.isExpired();
                case "OUT_OF_STOCK":
                    return c.isOutOfStock();
                case "ALL":
                default:
                    return true;
            }
        }).collect(Collectors.toList());

        // In-memory pagination
        int totalElements = filteredList.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<CouponResponse> pagedContent = (fromIndex <= totalElements) ? filteredList.subList(fromIndex, toIndex) : Collections.emptyList();

        Page<CouponResponse> couponPage = new PageImpl<>(pagedContent, PageRequest.of(page - 1, size), totalElements);

        Map<String, Object> data = new HashMap<>();
        data.put("shop", shop);
        data.put("coupons", pagedContent);
        data.put("couponPage", couponPage);
        data.put("currentPage", page);
        data.put("totalPages", totalPages);
        data.put("totalItems", totalElements);
        data.put("keyword", keyword != null ? keyword.trim() : "");
        data.put("status", cleanStatus);
        data.put("totalCount", totalCount);
        data.put("activeCount", activeCount);
        data.put("expiredCount", expiredCount);
        data.put("outOfStockCount", outOfStockCount);

        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon getShopCouponById(Integer userId, Integer couponId) {
        Shop shop = getShopForUser(userId);
        return couponRepository.findByCouponIdAndShop_ShopId(couponId, shop.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá hoặc mã không thuộc quyền quản lý của cửa hàng bạn."));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponRequest getShopCouponRequestForEdit(Integer userId, Integer couponId) {
        Coupon coupon = getShopCouponById(userId, couponId);
        CouponRequest request = new CouponRequest();
        request.setCouponId(coupon.getCouponId());
        request.setCouponCode(coupon.getCouponCode());
        request.setCouponCategory(coupon.getCouponCategory());
        request.setDiscountType(coupon.getDiscountType());
        request.setDiscountValue(coupon.getDiscountValue());
        request.setMinOrderValue(coupon.getMinOrderValue());
        request.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        request.setUsageLimit(coupon.getUsageLimit());
        request.setStartDate(coupon.getStartDate());
        request.setEndDate(coupon.getEndDate());
        request.setIsActive(coupon.getIsActive());
        return request;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Coupon createShopCoupon(Integer userId, CouponRequest request) {
        Shop shop = getShopForUser(userId);
        validateCouponRequest(request, null);

        Coupon coupon = new Coupon();
        coupon.setShop(shop);
        coupon.setCouponCode(request.getCouponCode().trim().toUpperCase());
        coupon.setCouponCategory(request.getCouponCategory());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO);
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setUsedCount(0);
        coupon.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());

        return couponRepository.save(coupon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Coupon updateShopCoupon(Integer userId, Integer couponId, CouponRequest request) {
        Coupon coupon = getShopCouponById(userId, couponId);
        validateCouponRequest(request, couponId);

        boolean hasBeenUsed = (coupon.getUsedCount() != null && coupon.getUsedCount() > 0);

        if (hasBeenUsed) {
            if (request.getUsageLimit() < coupon.getUsedCount()) {
                throw new IllegalArgumentException("Mã này đã được sử dụng " + coupon.getUsedCount()
                        + " lần, tổng lượt sử dụng không được nhỏ hơn số lượt đã dùng.");
            }
            coupon.setUsageLimit(request.getUsageLimit());
            coupon.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO);
            coupon.setEndDate(request.getEndDate());
            if (request.getIsActive() != null) {
                coupon.setIsActive(request.getIsActive());
            }
        } else {
            coupon.setCouponCode(request.getCouponCode().trim().toUpperCase());
            coupon.setCouponCategory(request.getCouponCategory());
            coupon.setDiscountType(request.getDiscountType());
            coupon.setDiscountValue(request.getDiscountValue());
            coupon.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO);
            coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
            coupon.setUsageLimit(request.getUsageLimit());
            coupon.setStartDate(request.getStartDate());
            coupon.setEndDate(request.getEndDate());
            if (request.getIsActive() != null) {
                coupon.setIsActive(request.getIsActive());
            }
        }

        return couponRepository.save(coupon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleShopCouponStatus(Integer userId, Integer couponId) {
        Coupon coupon = getShopCouponById(userId, couponId);
        boolean newStatus = !Boolean.TRUE.equals(coupon.getIsActive());
        coupon.setIsActive(newStatus);
        couponRepository.save(coupon);
        return newStatus;
    }

    @Override
    @Transactional(readOnly = true)
    public CouponUsageResponse getShopCouponUsage(Integer userId, Integer couponId, int page, int size) {
        Shop shop = getShopForUser(userId);
        Coupon coupon = getShopCouponById(userId, couponId);

        if (page < 1) page = 1;
        if (size < 1) size = 10;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "orderId"));
        Page<Order> orders = orderRepository.findByCoupon_CouponIdAndShop_ShopId(couponId, shop.getShopId(), pageable);
        BigDecimal totalDiscount = orderRepository.sumTotalDiscountByCouponIdAndShopId(couponId, shop.getShopId());

        CouponResponse couponResponse = CouponResponse.fromEntity(coupon);
        long used = (coupon.getUsedCount() != null) ? coupon.getUsedCount() : 0;
        long limit = (coupon.getUsageLimit() != null) ? coupon.getUsageLimit() : 0;
        long remaining = Math.max(0, limit - used);

        return CouponUsageResponse.builder()
                .coupon(couponResponse)
                .totalUsed(used)
                .usageLimit(limit)
                .remainingLimit(remaining)
                .usagePercentage(couponResponse.getUsagePercent())
                .totalDiscountSponsored(totalDiscount)
                .orders(orders)
                .build();
    }

    // =========================================================================
    // ADMIN METHODS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminCouponsPageData(String keyword, String scope, String status, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;

        String cleanKeyword = (keyword != null) ? keyword.trim().toLowerCase() : "";
        String cleanScope = (scope != null && !scope.trim().isEmpty()) ? scope.trim().toUpperCase() : "PLATFORM";
        String cleanStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "ALL";

        Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "couponId"));
        Page<Coupon> allCouponsPage;

        if ("PLATFORM".equals(cleanScope)) {
            if (!cleanKeyword.isEmpty()) {
                allCouponsPage = couponRepository.findByShopIsNullAndCouponCodeContainingIgnoreCase(cleanKeyword, pageable);
            } else {
                allCouponsPage = couponRepository.findByShopIsNull(pageable);
            }
        } else {
            if (!cleanKeyword.isEmpty()) {
                allCouponsPage = couponRepository.findByCouponCodeContainingIgnoreCase(cleanKeyword, pageable);
            } else {
                allCouponsPage = couponRepository.findAll(pageable);
            }
        }

        List<CouponResponse> allResponses = allCouponsPage.getContent().stream()
                .map(CouponResponse::fromEntity)
                .collect(Collectors.toList());

        // Calculate KPI stats
        long totalCount = allResponses.size();
        long activeCount = allResponses.stream().filter(CouponResponse::isValidNow).count();
        long expiredCount = allResponses.stream().filter(CouponResponse::isExpired).count();
        long outOfStockCount = allResponses.stream().filter(CouponResponse::isOutOfStock).count();

        // Filter by status tab
        List<CouponResponse> filteredList = allResponses.stream().filter(c -> {
            switch (cleanStatus) {
                case "ACTIVE":
                    return c.isValidNow();
                case "PAUSED":
                    return !Boolean.TRUE.equals(c.getIsActive());
                case "UPCOMING":
                    return c.isUpcoming();
                case "EXPIRED":
                    return c.isExpired();
                case "OUT_OF_STOCK":
                    return c.isOutOfStock();
                case "ALL":
                default:
                    return true;
            }
        }).collect(Collectors.toList());

        // In-memory pagination
        int totalElements = filteredList.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (totalPages == 0) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<CouponResponse> pagedContent = (fromIndex <= totalElements) ? filteredList.subList(fromIndex, toIndex) : Collections.emptyList();

        Page<CouponResponse> couponPage = new PageImpl<>(pagedContent, PageRequest.of(page - 1, size), totalElements);

        Map<String, Object> data = new HashMap<>();
        data.put("coupons", pagedContent);
        data.put("couponPage", couponPage);
        data.put("currentPage", page);
        data.put("totalPages", totalPages);
        data.put("totalItems", totalElements);
        data.put("keyword", keyword != null ? keyword.trim() : "");
        data.put("scope", cleanScope);
        data.put("status", cleanStatus);
        data.put("totalCount", totalCount);
        data.put("activeCount", activeCount);
        data.put("expiredCount", expiredCount);
        data.put("outOfStockCount", outOfStockCount);

        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon getAdminCouponById(Integer couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá với ID: " + couponId));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponRequest getAdminCouponRequestForEdit(Integer couponId) {
        Coupon coupon = getAdminCouponById(couponId);
        CouponRequest request = new CouponRequest();
        request.setCouponId(coupon.getCouponId());
        request.setCouponCode(coupon.getCouponCode());
        request.setCouponCategory(coupon.getCouponCategory());
        request.setDiscountType(coupon.getDiscountType());
        request.setDiscountValue(coupon.getDiscountValue());
        request.setMinOrderValue(coupon.getMinOrderValue());
        request.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        request.setUsageLimit(coupon.getUsageLimit());
        request.setStartDate(coupon.getStartDate());
        request.setEndDate(coupon.getEndDate());
        request.setIsActive(coupon.getIsActive());
        return request;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Coupon createAdminCoupon(CouponRequest request) {
        validateCouponRequest(request, null);

        Coupon coupon = new Coupon();
        coupon.setShop(null); // Platform-wide coupon
        coupon.setCouponCode(request.getCouponCode().trim().toUpperCase());
        coupon.setCouponCategory(request.getCouponCategory());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO);
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setUsedCount(0);
        coupon.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());

        return couponRepository.save(coupon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Coupon updateAdminCoupon(Integer couponId, CouponRequest request) {
        Coupon coupon = getAdminCouponById(couponId);
        validateCouponRequest(request, couponId);

        boolean hasBeenUsed = (coupon.getUsedCount() != null && coupon.getUsedCount() > 0);

        if (hasBeenUsed) {
            if (request.getUsageLimit() < coupon.getUsedCount()) {
                throw new IllegalArgumentException("Mã này đã được sử dụng " + coupon.getUsedCount()
                        + " lần, tổng lượt sử dụng không được nhỏ hơn số lượt đã dùng.");
            }
            coupon.setUsageLimit(request.getUsageLimit());
            coupon.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO);
            coupon.setEndDate(request.getEndDate());
            if (request.getIsActive() != null) {
                coupon.setIsActive(request.getIsActive());
            }
        } else {
            coupon.setCouponCode(request.getCouponCode().trim().toUpperCase());
            coupon.setCouponCategory(request.getCouponCategory());
            coupon.setDiscountType(request.getDiscountType());
            coupon.setDiscountValue(request.getDiscountValue());
            coupon.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO);
            coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
            coupon.setUsageLimit(request.getUsageLimit());
            coupon.setStartDate(request.getStartDate());
            coupon.setEndDate(request.getEndDate());
            if (request.getIsActive() != null) {
                coupon.setIsActive(request.getIsActive());
            }
        }

        return couponRepository.save(coupon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleAdminCouponStatus(Integer couponId) {
        Coupon coupon = getAdminCouponById(couponId);
        boolean newStatus = !Boolean.TRUE.equals(coupon.getIsActive());
        coupon.setIsActive(newStatus);
        couponRepository.save(coupon);
        return newStatus;
    }

    @Override
    @Transactional(readOnly = true)
    public CouponUsageResponse getAdminCouponUsage(Integer couponId, int page, int size) {
        Coupon coupon = getAdminCouponById(couponId);

        if (page < 1) page = 1;
        if (size < 1) size = 10;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "orderId"));
        Page<Order> orders = orderRepository.findByCoupon_CouponId(couponId, pageable);
        BigDecimal totalDiscount = orderRepository.sumTotalDiscountByCouponId(couponId);

        CouponResponse couponResponse = CouponResponse.fromEntity(coupon);
        long used = (coupon.getUsedCount() != null) ? coupon.getUsedCount() : 0;
        long limit = (coupon.getUsageLimit() != null) ? coupon.getUsageLimit() : 0;
        long remaining = Math.max(0, limit - used);

        return CouponUsageResponse.builder()
                .coupon(couponResponse)
                .totalUsed(used)
                .usageLimit(limit)
                .remainingLimit(remaining)
                .usagePercentage(couponResponse.getUsagePercent())
                .totalDiscountSponsored(totalDiscount)
                .orders(orders)
                .build();
    }

    // =========================================================================
    // HELPER & VALIDATION METHODS
    // =========================================================================

    private Shop getShopForUser(Integer userId) {
        return shopRepository.findByOwner_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản của bạn chưa đăng ký cửa hàng hoặc cửa hàng không tồn tại."));
    }

    private void validateCouponRequest(CouponRequest request, Integer currentCouponId) {
        String cleanCode = request.getCouponCode() != null ? request.getCouponCode().trim().toUpperCase() : "";
        if (cleanCode.isEmpty()) {
            throw new IllegalArgumentException("Mã giảm giá không được để trống.");
        }

        // Uniqueness check
        if (currentCouponId == null) {
            if (couponRepository.existsByCouponCodeIgnoreCase(cleanCode)) {
                throw new IllegalArgumentException("Mã giảm giá '" + cleanCode + "' đã tồn tại trên hệ thống. Vui lòng đặt mã khác.");
            }
        } else {
            if (couponRepository.existsByCouponCodeIgnoreCaseAndCouponIdNot(cleanCode, currentCouponId)) {
                throw new IllegalArgumentException("Mã giảm giá '" + cleanCode + "' đã tồn tại trên hệ thống. Vui lòng đặt mã khác.");
            }
        }

        // Discount value validation
        if (request.getDiscountType() == DiscountType.PERCENT) {
            if (request.getDiscountValue().compareTo(BigDecimal.ONE) < 0 || request.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Mức giảm theo phần trăm phải từ 1% đến 100%.");
            }
            if (request.getMaxDiscountAmount() == null || request.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Với hình thức giảm theo %, vui lòng nhập mức giảm tối đa (VNĐ) lớn hơn 0.");
            }
        } else if (request.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            if (request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Mức giảm số tiền cố định phải lớn hơn 0 VNĐ.");
            }
            if (request.getMinOrderValue() != null && request.getMinOrderValue().compareTo(BigDecimal.ZERO) > 0) {
                if (request.getDiscountValue().compareTo(request.getMinOrderValue()) > 0) {
                    throw new IllegalArgumentException("Mức giảm tiền (" + request.getDiscountValue()
                            + "đ) không được lớn hơn giá trị đơn hàng tối thiểu (" + request.getMinOrderValue() + "đ).");
                }
            }
        }

        // Date validation
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thời gian bắt đầu và kết thúc.");
        }
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc.");
        }
        if (currentCouponId == null && request.getEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian kết thúc không được ở trong quá khứ.");
        }

        // Usage limit
        if (request.getUsageLimit() == null || request.getUsageLimit() < 1) {

            throw new IllegalArgumentException("Số lượt sử dụng tối đa phải từ 1 lượt trở lên.");
        }
    }

    // =========================================================================
    // VOUCHER ANALYTICS DASHBOARD IMPLEMENTATIONS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public CouponDashboardResponse getAdminVoucherDashboardData() {
        // 1. Thống kê số liệu thô cho thẻ Summary
        BigDecimal rawPlatformDiscount = orderRepository.sumPlatformSponsoredDiscount();
        BigDecimal rawShopDiscount = orderRepository.sumShopSponsoredDiscount();
        BigDecimal rawRevenue = orderRepository.sumTotalVoucherRevenue();
        long voucherOrders = orderRepository.countVoucherOrders();
        long totalOrders = orderRepository.countTotalValidOrders();
        BigDecimal rawCostPrice = orderItemRepository.sumTotalCostPriceForVoucherOrders();

        BigDecimal platformDiscount = (rawPlatformDiscount != null) ? rawPlatformDiscount : BigDecimal.ZERO;
        BigDecimal shopDiscount = (rawShopDiscount != null) ? rawShopDiscount : BigDecimal.ZERO;
        BigDecimal totalDiscount = platformDiscount.add(shopDiscount);
        BigDecimal totalRevenue = (rawRevenue != null) ? rawRevenue : BigDecimal.ZERO;
        BigDecimal totalCost = (rawCostPrice != null) ? rawCostPrice : BigDecimal.ZERO;

        BigDecimal roi = BigDecimal.ZERO;
        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
            roi = totalRevenue.divide(totalDiscount, 2, RoundingMode.HALF_UP);
        }

        BigDecimal estimatedGrossProfit = totalRevenue.subtract(totalCost).subtract(totalDiscount);
        if (estimatedGrossProfit.signum() < 0 && totalRevenue.signum() == 0) {
            estimatedGrossProfit = BigDecimal.ZERO;
        }

        BigDecimal voucherOrderRate = BigDecimal.ZERO;
        if (totalOrders > 0) {
            voucherOrderRate = BigDecimal.valueOf((double) voucherOrders / totalOrders * 100)
                    .setScale(1, RoundingMode.HALF_UP);
        }

        BigDecimal averageOrderValue = BigDecimal.ZERO;
        BigDecimal averageCustomerSavings = BigDecimal.ZERO;
        if (voucherOrders > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(voucherOrders), 0, RoundingMode.HALF_UP);
            averageCustomerSavings = totalDiscount.divide(BigDecimal.valueOf(voucherOrders), 0, RoundingMode.HALF_UP);
        }

        CouponSummaryResponse summary = CouponSummaryResponse.builder()
                .totalDiscountAmount(totalDiscount)
                .platformDiscountAmount(platformDiscount)
                .shopDiscountAmount(shopDiscount)
                .totalVoucherRevenue(totalRevenue)
                .returnOnInvestment(roi)
                .estimatedGrossProfit(estimatedGrossProfit)
                .voucherOrderCount(voucherOrders)
                .totalOrderCount(totalOrders)
                .voucherOrderRate(voucherOrderRate)
                .averageOrderValue(averageOrderValue)
                .averageCustomerSavings(averageCustomerSavings)
                .build();

        // 2. Lấy danh sách toàn bộ đơn có dùng voucher
        List<Order> orders = orderRepository.findAllVoucherOrders();
        List<Coupon> allCoupons = couponRepository.findAll();

        // 3. Xây dựng Bảng xếp hạng hiệu quả từng mã (Leaderboard)
        List<CouponLeaderboardRowResponse> leaderboard = buildLeaderboard(allCoupons, orders);

        // 4. Xây dựng Biểu đồ đường xu hướng theo ngày (Daily Trend)
        List<CouponDailyTrendResponse> dailyTrends = buildDailyTrends(orders);

        // Chuẩn bị chuỗi JSON để Chart.js vẽ trực tiếp
        String trendLabelsJson = dailyTrends.stream()
                .map(d -> "\"" + d.getFormattedDate() + "\"")
                .collect(Collectors.joining(", ", "[", "]"));
        String trendRevenueJson = dailyTrends.stream()
                .map(d -> d.getRevenueTotal().toPlainString())
                .collect(Collectors.joining(", ", "[", "]"));
        String trendDiscountJson = dailyTrends.stream()
                .map(d -> d.getDiscountTotal().toPlainString())
                .collect(Collectors.joining(", ", "[", "]"));

        // 5. Phân bổ danh mục (Freeship vs Giảm giá quả)
        CouponCategoryDistributionResponse categoryDistribution = buildCategoryDistribution(orders);

        // 6. Phân bổ phương thức thanh toán (COD vs VNPAY)
        CouponPaymentMethodResponse paymentMethodDistribution = buildPaymentMethodDistribution(orders);

        // 7. Top 5 hoa quả bán chạy nhất nhờ voucher
        List<Object[]> rawTopFruits = orderItemRepository.findTopFruitProductsForVoucherOrders();
        List<CouponTopFruitProductResponse> topFruitProducts = buildTopFruitProducts(rawTopFruits);

        // 8. Báo cáo voucher đặc quyền theo Hạng Hội Viên (TierCoupon)
        List<CouponTierPerformanceResponse> tierPerformances = buildTierPerformance(orders);

        return CouponDashboardResponse.builder()
                .summary(summary)
                .leaderboard(leaderboard)
                .dailyTrends(dailyTrends)
                .categoryDistribution(categoryDistribution)
                .paymentMethodDistribution(paymentMethodDistribution)
                .topFruitProducts(topFruitProducts)
                .tierPerformances(tierPerformances)
                .trendLabelsJson(trendLabelsJson)
                .trendRevenueJson(trendRevenueJson)
                .trendDiscountJson(trendDiscountJson)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDashboardResponse getShopVoucherDashboardData(Integer userId) {
        Shop shop = getShopForUser(userId);
        Integer shopId = shop.getShopId();

        // 1. Thống kê số liệu thô của Shop
        BigDecimal rawShopDiscount = orderRepository.sumShopSponsoredDiscountForShop(shopId);
        BigDecimal rawPlatformDiscount = orderRepository.sumPlatformSponsoredDiscountForShop(shopId);
        BigDecimal rawRevenue = orderRepository.sumTotalVoucherRevenueForShop(shopId);
        long voucherOrders = orderRepository.countVoucherOrdersForShop(shopId);
        long totalOrders = orderRepository.countTotalValidOrdersForShop(shopId);
        BigDecimal rawCostPrice = orderItemRepository.sumTotalCostPriceForShopVoucherOrders(shopId);

        BigDecimal shopDiscount = (rawShopDiscount != null) ? rawShopDiscount : BigDecimal.ZERO;
        BigDecimal platformDiscount = (rawPlatformDiscount != null) ? rawPlatformDiscount : BigDecimal.ZERO;
        BigDecimal totalDiscount = shopDiscount.add(platformDiscount);
        BigDecimal totalRevenue = (rawRevenue != null) ? rawRevenue : BigDecimal.ZERO;
        BigDecimal totalCost = (rawCostPrice != null) ? rawCostPrice : BigDecimal.ZERO;

        BigDecimal roi = BigDecimal.ZERO;
        if (shopDiscount.compareTo(BigDecimal.ZERO) > 0) {
            roi = totalRevenue.divide(shopDiscount, 2, RoundingMode.HALF_UP);
        }

        BigDecimal estimatedGrossProfit = totalRevenue.subtract(totalCost).subtract(shopDiscount);
        if (estimatedGrossProfit.signum() < 0 && totalRevenue.signum() == 0) {
            estimatedGrossProfit = BigDecimal.ZERO;
        }

        BigDecimal voucherOrderRate = BigDecimal.ZERO;
        if (totalOrders > 0) {
            voucherOrderRate = BigDecimal.valueOf((double) voucherOrders / totalOrders * 100)
                    .setScale(1, RoundingMode.HALF_UP);
        }

        BigDecimal averageOrderValue = BigDecimal.ZERO;
        BigDecimal averageCustomerSavings = BigDecimal.ZERO;
        if (voucherOrders > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(voucherOrders), 0, RoundingMode.HALF_UP);
            averageCustomerSavings = totalDiscount.divide(BigDecimal.valueOf(voucherOrders), 0, RoundingMode.HALF_UP);
        }

        CouponSummaryResponse summary = CouponSummaryResponse.builder()
                .totalDiscountAmount(totalDiscount)
                .platformDiscountAmount(platformDiscount)
                .shopDiscountAmount(shopDiscount)
                .totalVoucherRevenue(totalRevenue)
                .returnOnInvestment(roi)
                .estimatedGrossProfit(estimatedGrossProfit)
                .voucherOrderCount(voucherOrders)
                .totalOrderCount(totalOrders)
                .voucherOrderRate(voucherOrderRate)
                .averageOrderValue(averageOrderValue)
                .averageCustomerSavings(averageCustomerSavings)
                .build();

        // 2. Lấy danh sách đơn của riêng Shop
        List<Order> orders = orderRepository.findAllVoucherOrdersByShopId(shopId);
        List<Coupon> shopCoupons = couponRepository.findByShop_ShopId(shopId, Pageable.unpaged()).getContent();

        // 3. Leaderboard riêng của shop
        List<CouponLeaderboardRowResponse> leaderboard = buildLeaderboard(shopCoupons, orders);

        // 4. Daily Trends riêng của shop
        List<CouponDailyTrendResponse> dailyTrends = buildDailyTrends(orders);

        String trendLabelsJson = dailyTrends.stream()
                .map(d -> "\"" + d.getFormattedDate() + "\"")
                .collect(Collectors.joining(", ", "[", "]"));
        String trendRevenueJson = dailyTrends.stream()
                .map(d -> d.getRevenueTotal().toPlainString())
                .collect(Collectors.joining(", ", "[", "]"));
        String trendDiscountJson = dailyTrends.stream()
                .map(d -> d.getDiscountTotal().toPlainString())
                .collect(Collectors.joining(", ", "[", "]"));

        // 5. Phân bổ danh mục
        CouponCategoryDistributionResponse categoryDistribution = buildCategoryDistribution(orders);

        // 6. Phân bổ phương thức thanh toán
        CouponPaymentMethodResponse paymentMethodDistribution = buildPaymentMethodDistribution(orders);

        // 7. Top 5 hoa quả bán chạy của shop
        List<Object[]> rawTopFruits = orderItemRepository.findTopFruitProductsForShopVoucherOrders(shopId);
        List<CouponTopFruitProductResponse> topFruitProducts = buildTopFruitProducts(rawTopFruits);

        return CouponDashboardResponse.builder()
                .summary(summary)
                .leaderboard(leaderboard)
                .dailyTrends(dailyTrends)
                .categoryDistribution(categoryDistribution)
                .paymentMethodDistribution(paymentMethodDistribution)
                .topFruitProducts(topFruitProducts)
                .tierPerformances(Collections.emptyList()) // Shop không quản lý tier toàn sàn
                .trendLabelsJson(trendLabelsJson)
                .trendRevenueJson(trendRevenueJson)
                .trendDiscountJson(trendDiscountJson)
                .build();
    }

    // =========================================================================
    // PRIVATE HELPER METHODS FOR DASHBOARD COMPUTATIONS
    // =========================================================================

    private List<CouponLeaderboardRowResponse> buildLeaderboard(List<Coupon> coupons, List<Order> orders) {
        if (coupons == null || coupons.isEmpty()) return Collections.emptyList();

        Map<Integer, List<Order>> ordersByCouponId = orders.stream()
                .filter(o -> o.getCoupon() != null)
                .collect(Collectors.groupingBy(o -> o.getCoupon().getCouponId()));

        List<CouponLeaderboardRowResponse> rows = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Coupon c : coupons) {
            List<Order> cOrders = ordersByCouponId.getOrDefault(c.getCouponId(), Collections.emptyList());

            BigDecimal discountTotal = cOrders.stream()
                    .map(Order::getDiscountAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal revenueTotal = cOrders.stream()
                    .map(Order::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal roi = BigDecimal.ZERO;
            if (discountTotal.compareTo(BigDecimal.ZERO) > 0) {
                roi = revenueTotal.divide(discountTotal, 2, RoundingMode.HALF_UP);
            }

            int used = (c.getUsedCount() != null) ? c.getUsedCount() : 0;
            int limit = (c.getUsageLimit() != null) ? c.getUsageLimit() : 0;
            int percent = (limit > 0) ? Math.min(100, (int) Math.round(((double) used / limit) * 100)) : 0;

            String sponsor = (c.getShop() == null) ? "Sàn BetaFruit" : c.getShop().getShopName();
            String category = (c.getCouponCategory() == CouponCategory.SHIPPING) ? "Vận chuyển (Freeship)" : "Sản phẩm";

            boolean expired = c.getEndDate() != null && now.isAfter(c.getEndDate());
            boolean outOfStock = limit > 0 && used >= limit;

            String status = "Đang chạy";
            String badge = "bg-success";
            if (!Boolean.TRUE.equals(c.getIsActive())) {
                status = "Tạm dừng";
                badge = "bg-secondary";
            } else if (expired) {
                status = "Hết hạn";
                badge = "bg-dark";
            } else if (outOfStock) {
                status = "Hết lượt";
                badge = "bg-danger";
            }

            rows.add(CouponLeaderboardRowResponse.builder()
                    .couponId(c.getCouponId())
                    .couponCode(c.getCouponCode())
                    .sponsorType(sponsor)
                    .categoryName(category)
                    .usedCount(used)
                    .usageLimit(limit)
                    .usagePercentage(percent)
                    .discountTotal(discountTotal)
                    .revenueTotal(revenueTotal)
                    .returnOnInvestment(roi)
                    .performanceStatus(status)
                    .statusBadgeClass(badge)
                    .build());
        }

        // Sắp xếp: Ưu tiên mã có doanh thu cao nhất lên đầu
        rows.sort((a, b) -> b.getRevenueTotal().compareTo(a.getRevenueTotal()));
        return rows;
    }

    private List<CouponDailyTrendResponse> buildDailyTrends(List<Order> orders) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");
        Map<LocalDate, List<Order>> ordersByDate = new HashMap<>();

        // Khởi tạo sẵn 14 ngày gần nhất để biểu đồ không bị đứt đoạn
        List<LocalDate> dateList = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dateList.add(d);
            ordersByDate.put(d, new ArrayList<>());
        }

        // Nhóm các đơn theo ngày
        for (Order o : orders) {
            ordersByDate.computeIfAbsent(today, k -> new ArrayList<>()).add(o);
        }

        List<CouponDailyTrendResponse> result = new ArrayList<>();
        for (LocalDate d : dateList) {
            List<Order> dayOrders = ordersByDate.getOrDefault(d, Collections.emptyList());
            BigDecimal rev = dayOrders.stream().map(Order::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal disc = dayOrders.stream().map(Order::getDiscountAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

            result.add(CouponDailyTrendResponse.builder()
                    .orderDate(d)
                    .formattedDate(d.format(dtf))
                    .revenueTotal(rev)
                    .discountTotal(disc)
                    .orderCount(dayOrders.size())
                    .build());
        }
        return result;
    }

    private CouponCategoryDistributionResponse buildCategoryDistribution(List<Order> orders) {
        BigDecimal shippingDisc = BigDecimal.ZERO;
        BigDecimal productDisc = BigDecimal.ZERO;
        long shippingCount = 0;
        long productCount = 0;

        for (Order o : orders) {
            if (o.getCoupon() != null) {
                BigDecimal disc = (o.getDiscountAmount() != null) ? o.getDiscountAmount() : BigDecimal.ZERO;
                if (o.getCoupon().getCouponCategory() == CouponCategory.SHIPPING) {
                    shippingDisc = shippingDisc.add(disc);
                    shippingCount++;
                } else {
                    productDisc = productDisc.add(disc);
                    productCount++;
                }
            }
        }

        return CouponCategoryDistributionResponse.builder()
                .shippingDiscountTotal(shippingDisc)
                .productDiscountTotal(productDisc)
                .shippingOrderCount(shippingCount)
                .productOrderCount(productCount)
                .build();
    }

    private CouponPaymentMethodResponse buildPaymentMethodDistribution(List<Order> orders) {
        long codCount = 0;
        long onlineCount = 0;
        BigDecimal codRev = BigDecimal.ZERO;
        BigDecimal onlineRev = BigDecimal.ZERO;

        for (Order o : orders) {
            BigDecimal rev = (o.getTotalAmount() != null) ? o.getTotalAmount() : BigDecimal.ZERO;
            if (o.getPaymentMethod() == PaymentMethod.COD) {
                codCount++;
                codRev = codRev.add(rev);
            } else {
                onlineCount++;
                onlineRev = onlineRev.add(rev);
            }
        }

        return CouponPaymentMethodResponse.builder()
                .codOrderCount(codCount)
                .onlineOrderCount(onlineCount)
                .codRevenueTotal(codRev)
                .onlineRevenueTotal(onlineRev)
                .build();
    }

    private List<CouponTopFruitProductResponse> buildTopFruitProducts(List<Object[]> rawList) {
        if (rawList == null || rawList.isEmpty()) return Collections.emptyList();

        List<CouponTopFruitProductResponse> list = new ArrayList<>();
        int count = 0;
        for (Object[] row : rawList) {
            if (count >= 5) break; // Chỉ lấy top 5 quả
            String pName = (row[0] != null) ? row[0].toString() : "Trái cây";
            long qty = (row[1] instanceof Number) ? ((Number) row[1]).longValue() : 0L;
            BigDecimal rev = (row[2] instanceof BigDecimal) ? (BigDecimal) row[2]
                    : (row[2] instanceof Number) ? BigDecimal.valueOf(((Number) row[2]).doubleValue()) : BigDecimal.ZERO;
            long oCount = (row[3] instanceof Number) ? ((Number) row[3]).longValue() : 0L;

            list.add(CouponTopFruitProductResponse.builder()
                    .productName(pName)
                    .totalQuantitySold(qty)
                    .totalRevenue(rev)
                    .orderCount(oCount)
                    .build());
            count++;
        }
        return list;
    }

    private List<CouponTierPerformanceResponse> buildTierPerformance(List<Order> orders) {
        List<MembershipTier> tiers = membershipTierRepository.findAllByOrderByMinSpentRequiredAsc();
        if (tiers == null || tiers.isEmpty()) return Collections.emptyList();

        List<CouponTierPerformanceResponse> result = new ArrayList<>();

        for (MembershipTier tier : tiers) {
            long members = customerMembershipRepository.countByTier_TierId(tier.getTierId());
            List<TierCoupon> tierCoupons = tierCouponRepository.findByTier_TierId(tier.getTierId());

            Set<Integer> couponIds = (tierCoupons != null)
                    ? tierCoupons.stream().map(tc -> tc.getCoupon().getCouponId()).collect(Collectors.toSet())
                    : Collections.emptySet();

            long usedCount = 0;
            BigDecimal revenue = BigDecimal.ZERO;

            if (!couponIds.isEmpty()) {
                for (Order o : orders) {
                    if (o.getCoupon() != null && couponIds.contains(o.getCoupon().getCouponId())) {
                        usedCount++;
                        if (o.getTotalAmount() != null) {
                            revenue = revenue.add(o.getTotalAmount());
                        }
                    }
                }
            }

            String badge = "bg-secondary";
            String tName = tier.getTierName() != null ? tier.getTierName().toLowerCase() : "";
            if (tName.contains("bạc") || tName.contains("silver")) badge = "bg-secondary";
            else if (tName.contains("vàng") || tName.contains("gold")) badge = "bg-warning text-dark";
            else if (tName.contains("kim cương") || tName.contains("diamond")) badge = "bg-info text-dark";
            else if (tName.contains("đồng") || tName.contains("bronze")) badge = "bg-dark";

            result.add(CouponTierPerformanceResponse.builder()
                    .tierId(tier.getTierId())
                    .tierName(tier.getTierName())
                    .memberCount(members)
                    .usedVoucherCount(usedCount)
                    .revenueTotal(revenue)
                    .tierBadgeClass(badge)
                    .build());
        }
        return result;
    }
}
