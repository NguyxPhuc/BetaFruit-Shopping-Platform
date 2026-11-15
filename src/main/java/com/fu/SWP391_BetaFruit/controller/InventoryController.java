package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.service.InventoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // =========================================================================
    // 1. BẬT / TẮT MỞ BÁN BIẾN THỂ (MANAGE VARIANT AVAILABILITY)
    // =========================================================================
    @PostMapping("/shop/inventory/variants/{id}/toggle-availability")
    public String toggleVariantAvailability(@PathVariable("id") Integer variantId,
                                            HttpSession session,
                                            RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            boolean isAvailable = inventoryService.toggleVariantAvailability(userId, variantId);
            redirectAttributes.addFlashAttribute("successMessage",
                    isAvailable ? "Đã mở bán lại thực thể thành công!" : "Đã tạm dừng bán thực thể này!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/shop/products";
    }

    // =========================================================================
    // 2. CẬP NHẬT TỒN KHO TRỰC TIẾP (UPDATE STOCK QUANTITY)
    // =========================================================================
    @PostMapping("/shop/inventory/variants/{id}/update-stock")
    public String updateStockQuantity(@PathVariable("id") Integer variantId,
                                      @RequestParam("newStock") int newStock,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            inventoryService.updateStockQuantity(userId, variantId, newStock);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật số lượng tồn kho thành " + newStock + " thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/shop/products";
    }


}
