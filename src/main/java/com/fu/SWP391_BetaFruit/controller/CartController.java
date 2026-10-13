package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping({"/cart", "/customer/cart"})
    public String viewCart(HttpSession session, Model model) {
        var summary = cartService.getSummary(customerId(session));
        model.addAttribute("cart", summary);
        model.addAttribute("cartCount", summary.totalQuantity());
        model.addAttribute("cartToken", CartFormToken.get(session));
        return "customer/cart";
    }

    @PostMapping("/customer/cart/items")
    public String add(@RequestParam Integer variantId, @RequestParam Integer quantity,
                      @RequestParam(required = false) String cartToken, HttpSession session,
                      RedirectAttributes redirect) {
        return mutate(session, cartToken, redirect,
                () -> cartService.addItem(customerId(session), variantId, quantity), "Đã thêm vào giỏ hàng.");
    }

    @PostMapping("/customer/cart/items/add")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> addWithoutRedirect(
            @RequestParam Integer variantId, @RequestParam Integer quantity,
            @RequestParam(required = false) String cartToken, HttpSession session) {
        try {
            CartFormToken.validate(session, cartToken);
            cartService.addItem(customerId(session), variantId, quantity);
            long count = cartService.getItemCount(customerId(session));
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                    "message", "Đã thêm vào giỏ hàng.", "cartCount", count));
        } catch (IllegalArgumentException ex) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/customer/cart/items/{itemId}/quantity")
    public String update(@PathVariable Integer itemId, @RequestParam Integer quantity,
                         @RequestParam(required = false) String cartToken, HttpSession session,
                         RedirectAttributes redirect) {
        return mutate(session, cartToken, redirect,
                () -> cartService.updateQuantity(customerId(session), itemId, quantity), "Đã cập nhật số lượng.");
    }

    @PostMapping("/customer/cart/items/{itemId}/remove")
    public String remove(@PathVariable Integer itemId, @RequestParam(required = false) String cartToken,
                         HttpSession session, RedirectAttributes redirect) {
        return mutate(session, cartToken, redirect,
                () -> cartService.removeItem(customerId(session), itemId), "Đã xóa sản phẩm khỏi giỏ.");
    }

    @PostMapping("/customer/cart/clear")
    public String clear(@RequestParam(required = false) String cartToken, HttpSession session,
                        RedirectAttributes redirect) {
        return mutate(session, cartToken, redirect,
                () -> cartService.clearCart(customerId(session)), "Đã xóa toàn bộ giỏ hàng.");
    }

    @ExceptionHandler({org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public String invalidInput(RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage", "Phân loại và số lượng phải là số nguyên hợp lệ.");
        return "redirect:/cart";
    }

    private String mutate(HttpSession session, String token, RedirectAttributes redirect,
                          Runnable action, String message) {
        try {
            CartFormToken.validate(session, token);
            action.run();
            redirect.addFlashAttribute("successMessage", message);
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    private Integer customerId(HttpSession session) {
        return (Integer) session.getAttribute("LOGGED_IN_USER_ID");
    }
}
