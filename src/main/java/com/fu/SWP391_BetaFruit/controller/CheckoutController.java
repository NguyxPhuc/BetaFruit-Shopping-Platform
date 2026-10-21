package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.customer.CheckoutResponse;
import com.fu.SWP391_BetaFruit.service.CartService;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/customer/checkout")
public class CheckoutController {
    private final CheckoutService checkout;
    private final CartService carts;
    private record Draft(Integer customerId, CheckoutResponse preview, String token) {}
    private record Receipt(Integer customerId, List<CheckoutResponse.PlacedOrder> orders) {}

    @ModelAttribute
    public void preventCachedForms(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }

    @GetMapping
    public String view(@RequestParam(required = false) List<Integer> itemIds, HttpSession session,
                       Model model, RedirectAttributes redirect) {
        synchronized (session) {
            Integer id = customerId(session);
            Draft previous = (Draft) session.getAttribute("CHECKOUT_DRAFT");
            try {
                var ids = itemIds != null ? itemIds : previous != null && previous.customerId().equals(id)
                        ? previous.preview().itemIds() : List.<Integer>of();
                var preview = checkout.preview(id, ids);
                // Adding an address or revisiting the same checkout must not invalidate
                // a rendered form. Rotate only when the customer or reviewed cart changes.
                var draft = previous != null && Objects.equals(previous.customerId(), id)
                        && previous.preview().equals(preview)
                        ? previous : new Draft(id, preview, UUID.randomUUID().toString());
                session.setAttribute("CHECKOUT_DRAFT", draft);
                model.addAttribute("checkout", preview);
                model.addAttribute("addresses", checkout.shippingAddresses(id));
                model.addAttribute("checkoutToken", draft.token());
                model.addAttribute("cartCount", carts.getItemCount(id));
                return "customer/checkout";
            } catch (IllegalArgumentException ex) {
                redirect.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/cart";
            }
        }
    }

    @PostMapping
    public String place(@RequestParam(required = false) Integer addressId,
                        @RequestParam(required = false) String checkoutToken,
                        HttpSession session, RedirectAttributes redirect) {
        synchronized (session) {
            Draft draft = (Draft) session.getAttribute("CHECKOUT_DRAFT");
            String invalidReason = draft == null ? "MISSING_DRAFT"
                    : !Objects.equals(draft.customerId(), customerId(session)) ? "CUSTOMER_CHANGED"
                    : checkoutToken == null || checkoutToken.isBlank() ? "MISSING_TOKEN"
                    : !draft.token().equals(checkoutToken) ? "TOKEN_MISMATCH" : null;
            if (invalidReason != null) {
                // Diagnose invalid submissions without logging tokens or session identifiers.
                log.warn("Checkout submission rejected: {}", invalidReason);
                redirect.addFlashAttribute("errorMessage", "Phiên đặt hàng đã hết hạn hoặc đã được sử dụng. Vui lòng kiểm tra lại giỏ hàng.");
                return "redirect:/cart";
            }
            try {
                var result = checkout.place(customerId(session), draft.preview(), addressId);
                session.removeAttribute("CHECKOUT_DRAFT");
                session.setAttribute("CHECKOUT_RECEIPT", new Receipt(customerId(session), result));
                return "redirect:/customer/checkout/success";
            } catch (IllegalArgumentException ex) {
                redirect.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/customer/checkout";
            }
        }
    }

    @GetMapping("/success")
    public String success(HttpSession session, Model model) {
        Receipt receipt = (Receipt) session.getAttribute("CHECKOUT_RECEIPT");
        if (receipt == null || !receipt.customerId().equals(customerId(session))) return "redirect:/cart";
        model.addAttribute("orders", receipt.orders());
        model.addAttribute("cartCount", carts.getItemCount(customerId(session)));
        return "customer/checkout-success";
    }

    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public String invalidInput(RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage", "Thông tin checkout không hợp lệ. Vui lòng chọn lại sản phẩm và địa chỉ.");
        return "redirect:/cart";
    }

    private Integer customerId(HttpSession session) {
        return (Integer) session.getAttribute("LOGGED_IN_USER_ID");
    }
}
