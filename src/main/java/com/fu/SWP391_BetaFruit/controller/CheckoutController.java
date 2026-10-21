package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.customer.CheckoutResponse;
import com.fu.SWP391_BetaFruit.service.CartService;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutService;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutPaymentService;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import jakarta.servlet.http.HttpServletRequest;
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
    private final CheckoutPaymentService checkoutPayments;
    private record Draft(Integer customerId, CheckoutResponse preview, String token) {}
    private record Receipt(Integer customerId, List<CheckoutResponse.PlacedOrder> orders,
                           PaymentMethod paymentMethod, String paymentToken) {}

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
                String savedCode = previous != null && Objects.equals(previous.customerId(), id)
                        && previous.preview().coupon() != null
                        && new java.util.HashSet<>(ids).equals(new java.util.HashSet<>(previous.preview().itemIds()))
                        ? previous.preview().coupon().code() : null;
                CheckoutResponse preview;
                try {
                    preview = savedCode == null ? checkout.preview(id, ids) : checkout.preview(id, ids, savedCode);
                } catch (IllegalArgumentException ex) {
                    if (savedCode == null) throw ex;
                    preview = checkout.preview(id, ids);
                    model.addAttribute("errorMessage", "Mã đã áp dụng không còn hợp lệ: " + ex.getMessage());
                }
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

    @PostMapping("/coupon")
    public String applyCoupon(@RequestParam String checkoutToken,
                              @RequestParam(defaultValue = "") String couponCode,
                              @RequestParam(defaultValue = "apply") String couponAction,
                              @RequestParam(required = false) Integer addressId,
                              @RequestParam(defaultValue = "COD") PaymentMethod paymentMethod,
                              HttpSession session, RedirectAttributes redirect) {
        synchronized (session) {
            Draft draft = (Draft) session.getAttribute("CHECKOUT_DRAFT");
            if (draft == null || !Objects.equals(draft.customerId(), customerId(session))
                    || !draft.token().equals(checkoutToken)) {
                redirect.addFlashAttribute("errorMessage", "Phiên áp dụng mã đã hết hạn. Vui lòng mở lại checkout.");
                return "redirect:/cart";
            }
            try {
                if (!"remove".equals(couponAction) && couponCode.isBlank())
                    throw new IllegalArgumentException("Vui lòng nhập mã giảm giá.");
                var preview = "remove".equals(couponAction)
                        ? checkout.preview(customerId(session), draft.preview().itemIds())
                        : checkout.preview(customerId(session), draft.preview().itemIds(), couponCode);
                session.setAttribute("CHECKOUT_DRAFT", new Draft(customerId(session), preview, UUID.randomUUID().toString()));
                redirect.addFlashAttribute("successMessage", "remove".equals(couponAction)
                        ? "Đã bỏ mã giảm giá." : "Đã áp dụng mã giảm giá.");
            } catch (IllegalArgumentException ex) {
                redirect.addFlashAttribute("errorMessage", ex.getMessage());
            }
            redirect.addFlashAttribute("selectedAddressId", addressId);
            redirect.addFlashAttribute("selectedPaymentMethod", paymentMethod.name());
            return "redirect:/customer/checkout";
        }
    }

    @PostMapping
    public String place(@RequestParam(required = false) Integer addressId,
                        @RequestParam(required = false) String checkoutToken,
                        HttpSession session, RedirectAttributes redirect,
                        @RequestParam(defaultValue = "COD") PaymentMethod paymentMethod,
                        HttpServletRequest request) {
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
                var result = paymentMethod == PaymentMethod.COD
                        ? checkout.place(customerId(session), draft.preview(), addressId)
                        : checkout.place(customerId(session), draft.preview(), addressId, paymentMethod);
                session.removeAttribute("CHECKOUT_DRAFT");
                session.setAttribute("CHECKOUT_RECEIPT", new Receipt(customerId(session), result,
                        paymentMethod, UUID.randomUUID().toString()));
                if (paymentMethod == PaymentMethod.VNPAY && !result.isEmpty()) {
                    return startPayment(result.getFirst().orderId(), session, redirect, request);
                }
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
        model.addAttribute("paymentMethod", receipt.paymentMethod().name());
        model.addAttribute("paymentToken", receipt.paymentToken());
        if (receipt.paymentMethod() == PaymentMethod.VNPAY) {
            model.addAttribute("paymentStates", checkoutPayments.states(customerId(session),
                    receipt.orders().stream().map(CheckoutResponse.PlacedOrder::orderId).toList()));
        }
        model.addAttribute("cartCount", carts.getItemCount(customerId(session)));
        return "customer/checkout-success";
    }

    @PostMapping("/{orderId}/pay")
    public String pay(@PathVariable Integer orderId, @RequestParam String paymentToken,
                      HttpSession session, RedirectAttributes redirect, HttpServletRequest request) {
        synchronized (session) {
            Receipt receipt = (Receipt) session.getAttribute("CHECKOUT_RECEIPT");
            if (receipt == null || !Objects.equals(receipt.customerId(), customerId(session))
                    || receipt.paymentMethod() != PaymentMethod.VNPAY
                    || !receipt.paymentToken().equals(paymentToken)
                    || receipt.orders().stream().noneMatch(order -> order.orderId().equals(orderId))) {
                redirect.addFlashAttribute("errorMessage", "Phiên thanh toán không hợp lệ. Vui lòng kiểm tra lại đơn hàng.");
                return "redirect:/cart";
            }
            return startPayment(orderId, session, redirect, request);
        }
    }

    private String startPayment(Integer orderId, HttpSession session, RedirectAttributes redirect,
                                HttpServletRequest request) {
        try {
            String baseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                    .fromRequestUri(request).replacePath(request.getContextPath()).replaceQuery(null)
                    .build().toUriString();
            String ipAddress = request.getRemoteAddr();
            if ("::1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress)) ipAddress = "127.0.0.1";
            return "redirect:" + checkoutPayments.start(customerId(session), orderId, ipAddress, baseUrl);
        } catch (Exception exception) {
            log.warn("Cannot start checkout payment for orderId={}", orderId, exception);
            redirect.addFlashAttribute("errorMessage", "Đơn hàng đã được tạo nhưng chưa thể mở thanh toán. "
                    + "Vui lòng kiểm tra trạng thái và thử lại bên dưới.");
            return "redirect:/customer/checkout/success";
        }
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
