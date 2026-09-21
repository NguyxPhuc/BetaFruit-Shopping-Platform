package com.fu.SWP391_BetaFruit.controller.admin;

import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;
import com.fu.SWP391_BetaFruit.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/shops")
@RequiredArgsConstructor
public class AdminShopController {

    private final ShopService shopService;

    @GetMapping
    public String listShops(@RequestParam(value = "status", required = false) String statusStr,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            Model model) {
        ShopApprovalStatus filterStatus = null;
        if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr.trim())) {
            try {
                filterStatus = ShopApprovalStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<Shop> shops = shopService.searchShops(keyword, filterStatus);

        model.addAttribute("shops", shops);
        model.addAttribute("currentStatus", filterStatus != null ? filterStatus.name() : "ALL");
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        model.addAttribute("totalCount", shopService.countTotal());
        model.addAttribute("pendingCount", shopService.countByStatus(ShopApprovalStatus.PENDING));
        model.addAttribute("approvedCount", shopService.countByStatus(ShopApprovalStatus.APPROVED));
        model.addAttribute("rejectedCount", shopService.countByStatus(ShopApprovalStatus.REJECTED));

        return "admin/shops/list";
    }

    @PostMapping("/{id}/approve")
    public String approveShop(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            shopService.approveShop(id);
            redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt cửa hàng thành công! Chủ shop đã được cấp quyền bán hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/shops";
    }

    @PostMapping("/{id}/reject")
    public String rejectShop(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            shopService.rejectShop(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đơn đăng ký mở cửa hàng này!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/shops";
    }
}
