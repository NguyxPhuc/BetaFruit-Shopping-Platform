package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.entity.InventoryTransaction;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.enums.InventoryTransactionType;
import com.fu.SWP391_BetaFruit.service.InventoryService;
import com.fu.SWP391_BetaFruit.service.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductService productService;

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

    // =========================================================================
    // 3. NHẬP THÊM HÀNG VÀO KHO (RESTOCK PRODUCTS)
    // =========================================================================
    @PostMapping("/shop/inventory/variants/{id}/restock")
    public String restockVariant(@PathVariable("id") Integer variantId,
                                 @RequestParam("additionalQuantity") int additionalQuantity,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            inventoryService.restockVariant(userId, variantId, additionalQuantity);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Nhập thêm +" + additionalQuantity + " đơn vị vào kho thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/shop/products";
    }

    // =========================================================================
    // 4. LỊCH SỬ GIAO DỊCH KHO HÀNG (INVENTORY HISTORY)
    // =========================================================================
    @GetMapping("/shop/inventory/history")
    public String viewInventoryHistory(@RequestParam(value = "type", required = false) InventoryTransactionType type,
                                       @RequestParam(value = "page", defaultValue = "1") int page,
                                       @RequestParam(value = "size", defaultValue = "15") int size,
                                       HttpSession session,
                                       Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            Shop shop = productService.getShopByUserId(userId);
            Page<InventoryTransaction> transactionPage = inventoryService.getShopInventoryHistory(userId, type, page, size);
            long lowStockCount = inventoryService.countLowStockVariants(userId);

            model.addAttribute("shop", shop);
            model.addAttribute("transactions", transactionPage.getContent());
            model.addAttribute("transactionPage", transactionPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", transactionPage.getTotalPages());
            model.addAttribute("totalItems", transactionPage.getTotalElements());
            model.addAttribute("selectedType", type);
            model.addAttribute("transactionTypes", InventoryTransactionType.values());
            model.addAttribute("lowStockCount", lowStockCount);

            return "shop/inventory/history";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "redirect:/shop/products";
        }
    }
}
