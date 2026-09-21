package com.fu.SWP391_BetaFruit.controller.admin;

import com.fu.SWP391_BetaFruit.entity.Product;
import com.fu.SWP391_BetaFruit.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public String listProducts(@RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               Model model) {
        List<Product> products = productService.searchProducts(keyword, tab);

        model.addAttribute("products", products);
        model.addAttribute("currentTab", tab.toUpperCase());
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        model.addAttribute("totalCount", productService.countTotal());
        model.addAttribute("pendingCount", productService.countPending());
        model.addAttribute("activeCount", productService.countActive());
        model.addAttribute("hiddenCount", productService.countHidden());
        model.addAttribute("rejectedCount", productService.countRejected());

        return "admin/products/list";
    }

    @PostMapping("/{id}/approve")
    public String approveProduct(@PathVariable("id") Integer id,
                                 @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                                 RedirectAttributes redirectAttributes) {
        try {
            productService.approveProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt hoa quả thành công! Sản phẩm đã được kích hoạt mở bán trên sàn.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products?tab=" + tab;
    }

    @PostMapping("/{id}/reject")
    public String rejectProduct(@PathVariable("id") Integer id,
                                @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                                RedirectAttributes redirectAttributes) {
        try {
            productService.rejectProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối kiểm duyệt sản phẩm hoa quả này!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products?tab=" + tab;
    }

    @PostMapping("/{id}/toggle-visibility")
    public String toggleVisibility(@PathVariable("id") Integer id,
                                   @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                                   RedirectAttributes redirectAttributes) {
        try {
            productService.toggleProductVisibility(id);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái hiển thị hoa quả thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products?tab=" + tab;
    }
}
