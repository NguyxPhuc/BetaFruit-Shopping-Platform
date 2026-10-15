package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.CouponRequest;
import com.fu.SWP391_BetaFruit.dto.response.CouponResponse;
import com.fu.SWP391_BetaFruit.dto.response.CouponUsageResponse;
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
}
