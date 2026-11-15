package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.CouponDashboardFilterRequest;
import com.fu.SWP391_BetaFruit.dto.request.CouponRequest;
import java.nio.charset.StandardCharsets;
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
import com.fu.SWP391_BetaFruit.service.CouponNotificationService;
import com.fu.SWP391_BetaFruit.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;
    private final CouponNotificationService couponNotificationService;
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

        Coupon saved = couponRepository.save(coupon);
        try {
            couponNotificationService.notifyCustomersNewCoupon(saved);
        } catch (Exception ex) {
            log.warn("Không thể gửi thông báo coupon sàn mới: {}", ex.getMessage());
        }
        return saved;
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

        Coupon saved = couponRepository.save(coupon);
        try {
            couponNotificationService.notifyCustomersNewCoupon(saved);
        } catch (Exception ex) {
            log.warn("Không thể gửi thông báo coupon sàn mới: {}", ex.getMessage());
        }
        return saved;
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
        return getAdminVoucherDashboardData(new CouponDashboardFilterRequest());
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDashboardResponse getAdminVoucherDashboardData(CouponDashboardFilterRequest filter) {
        if (filter == null) {
            filter = new CouponDashboardFilterRequest();
        }
        filter.resolveEffectiveDates();

        List<Order> allOrders = orderRepository.findAllVoucherOrders();
        List<Order> orders = filterOrdersByDateAndCategory(allOrders, filter);

        BigDecimal platformDiscount = BigDecimal.ZERO;
        BigDecimal shopDiscount = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long voucherOrders = orders.size();
        long totalOrders = orderRepository.countTotalValidOrders();
        BigDecimal rawCostPrice = orderItemRepository.sumTotalCostPriceForVoucherOrders();
        BigDecimal totalCost = (rawCostPrice != null) ? rawCostPrice : BigDecimal.ZERO;

        boolean isDefaultFilter = "14DAYS".equalsIgnoreCase(filter.getRangePreset()) && filter.getCategory() == null;
        if (isDefaultFilter) {
            BigDecimal rawPlatform = orderRepository.sumPlatformSponsoredDiscount();
            BigDecimal rawShop = orderRepository.sumShopSponsoredDiscount();
            BigDecimal rawRev = orderRepository.sumTotalVoucherRevenue();
            Long vOrders = orderRepository.countVoucherOrders();

            platformDiscount = (rawPlatform != null) ? rawPlatform : BigDecimal.ZERO;
            shopDiscount = (rawShop != null) ? rawShop : BigDecimal.ZERO;
            totalRevenue = (rawRev != null) ? rawRev : BigDecimal.ZERO;
            if (vOrders != null && vOrders > 0) voucherOrders = vOrders;
        } else {
            for (Order o : orders) {
                if (o.getDiscountAmount() != null) {
                    if (o.getCoupon() != null && o.getCoupon().getShop() != null) {
                        shopDiscount = shopDiscount.add(o.getDiscountAmount());
                    } else {
                        platformDiscount = platformDiscount.add(o.getDiscountAmount());
                    }
                }
                if (o.getTotalAmount() != null) {
                    totalRevenue = totalRevenue.add(o.getTotalAmount());
                }
            }
        }

        BigDecimal totalDiscount = platformDiscount.add(shopDiscount);

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

        List<Coupon> allCoupons = couponRepository.findAll();
        List<CouponLeaderboardRowResponse> leaderboard = buildLeaderboard(allCoupons, orders);
        List<CouponDailyTrendResponse> dailyTrends = buildDailyTrends(orders, filter.getStartDate(), filter.getEndDate());

        String trendLabelsJson = dailyTrends.stream()
                .map(d -> "\"" + d.getFormattedDate() + "\"")
                .collect(Collectors.joining(", ", "[", "]"));
        String trendRevenueJson = dailyTrends.stream()
                .map(d -> d.getRevenueTotal().toPlainString())
                .collect(Collectors.joining(", ", "[", "]"));
        String trendDiscountJson = dailyTrends.stream()
                .map(d -> d.getDiscountTotal().toPlainString())
                .collect(Collectors.joining(", ", "[", "]"));

        CouponCategoryDistributionResponse categoryDistribution = buildCategoryDistribution(orders);
        CouponPaymentMethodResponse paymentMethodDistribution = buildPaymentMethodDistribution(orders);

        List<Object[]> rawTopFruits = orderItemRepository.findTopFruitProductsForVoucherOrders();
        List<CouponTopFruitProductResponse> topFruitProducts = buildTopFruitProducts(rawTopFruits);

        List<CouponTierPerformanceResponse> tierPerformances = buildTierPerformance(orders);
        List<String> anomalyAlerts = buildAnomalyAlerts(leaderboard);

        return CouponDashboardResponse.builder()
                .summary(summary)
                .leaderboard(leaderboard)
                .dailyTrends(dailyTrends)
                .categoryDistribution(categoryDistribution)
                .paymentMethodDistribution(paymentMethodDistribution)
                .topFruitProducts(topFruitProducts)
                .tierPerformances(tierPerformances)
                .filter(filter)
                .anomalyAlerts(anomalyAlerts)
                .trendLabelsJson(trendLabelsJson)
                .trendRevenueJson(trendRevenueJson)
                .trendDiscountJson(trendDiscountJson)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDashboardResponse getShopVoucherDashboardData(Integer userId) {
        return getShopVoucherDashboardData(userId, new CouponDashboardFilterRequest());
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDashboardResponse getShopVoucherDashboardData(Integer userId, CouponDashboardFilterRequest filter) {
        if (filter == null) {
            filter = new CouponDashboardFilterRequest();
        }
        filter.resolveEffectiveDates();

        Shop shop = getShopForUser(userId);
        Integer shopId = shop.getShopId();

        List<Order> allOrders = orderRepository.findAllVoucherOrdersByShopId(shopId);
        List<Order> orders = filterOrdersByDateAndCategory(allOrders, filter);

        BigDecimal rawCostPrice = orderItemRepository.sumTotalCostPriceForShopVoucherOrders(shopId);
        BigDecimal totalCost = (rawCostPrice != null) ? rawCostPrice : BigDecimal.ZERO;
        long totalOrders = orderRepository.countTotalValidOrdersForShop(shopId);

        BigDecimal shopDiscount = BigDecimal.ZERO;
        BigDecimal platformDiscount = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long voucherOrders = orders.size();

        boolean isDefaultFilter = "14DAYS".equalsIgnoreCase(filter.getRangePreset()) && filter.getCategory() == null;
        if (isDefaultFilter) {
            BigDecimal rawShop = orderRepository.sumShopSponsoredDiscountForShop(shopId);
            BigDecimal rawPlatform = orderRepository.sumPlatformSponsoredDiscountForShop(shopId);
            BigDecimal rawRev = orderRepository.sumTotalVoucherRevenueForShop(shopId);
            Long vOrders = orderRepository.countVoucherOrdersForShop(shopId);

            shopDiscount = (rawShop != null) ? rawShop : BigDecimal.ZERO;
            platformDiscount = (rawPlatform != null) ? rawPlatform : BigDecimal.ZERO;
            totalRevenue = (rawRev != null) ? rawRev : BigDecimal.ZERO;
            if (vOrders != null && vOrders > 0) voucherOrders = vOrders;
        } else {
            for (Order o : orders) {
                if (o.getDiscountAmount() != null) {
                    if (o.getCoupon() != null && o.getCoupon().getShop() != null) {
                        shopDiscount = shopDiscount.add(o.getDiscountAmount());
                    } else {
                        platformDiscount = platformDiscount.add(o.getDiscountAmount());
                    }
                }
                if (o.getTotalAmount() != null) {
                    totalRevenue = totalRevenue.add(o.getTotalAmount());
                }
            }
        }

        BigDecimal totalDiscount = shopDiscount.add(platformDiscount);

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

        List<Coupon> shopCoupons = couponRepository.findByShop_ShopId(shopId, Pageable.unpaged()).getContent();
        List<CouponLeaderboardRowResponse> leaderboard = buildLeaderboard(shopCoupons, orders);
        List<CouponDailyTrendResponse> dailyTrends = buildDailyTrends(orders, filter.getStartDate(), filter.getEndDate());

        String trendLabelsJson = dailyTrends.stream()
                .map(d -> "\"" + d.getFormattedDate() + "\"")
                .collect(Collectors.joining(", ", "[", "]"));
        String trendRevenueJson = dailyTrends.stream()
                .map(d -> d.getRevenueTotal().toPlainString())
                .collect(Collectors.joining(", ", "[", "]"));
        String trendDiscountJson = dailyTrends.stream()
                .map(d -> d.getDiscountTotal().toPlainString())
                .collect(Collectors.joining(", ", "[", "]"));

        CouponCategoryDistributionResponse categoryDistribution = buildCategoryDistribution(orders);
        CouponPaymentMethodResponse paymentMethodDistribution = buildPaymentMethodDistribution(orders);

        List<Object[]> rawTopFruits = orderItemRepository.findTopFruitProductsForShopVoucherOrders(shopId);
        List<CouponTopFruitProductResponse> topFruitProducts = buildTopFruitProducts(rawTopFruits);
        List<CouponTierPerformanceResponse> tierPerformances = buildTierPerformance(orders);
        List<String> anomalyAlerts = buildAnomalyAlerts(leaderboard);

        return CouponDashboardResponse.builder()
                .summary(summary)
                .leaderboard(leaderboard)
                .dailyTrends(dailyTrends)
                .categoryDistribution(categoryDistribution)
                .paymentMethodDistribution(paymentMethodDistribution)
                .topFruitProducts(topFruitProducts)
                .tierPerformances(tierPerformances)
                .filter(filter)
                .anomalyAlerts(anomalyAlerts)
                .trendLabelsJson(trendLabelsJson)
                .trendRevenueJson(trendRevenueJson)
                .trendDiscountJson(trendDiscountJson)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAdminVoucherReportCsv(CouponDashboardFilterRequest filter) {
        CouponDashboardResponse dashboard = getAdminVoucherDashboardData(filter);
        return generateVoucherCsvReport("TOÀN SÀN BETAFRUIT", dashboard);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportShopVoucherReportCsv(Integer userId, CouponDashboardFilterRequest filter) {
        Shop shop = getShopForUser(userId);
        CouponDashboardResponse dashboard = getShopVoucherDashboardData(userId, filter);
        return generateVoucherCsvReport("CỬA HÀNG " + shop.getShopName().toUpperCase(), dashboard);
    }

    // =========================================================================
    // PRIVATE HELPER METHODS FOR DASHBOARD COMPUTATIONS
    // =========================================================================

    private List<Order> filterOrdersByDateAndCategory(List<Order> orders, CouponDashboardFilterRequest filter) {
        if (orders == null || orders.isEmpty()) return Collections.emptyList();
        LocalDate start = filter.getStartDate();
        LocalDate end = filter.getEndDate();
        CouponCategory cat = filter.getCategory();

        return orders.stream()
                .filter(o -> {
                    if (start != null && end != null && o.getCreatedAt() != null) {
                        LocalDate d = o.getCreatedAt().toLocalDate();
                        if (d.isBefore(start) || d.isAfter(end)) return false;
                    }
                    if (cat != null && o.getCoupon() != null) {
                        if (o.getCoupon().getCouponCategory() != cat) return false;
                    }
                    return true;
                })
                .toList();
    }

    private List<String> buildAnomalyAlerts(List<CouponLeaderboardRowResponse> leaderboard) {
        if (leaderboard == null || leaderboard.isEmpty()) return Collections.emptyList();
        List<String> alerts = new ArrayList<>();
        for (CouponLeaderboardRowResponse row : leaderboard) {
            if (row.getUsagePercentage() != null && row.getUsagePercentage() >= 90) {
                alerts.add("Mã [" + row.getCouponCode() + "] đã sử dụng " + row.getUsagePercentage() + "% ngân sách lượt dùng ("
                        + row.getUsedCount() + "/" + (row.getUsageLimit() != null && row.getUsageLimit() > 0 ? row.getUsageLimit() : "∞")
                        + "). Cân nhắc gia hạn thêm hạn mức.");
            }
            if (row.getReturnOnInvestment() != null && row.getReturnOnInvestment().compareTo(new BigDecimal("1.50")) < 0
                    && row.getUsedCount() != null && row.getUsedCount() > 0) {
                alerts.add("Mã [" + row.getCouponCode() + "] có tỷ suất sinh lời thấp (ROI " + row.getReturnOnInvestment()
                        + "x < 1.5x). Đề xuất tối ưu mức giảm hoặc xem xét đối tượng khách hàng áp dụng.");
            }
        }
        return alerts;
    }

    private byte[] generateVoucherCsvReport(String scopeTitle, CouponDashboardResponse dashboard) {
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff'); // UTF-8 BOM for Microsoft Excel

        sb.append("BÁO CÁO THỐNG KÊ CHI PHÍ & HIỆU QUẢ VOUCHER - ").append(scopeTitle).append("\n");
        sb.append("Thời gian xuất: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
        if (dashboard.getFilter() != null) {
            sb.append("Khoảng thời gian: Từ ").append(dashboard.getFilter().getStartDate()).append(" đến ").append(dashboard.getFilter().getEndDate()).append("\n");
            if (dashboard.getFilter().getCategory() != null) {
                sb.append("Danh mục ưu đãi: ").append(dashboard.getFilter().getCategory().name()).append("\n");
            }
        }
        sb.append("\n");

        sb.append("=== 1. TỔNG HỢP CHỈ SỐ KINH DOANH ===\n");
        sb.append("Chỉ Số,Giá Trị\n");
        CouponSummaryResponse s = dashboard.getSummary();
        if (s != null) {
            sb.append("Doanh thu từ đơn có mã (đ),").append(s.getTotalVoucherRevenue() != null ? s.getTotalVoucherRevenue().toPlainString() : "0").append("\n");
            sb.append("Tổng tiền giảm giá trợ giá (đ),").append(s.getTotalDiscountAmount() != null ? s.getTotalDiscountAmount().toPlainString() : "0").append("\n");
            sb.append("  - Sàn BetaFruit tài trợ (đ),").append(s.getPlatformDiscountAmount() != null ? s.getPlatformDiscountAmount().toPlainString() : "0").append("\n");
            sb.append("  - Cửa hàng tự tài trợ (đ),").append(s.getShopDiscountAmount() != null ? s.getShopDiscountAmount().toPlainString() : "0").append("\n");
            sb.append("Hiệu suất sinh lời (ROI),").append(s.getReturnOnInvestment() != null ? s.getReturnOnInvestment().toPlainString() : "0").append(" lần\n");
            sb.append("Lợi nhuận gộp ước tính (đ),").append(s.getEstimatedGrossProfit() != null ? s.getEstimatedGrossProfit().toPlainString() : "0").append("\n");
            sb.append("Số đơn áp dụng mã,").append(s.getVoucherOrderCount()).append(" đơn\n");
            sb.append("Tổng đơn toàn sàn,").append(s.getTotalOrderCount()).append(" đơn\n");
            sb.append("Tỷ lệ đơn dùng mã,").append(s.getVoucherOrderRate() != null ? s.getVoucherOrderRate().toPlainString() : "0").append(" %\n");
            sb.append("Giá trị đơn trung bình AOV (đ),").append(s.getAverageOrderValue() != null ? s.getAverageOrderValue().toPlainString() : "0").append("\n");
            sb.append("Tiết kiệm trung bình mỗi đơn (đ),").append(s.getAverageCustomerSavings() != null ? s.getAverageCustomerSavings().toPlainString() : "0").append("\n");
        }
        sb.append("\n");

        sb.append("=== 2. BẢNG XẾP HẠNG HIỆU QUẢ TỪNG MÃ VOUCHER ===\n");
        sb.append("Hạng,Mã Voucher,Bên Tài Trợ,Loại Ưu Đãi,Lượt Dùng,Giới Hạn,Tiến Độ (%),Tiền Giảm (đ),Doanh Thu (đ),ROI (lần),Trạng Thái\n");
        if (dashboard.getLeaderboard() != null) {
            int rank = 1;
            for (CouponLeaderboardRowResponse r : dashboard.getLeaderboard()) {
                sb.append(rank++).append(",");
                sb.append(escapeCsv(r.getCouponCode())).append(",");
                sb.append(escapeCsv(r.getSponsorType())).append(",");
                sb.append(escapeCsv(r.getCategoryName())).append(",");
                sb.append(r.getUsedCount() != null ? r.getUsedCount() : 0).append(",");
                sb.append(r.getUsageLimit() != null && r.getUsageLimit() > 0 ? r.getUsageLimit() : "Không giới hạn").append(",");
                sb.append(r.getUsagePercentage() != null ? r.getUsagePercentage() : 0).append("%,");
                sb.append(r.getDiscountTotal() != null ? r.getDiscountTotal().toPlainString() : "0").append(",");
                sb.append(r.getRevenueTotal() != null ? r.getRevenueTotal().toPlainString() : "0").append(",");
                sb.append(r.getReturnOnInvestment() != null ? r.getReturnOnInvestment().toPlainString() : "0").append(",");
                sb.append(escapeCsv(r.getPerformanceStatus())).append("\n");
            }
        }
        sb.append("\n");

        sb.append("=== 3. SỐ LIỆU BIỂU ĐỒ: XU HƯỚNG DOANH THU & CHI PHÍ THEO NGÀY ===\n");
        sb.append("Ngày,Doanh Thu Từ Đơn Có Mã (đ),Tổng Chi Phí Giảm Giá (đ),Doanh Thu Ròng (đ)\n");
        if (dashboard.getDailyTrends() != null) {
            for (CouponDailyTrendResponse d : dashboard.getDailyTrends()) {
                BigDecimal rev = d.getRevenueTotal() != null ? d.getRevenueTotal() : BigDecimal.ZERO;
                BigDecimal disc = d.getDiscountTotal() != null ? d.getDiscountTotal() : BigDecimal.ZERO;
                BigDecimal net = rev.subtract(disc);
                String dateStr = d.getFormattedDate() != null ? d.getFormattedDate() : (d.getOrderDate() != null ? d.getOrderDate().toString() : "");
                sb.append(escapeCsv(dateStr)).append(",");
                sb.append(rev.toPlainString()).append(",");
                sb.append(disc.toPlainString()).append(",");
                sb.append(net.toPlainString()).append("\n");
            }
        }
        sb.append("\n");

        sb.append("=== 4. SỐ LIỆU BIỂU ĐỒ: CƠ CẤU KHUYẾN MÃI & THANH TOÁN ===\n");
        sb.append("Phân Loại,Danh Mục / Phương Thức,Giá Trị (đ)\n");
        if (dashboard.getCategoryDistribution() != null) {
            CouponCategoryDistributionResponse cd = dashboard.getCategoryDistribution();
            sb.append("Loại Khuyến Mãi,Giảm Giá Đơn Hàng,").append(cd.getProductDiscountTotal() != null ? cd.getProductDiscountTotal().toPlainString() : "0").append("\n");
            sb.append("Loại Khuyến Mãi,Freeship Vận Chuyển,").append(cd.getShippingDiscountTotal() != null ? cd.getShippingDiscountTotal().toPlainString() : "0").append("\n");
        }
        if (dashboard.getPaymentMethodDistribution() != null) {
            CouponPaymentMethodResponse pm = dashboard.getPaymentMethodDistribution();
            sb.append("Phương Thức Thanh Toán,Thanh Toán COD (Tiền mặt),").append(pm.getCodRevenueTotal() != null ? pm.getCodRevenueTotal().toPlainString() : "0").append("\n");
            sb.append("Phương Thức Thanh Toán,Thanh Toán VNPAY (Trực tuyến),").append(pm.getOnlineRevenueTotal() != null ? pm.getOnlineRevenueTotal().toPlainString() : "0").append("\n");
        }
        sb.append("\n");

        sb.append("=== 5. BẢNG TOP HOA QUẢ BÁN CHẠY NHỜ VOUCHER ===\n");
        sb.append("Hạng,Tên Sản Phẩm Hoa Quả,Số Lượng Đã Bán (kg/hộp),Số Đơn Hàng Áp Mã,Doanh Thu Thu Về (đ)\n");
        if (dashboard.getTopFruitProducts() != null) {
            int fruitRank = 1;
            for (CouponTopFruitProductResponse f : dashboard.getTopFruitProducts()) {
                sb.append(fruitRank++).append(",");
                sb.append(escapeCsv(f.getProductName())).append(",");
                sb.append(f.getTotalQuantitySold()).append(",");
                sb.append(f.getOrderCount()).append(",");
                sb.append(f.getTotalRevenue() != null ? f.getTotalRevenue().toPlainString() : "0").append("\n");
            }
        }
        sb.append("\n");

        sb.append("=== 6. BẢNG HIỆU QUẢ VOUCHER THEO HẠNG HỘI VIÊN THÂN THIẾT ===\n");
        sb.append("Hạng Hội Viên,Số Khách Hàng,Số Đơn Dùng Mã,Doanh Thu Mang Lại (đ)\n");
        if (dashboard.getTierPerformances() != null) {
            for (CouponTierPerformanceResponse t : dashboard.getTierPerformances()) {
                sb.append(escapeCsv(t.getTierName())).append(",");
                sb.append(t.getMemberCount()).append(",");
                sb.append(t.getUsedVoucherCount()).append(",");
                sb.append(t.getRevenueTotal() != null ? t.getRevenueTotal().toPlainString() : "0").append("\n");
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private List<CouponLeaderboardRowResponse> buildLeaderboard(List<Coupon> coupons, List<Order> orders) {
        if (coupons == null || coupons.isEmpty()) return Collections.emptyList();

        Map<Integer, List<Order>> ordersByCoupon = new HashMap<>();
        for (Order o : orders) {
            if (o.getCoupon() != null && o.getCoupon().getCouponId() != null) {
                ordersByCoupon.computeIfAbsent(o.getCoupon().getCouponId(), k -> new ArrayList<>()).add(o);
            }
        }

        List<CouponLeaderboardRowResponse> rows = new ArrayList<>();
        for (Coupon c : coupons) {
            List<Order> couponOrders = ordersByCoupon.getOrDefault(c.getCouponId(), Collections.emptyList());

            BigDecimal discountTotal = BigDecimal.ZERO;
            BigDecimal revenueTotal = BigDecimal.ZERO;

            for (Order o : couponOrders) {
                if (o.getDiscountAmount() != null) {
                    discountTotal = discountTotal.add(o.getDiscountAmount());
                }
                if (o.getTotalAmount() != null) {
                    revenueTotal = revenueTotal.add(o.getTotalAmount());
                }
            }

            BigDecimal roi = BigDecimal.ZERO;
            if (discountTotal.compareTo(BigDecimal.ZERO) > 0) {
                roi = revenueTotal.divide(discountTotal, 2, RoundingMode.HALF_UP);
            }

            int used = (c.getUsedCount() != null) ? c.getUsedCount() : couponOrders.size();
            int limit = (c.getUsageLimit() != null) ? c.getUsageLimit() : 0;
            int percent = (limit > 0) ? Math.min(100, (int) Math.round((double) used / limit * 100)) : 0;

            String sponsor = (c.getShop() == null) ? "Sàn tài trợ" : c.getShop().getShopName();
            String catName = (c.getCouponCategory() == CouponCategory.SHIPPING) ? "Vận chuyển" : "Sản phẩm";
            String status = Boolean.TRUE.equals(c.getIsActive()) ? "Đang chạy" : "Tạm dừng";
            String badge = Boolean.TRUE.equals(c.getIsActive()) ? "bg-success" : "bg-secondary";

            rows.add(CouponLeaderboardRowResponse.builder()
                    .couponId(c.getCouponId())
                    .couponCode(c.getCouponCode())
                    .sponsorType(sponsor)
                    .categoryName(catName)
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

        rows.sort((a, b) -> b.getRevenueTotal().compareTo(a.getRevenueTotal()));
        return rows;
    }

    private List<CouponDailyTrendResponse> buildDailyTrends(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            LocalDate today = LocalDate.now();
            startDate = today.minusDays(13);
            endDate = today;
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");
        Map<LocalDate, List<Order>> ordersByDate = new HashMap<>();

        List<LocalDate> dateList = new ArrayList<>();
        LocalDate curr = startDate;
        while (!curr.isAfter(endDate)) {
            dateList.add(curr);
            ordersByDate.put(curr, new ArrayList<>());
            curr = curr.plusDays(1);
        }

        for (Order o : orders) {
            if (o.getCreatedAt() != null) {
                LocalDate d = o.getCreatedAt().toLocalDate();
                if (ordersByDate.containsKey(d)) {
                    ordersByDate.get(d).add(o);
                }
            }
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
                } else if (o.getCoupon().getCouponCategory() == CouponCategory.PRODUCT) {
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
        BigDecimal codRev = BigDecimal.ZERO;
        BigDecimal onlineRev = BigDecimal.ZERO;
        long codCount = 0;
        long onlineCount = 0;

        for (Order o : orders) {
            BigDecimal amt = (o.getTotalAmount() != null) ? o.getTotalAmount() : BigDecimal.ZERO;
            if (o.getPaymentMethod() == PaymentMethod.COD) {
                codRev = codRev.add(amt);
                codCount++;
            } else if (o.getPaymentMethod() == PaymentMethod.VNPAY) {
                onlineRev = onlineRev.add(amt);
                onlineCount++;
            }
        }

        return CouponPaymentMethodResponse.builder()
                .codRevenueTotal(codRev)
                .onlineRevenueTotal(onlineRev)
                .codOrderCount(codCount)
                .onlineOrderCount(onlineCount)
                .build();
    }

    private List<CouponTopFruitProductResponse> buildTopFruitProducts(List<Object[]> rawList) {
        if (rawList == null || rawList.isEmpty()) return Collections.emptyList();

        List<CouponTopFruitProductResponse> list = new ArrayList<>();
        int count = 0;
        for (Object[] row : rawList) {
            if (count >= 5) break;
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
