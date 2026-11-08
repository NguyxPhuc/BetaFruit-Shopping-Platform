package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.CustomerOrderResponse;
import com.fu.SWP391_BetaFruit.dto.response.TierUpgradeResult;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.service.CustomerOrderService;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutPaymentService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
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
    private final CheckoutPaymentService checkoutPayments;

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
        model.addAttribute("paymentStates", checkoutPayments.states(customerId,
                ordersPage.getContent().stream().map(CustomerOrderResponse::orderId).toList()));
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
        model.addAttribute("paymentState", checkoutPayments.states(customerId, java.util.List.of(orderId)).get(orderId));

        return "customer/order-detail";
    }

    @PostMapping("/{orderId}/pay")
    public String retryVnpay(@PathVariable Integer orderId, HttpSession session,
                             HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Integer customerId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (customerId == null) return "redirect:/auth/login";

        try {
            String baseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                    .fromRequestUri(request).replacePath(request.getContextPath()).replaceQuery(null)
                    .build().toUriString();
            String ipAddress = request.getRemoteAddr();
            if ("::1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress)) ipAddress = "127.0.0.1";
            return "redirect:" + checkoutPayments.start(customerId, orderId, ipAddress, baseUrl);
        } catch (Exception exception) {
            log.warn("Cannot retry VNPay for customerId={}, orderId={}", customerId, orderId, exception);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Chưa thể mở thanh toán VNPay. Đơn hàng vẫn được giữ lại, vui lòng thử lại.");
            return "redirect:/orders/" + orderId;
        }
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
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận đã nhận hàng thành công.");

            if (upgradeResult != null && upgradeResult.isUpgraded()) {
                redirectAttributes.addFlashAttribute("upgradeCelebration", true);
                redirectAttributes.addFlashAttribute("newTierName", upgradeResult.getNewTierName());
                redirectAttributes.addFlashAttribute("welcomeCoupon", upgradeResult.getWelcomeCouponCode());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/orders/" + orderId;
    }
}
