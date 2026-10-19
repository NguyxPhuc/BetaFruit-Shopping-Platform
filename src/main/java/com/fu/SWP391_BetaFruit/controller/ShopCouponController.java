package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.CouponRequest;
import com.fu.SWP391_BetaFruit.dto.response.CouponUsageResponse;
import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import com.fu.SWP391_BetaFruit.service.CouponService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/shop/coupons")
@RequiredArgsConstructor
public class ShopCouponController {

    private final CouponService couponService;

    // =========================================================================
    // 1. DANH SÁCH MÃ GIẢM GIÁ CỦA CỬA HÀNG
    // URL: /shop/coupons
    // =========================================================================
    @GetMapping
    public String listShopCoupons(@RequestParam(value = "keyword", required = false) String keyword,
                                 @RequestParam(value = "status", defaultValue = "ALL") String status,
                                 @RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "8") int size,
                                 HttpSession session,
                                 Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            Map<String, Object> pageData = couponService.getShopCouponsPageData(userId, keyword, status, page, size);
            model.addAllAttributes(pageData);
            return "shop/coupons/list";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "shop/coupons/list";
        }
    }

    // =========================================================================
    // 2. GIAO DIỆN TẠO MÃ GIẢM GIÁ MỚI
    // URL: /shop/coupons/create
    // =========================================================================
    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        if (!model.containsAttribute("couponRequest")) {
            CouponRequest request = new CouponRequest();
            request.setCouponCategory(CouponCategory.PRODUCT);
            request.setDiscountType(DiscountType.PERCENT);
            request.setStartDate(LocalDateTime.now().withSecond(0).withNano(0));
            request.setEndDate(LocalDateTime.now().plusDays(7).withSecond(0).withNano(0));
            request.setUsageLimit(50);
            request.setIsActive(true);
            model.addAttribute("couponRequest", request);
        }

        model.addAttribute("categories", CouponCategory.values());
        model.addAttribute("discountTypes", DiscountType.values());
        return "shop/coupons/create";
    }

    // =========================================================================
    // 3. XỬ LÝ LƯU TẠO MỚI MÃ GIẢM GIÁ
    // URL: /shop/coupons/create
    // =========================================================================
    @PostMapping("/create")
    public String processCreate(@Valid @ModelAttribute("couponRequest") CouponRequest request,
                                BindingResult bindingResult,
                                HttpSession session,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            return "shop/coupons/create";
        }

        try {
            Coupon saved = couponService.createShopCoupon(userId, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Tạo mã giảm giá '" + saved.getCouponCode() + "' thành công!");
            return "redirect:/shop/coupons";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            return "shop/coupons/create";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi tạo mã giảm giá: " + ex.getMessage());
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            return "shop/coupons/create";
        }
    }

    // =========================================================================
    // 4. GIAO DIỆN CHỈNH SỬA MÃ GIẢM GIÁ
    // URL: /shop/coupons/{id}/edit
    // =========================================================================
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Integer id,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            Coupon originalCoupon = couponService.getShopCouponById(userId, id);
            if (!model.containsAttribute("couponRequest")) {
                CouponRequest request = couponService.getShopCouponRequestForEdit(userId, id);
                model.addAttribute("couponRequest", request);
            }
            model.addAttribute("coupon", originalCoupon);
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("isLocked", originalCoupon.getUsedCount() != null && originalCoupon.getUsedCount() > 0);
            return "shop/coupons/edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/shop/coupons";
        }
    }

    // =========================================================================
    // 5. XỬ LÝ LƯU CẬP NHẬT MÃ GIẢM GIÁ
    // URL: /shop/coupons/{id}/edit
    // =========================================================================
    @PostMapping("/{id}/edit")
    public String processEdit(@PathVariable("id") Integer id,
                              @Valid @ModelAttribute("couponRequest") CouponRequest request,
                              BindingResult bindingResult,
                              HttpSession session,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        if (bindingResult.hasErrors()) {
            Coupon originalCoupon = couponService.getShopCouponById(userId, id);
            model.addAttribute("coupon", originalCoupon);
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("isLocked", originalCoupon.getUsedCount() != null && originalCoupon.getUsedCount() > 0);
            return "shop/coupons/edit";
        }

        try {
            Coupon updated = couponService.updateShopCoupon(userId, id, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật mã giảm giá '" + updated.getCouponCode() + "' thành công!");
            return "redirect:/shop/coupons";
        } catch (IllegalArgumentException ex) {
            Coupon originalCoupon = couponService.getShopCouponById(userId, id);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("coupon", originalCoupon);
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("isLocked", originalCoupon.getUsedCount() != null && originalCoupon.getUsedCount() > 0);
            return "shop/coupons/edit";
        } catch (Exception ex) {
            Coupon originalCoupon = couponService.getShopCouponById(userId, id);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi cập nhật: " + ex.getMessage());
            model.addAttribute("coupon", originalCoupon);
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("isLocked", originalCoupon.getUsedCount() != null && originalCoupon.getUsedCount() > 0);
            return "shop/coupons/edit";
        }
    }

    // =========================================================================
    // 6. BẬT / TẮT KÍCH HOẠT MÃ GIẢM GIÁ (TOGGLE ACTIVE)
    // URL: /shop/coupons/{id}/toggle-status
    // =========================================================================
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Integer id,
                               @RequestParam(value = "status", required = false) String status,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            boolean active = couponService.toggleShopCouponStatus(userId, id);
            redirectAttributes.addFlashAttribute("successMessage",
                    active ? "Đã kích hoạt lại mã giảm giá thành công!" : "Đã tạm dừng áp dụng mã giảm giá thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        StringBuilder redirectUrl = new StringBuilder("redirect:/shop/coupons?page=").append(page);
        if (status != null && !status.trim().isEmpty()) {
            redirectUrl.append("&status=").append(status.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            redirectUrl.append("&keyword=").append(keyword.trim());
        }
        return redirectUrl.toString();
    }

    // =========================================================================
    // 7. THEO DÕI SỐ LƯỢT DÙNG & LỊCH SỬ ĐƠN HÀNG DÙNG MÃ
    // URL: /shop/coupons/{id}/usage
    // =========================================================================
    @GetMapping("/{id}/usage")
    public String viewUsage(@PathVariable("id") Integer id,
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            CouponUsageResponse usageData = couponService.getShopCouponUsage(userId, id, page, size);
            model.addAttribute("usageData", usageData);
            model.addAttribute("coupon", usageData.getCoupon());
            model.addAttribute("orders", usageData.getOrders().getContent());
            model.addAttribute("orderPage", usageData.getOrders());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", usageData.getOrders().getTotalPages());
            model.addAttribute("totalOrders", usageData.getOrders().getTotalElements());
            return "shop/coupons/usage";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/shop/coupons";
        }
    }
}
