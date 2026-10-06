package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.service.ProductService;
import com.fu.SWP391_BetaFruit.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class StaffController {

    private final ShopService shopService;
    private final ProductService productService;

    // ================= 0. TRANG TỔNG QUAN VẬN HÀNH (DASHBOARD) =================
    @GetMapping("/staff/dashboard")
    public String staffDashboard() {
        return "operations-staff/operation-staff-dashboard";
    }

    // ================= 1. QUẢN LÝ CỬA HÀNG (SHOPS) =================
    @GetMapping("/staff/shops")
    public String listShops(@RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "5") int size,
                            @RequestParam(value = "sortBy", defaultValue = "shopId") String sortBy,
                            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
                            Model model) {
        model.addAllAttributes(shopService.getAdminShopPageData(keyword, status, page, size, sortBy, sortDir));
        return "admin/shops/list";
    }

    @PostMapping("/staff/shops/{id}/approve")
    public String approveShop(@PathVariable("id") Integer id,
                              @RequestParam(value = "page", defaultValue = "1") int page,
                              @RequestParam(value = "keyword", required = false) String keyword,
                              @RequestParam(value = "status", required = false) String status,
                              RedirectAttributes redirectAttributes) {
        shopService.approveShop(id);
        redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt cửa hàng thành công! Chủ shop đã được cấp quyền bán hàng.");
        StringBuilder redirectUrl = new StringBuilder("redirect:/staff/shops?page=").append(page);
        if (keyword != null && !keyword.trim().isEmpty()) {
            redirectUrl.append("&keyword=").append(keyword.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            redirectUrl.append("&status=").append(status.trim());
        }
        return redirectUrl.toString();
    }

    @PostMapping("/staff/shops/{id}/reject")
    public String rejectShop(@PathVariable("id") Integer id,
                             @RequestParam(value = "page", defaultValue = "1") int page,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "status", required = false) String status,
                             RedirectAttributes redirectAttributes) {
        shopService.rejectShop(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đơn đăng ký mở cửa hàng này!");
        StringBuilder redirectUrl = new StringBuilder("redirect:/staff/shops?page=").append(page);
        if (keyword != null && !keyword.trim().isEmpty()) {
            redirectUrl.append("&keyword=").append(keyword.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            redirectUrl.append("&status=").append(status.trim());
        }
        return redirectUrl.toString();
    }

    // ================= 2. QUẢN LÝ SẢN PHẨM (PRODUCTS) =================
    @GetMapping("/staff/products")
    public String listProducts(@RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "size", defaultValue = "5") int size,
                               @RequestParam(value = "sortBy", defaultValue = "productId") String sortBy,
                               @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
                               Model model) {
        model.addAllAttributes(productService.getAdminProductPageData(keyword, tab, page, size, sortBy, sortDir));
        return "admin/products/list";
    }

    @PostMapping("/staff/products/{id}/approve")
    public String approveProduct(@PathVariable("id") Integer id,
                                 @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                                 @RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "keyword", required = false) String keyword,
                                 RedirectAttributes redirectAttributes) {
        productService.approveProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt hoa quả thành công! Sản phẩm đã được kích hoạt mở bán trên sàn.");
        StringBuilder redirectUrl = new StringBuilder("redirect:/staff/products?tab=").append(tab).append("&page=").append(page);
        if (keyword != null && !keyword.trim().isEmpty()) {
            redirectUrl.append("&keyword=").append(keyword.trim());
        }
        return redirectUrl.toString();
    }

    @PostMapping("/staff/products/{id}/reject")
    public String rejectProduct(@PathVariable("id") Integer id,
                                @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                                @RequestParam(value = "page", defaultValue = "1") int page,
                                @RequestParam(value = "keyword", required = false) String keyword,
                                RedirectAttributes redirectAttributes) {
        productService.rejectProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối kiểm duyệt sản phẩm hoa quả này!");
        StringBuilder redirectUrl = new StringBuilder("redirect:/staff/products?tab=").append(tab).append("&page=").append(page);
        if (keyword != null && !keyword.trim().isEmpty()) {
            redirectUrl.append("&keyword=").append(keyword.trim());
        }
        return redirectUrl.toString();
    }

    @PostMapping("/staff/products/{id}/toggle-visibility")
    public String toggleVisibility(@PathVariable("id") Integer id,
                                   @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                                   @RequestParam(value = "page", defaultValue = "1") int page,
                                   @RequestParam(value = "keyword", required = false) String keyword,
                                   RedirectAttributes redirectAttributes) {
        productService.toggleProductVisibility(id);
        redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái hiển thị hoa quả thành công!");
        StringBuilder redirectUrl = new StringBuilder("redirect:/staff/products?tab=").append(tab).append("&page=").append(page);
        if (keyword != null && !keyword.trim().isEmpty()) {
            redirectUrl.append("&keyword=").append(keyword.trim());
        }
        return redirectUrl.toString();
    }
}
