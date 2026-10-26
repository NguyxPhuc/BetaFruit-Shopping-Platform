package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.CustomerOrderResponse;
import com.fu.SWP391_BetaFruit.dto.response.TierUpgradeResult;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.service.CustomerOrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;

    @GetMapping
    public String listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session,
            Model model
    ) {
        Integer customerId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (customerId == null) {
            return "redirect:/auth/login";
        }

        Page<CustomerOrderResponse> ordersPage = customerOrderService.getCustomerOrders(customerId, status, page, size);
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());

        return "customer/orders";
    }

    @GetMapping("/{orderId}")
    public String orderDetail(
            @PathVariable Integer orderId,
            HttpSession session,
            Model model
    ) {
        Integer customerId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (customerId == null) {
            return "redirect:/auth/login";
        }

        CustomerOrderResponse order = customerOrderService.getOrderDetail(orderId, customerId);
        model.addAttribute("order", order);

        return "customer/order-detail";
    }

    @PostMapping("/{orderId}/confirm-received")
    public String confirmReceived(
            @PathVariable Integer orderId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Integer customerId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (customerId == null) {
            return "redirect:/auth/login";
        }

        try {
            TierUpgradeResult upgradeResult = customerOrderService.confirmReceived(orderId, customerId);
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận nhận hàng thành công! Đơn hàng đã hoàn tất.");

            if (upgradeResult != null && upgradeResult.isUpgraded()) {
                redirectAttributes.addFlashAttribute("upgradeCelebration", true);
                redirectAttributes.addFlashAttribute("newTierName", upgradeResult.getNewTierName());
                redirectAttributes.addFlashAttribute("welcomeCoupon", upgradeResult.getWelcomeCouponCode());
                redirectAttributes.addFlashAttribute("upgradeMessage",
                        "🎉 CHÚC MỪNG BẠN ĐÃ THĂNG HẠNG " + upgradeResult.getNewTierName().toUpperCase()
                                + "! Đặc quyền chiết khấu đã được kích hoạt. Nhận ngay Voucher: "
                                + upgradeResult.getWelcomeCouponCode());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/orders/" + orderId;
    }
}
