package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.ShopOrderListItemResponse;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.service.SessionUserService;
import com.fu.SWP391_BetaFruit.service.ShopOrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/shop/orders")
public class ShopOrderController {
    private final ShopOrderService shopOrderService;
    private final SessionUserService sessionUserService;

    public ShopOrderController(ShopOrderService shopOrderService, SessionUserService sessionUserService) {
        this.shopOrderService = shopOrderService;
        this.sessionUserService = sessionUserService;
    }

    @GetMapping
    public String orderList(
            HttpSession session,
            Model model,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User currentUser = sessionUserService.findCurrentUser(session).orElse(null);
        if (currentUser == null) return "redirect:/login";
        model.addAttribute("orders", shopOrderService.getOrdersForShopOwner(currentUser.getUserId(), keyword, status, page, size));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("shopOwnerName", currentUser.getFullName());
        model.addAttribute("shopOwnerAvatar", currentUser.getAvatarUrl());
        return "shop/orders";
    }

    @PostMapping("/{orderId}/confirm")
    public String confirmOrder(@PathVariable Integer orderId, HttpSession session) {
        shopOrderService.confirmOrder(orderId, currentUserId(session));
        return "redirect:/shop/orders";
    }

    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable Integer orderId, HttpSession session) {
        shopOrderService.cancelOrder(orderId, currentUserId(session));
        return "redirect:/shop/orders";
    }

    @GetMapping("/api")
    @ResponseBody
    public Page<ShopOrderListItemResponse> getOrdersApi(HttpSession session,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return shopOrderService.getOrdersForShopOwner(currentUserId(session), keyword, status, page, size);
    }

    private Integer currentUserId(HttpSession session) {
        return sessionUserService.findCurrentUser(session)
                .map(User::getUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập"));
    }
}
