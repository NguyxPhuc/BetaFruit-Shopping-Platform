package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.CouponRequest;
import com.fu.SWP391_BetaFruit.dto.response.CouponUsageResponse;
import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import com.fu.SWP391_BetaFruit.service.CouponService;
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
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    // =========================================================================
    // 1. DANH SÁCH MÃ GIẢM GIÁ (ADMIN)
    // URL: /admin/coupons
    // =========================================================================
    @GetMapping
    public String listAdminCoupons(@RequestParam(value = "keyword", required = false) String keyword,
                                   @RequestParam(value = "scope", defaultValue = "PLATFORM") String scope,
                                   @RequestParam(value = "status", defaultValue = "ALL") String status,
                                   @RequestParam(value = "page", defaultValue = "1") int page,
                                   @RequestParam(value = "size", defaultValue = "10") int size,
                                   Model model) {
        Map<String, Object> pageData = couponService.getAdminCouponsPageData(keyword, scope, status, page, size);
        model.addAllAttributes(pageData);
        model.addAttribute("activeMenu", "coupons");
        return "admin/coupons/list";
    }

    // =========================================================================
    // 2. GIAO DIỆN TẠO MÃ TOÀN SÀN MỚI (ADMIN)
    // URL: /admin/coupons/create
    // =========================================================================
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("couponRequest")) {
            CouponRequest request = new CouponRequest();
            request.setCouponCategory(CouponCategory.SHIPPING); // Default Freeship for platform
            request.setDiscountType(DiscountType.FIXED_AMOUNT);
            request.setStartDate(LocalDateTime.now().withSecond(0).withNano(0));
            request.setEndDate(LocalDateTime.now().plusDays(30).withSecond(0).withNano(0));
            request.setUsageLimit(500);
            request.setIsActive(true);
            model.addAttribute("couponRequest", request);
        }

        model.addAttribute("categories", CouponCategory.values());
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("activeMenu", "coupons");
        return "admin/coupons/create";
    }

    // =========================================================================
    // 3. XỬ LÝ LƯU TẠO MÃ TOÀN SÀN
    // URL: /admin/coupons/create
    // =========================================================================
    @PostMapping("/create")
    public String processCreate(@Valid @ModelAttribute("couponRequest") CouponRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("activeMenu", "coupons");
            return "admin/coupons/create";
        }

        try {
            Coupon saved = couponService.createAdminCoupon(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Tạo mã ưu đãi sàn '" + saved.getCouponCode() + "' thành công!");
            return "redirect:/admin/coupons";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("activeMenu", "coupons");
            return "admin/coupons/create";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Đã xảy ra lỗi: " + ex.getMessage());
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("activeMenu", "coupons");
            return "admin/coupons/create";
        }
    }

    // =========================================================================
    // 4. GIAO DIỆN CHỈNH SỬA MÃ GIẢM GIÁ (ADMIN)
    // URL: /admin/coupons/{id}/edit
    // =========================================================================
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Integer id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            Coupon originalCoupon = couponService.getAdminCouponById(id);
            if (!model.containsAttribute("couponRequest")) {
                CouponRequest request = couponService.getAdminCouponRequestForEdit(id);
                model.addAttribute("couponRequest", request);
            }
            model.addAttribute("coupon", originalCoupon);
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("isLocked", originalCoupon.getUsedCount() != null && originalCoupon.getUsedCount() > 0);
            model.addAttribute("activeMenu", "coupons");
            return "admin/coupons/edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/coupons";
        }
    }

    // =========================================================================
    // 5. XỬ LÝ LƯU CẬP NHẬT MÃ (ADMIN)
    // URL: /admin/coupons/{id}/edit
    // =========================================================================
    @PostMapping("/{id}/edit")
    public String processEdit(@PathVariable("id") Integer id,
                              @Valid @ModelAttribute("couponRequest") CouponRequest request,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (bindingResult.hasErrors()) {
            Coupon originalCoupon = couponService.getAdminCouponById(id);
            model.addAttribute("coupon", originalCoupon);
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("isLocked", originalCoupon.getUsedCount() != null && originalCoupon.getUsedCount() > 0);
            model.addAttribute("activeMenu", "coupons");
            return "admin/coupons/edit";
        }

        try {
            Coupon updated = couponService.updateAdminCoupon(id, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật mã giảm giá '" + updated.getCouponCode() + "' thành công!");
            return "redirect:/admin/coupons";
        } catch (IllegalArgumentException ex) {
            Coupon originalCoupon = couponService.getAdminCouponById(id);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("coupon", originalCoupon);
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("isLocked", originalCoupon.getUsedCount() != null && originalCoupon.getUsedCount() > 0);
            model.addAttribute("activeMenu", "coupons");
            return "admin/coupons/edit";
        } catch (Exception ex) {
            Coupon originalCoupon = couponService.getAdminCouponById(id);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi: " + ex.getMessage());
            model.addAttribute("coupon", originalCoupon);
            model.addAttribute("categories", CouponCategory.values());
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("isLocked", originalCoupon.getUsedCount() != null && originalCoupon.getUsedCount() > 0);
            model.addAttribute("activeMenu", "coupons");
            return "admin/coupons/edit";
        }
    }

    // =========================================================================
    // 6. BẬT / TẮT KÍCH HOẠT MÃ (ADMIN)
    // URL: /admin/coupons/{id}/toggle-status
    // =========================================================================
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Integer id,
                               @RequestParam(value = "scope", required = false) String scope,
                               @RequestParam(value = "status", required = false) String status,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               RedirectAttributes redirectAttributes) {
        try {
            boolean active = couponService.toggleAdminCouponStatus(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    active ? "Đã kích hoạt lại mã thành công!" : "Đã tạm dừng mã giảm giá thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        StringBuilder redirectUrl = new StringBuilder("redirect:/admin/coupons?page=").append(page);
        if (scope != null && !scope.trim().isEmpty()) {
            redirectUrl.append("&scope=").append(scope.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            redirectUrl.append("&status=").append(status.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            redirectUrl.append("&keyword=").append(keyword.trim());
        }
        return redirectUrl.toString();
    }

    // =========================================================================
    // 7. THEO DÕI SỐ LƯỢT DÙNG & LỊCH SỬ ĐƠN HÀNG (ADMIN)
    // URL: /admin/coupons/{id}/usage
    // =========================================================================
    @GetMapping("/{id}/usage")
    public String viewUsage(@PathVariable("id") Integer id,
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        try {
            CouponUsageResponse usageData = couponService.getAdminCouponUsage(id, page, size);
            model.addAttribute("usageData", usageData);
            model.addAttribute("coupon", usageData.getCoupon());
            model.addAttribute("orders", usageData.getOrders().getContent());
            model.addAttribute("orderPage", usageData.getOrders());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", usageData.getOrders().getTotalPages());
            model.addAttribute("totalOrders", usageData.getOrders().getTotalElements());
            model.addAttribute("activeMenu", "coupons");
            return "admin/coupons/usage";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/coupons";
        }
    }
}
