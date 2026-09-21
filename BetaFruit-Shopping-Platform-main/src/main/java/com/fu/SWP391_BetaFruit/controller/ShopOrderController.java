package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.ShopOrderListItemResponse;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.service.ShopOrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/shop/orders")
public class ShopOrderController {
    private final ShopOrderService shopOrderService;

    public ShopOrderController(ShopOrderService shopOrderService) {
        this.shopOrderService = shopOrderService;
    }

    /**
     * Temporary ownerId input until Spring Security is added. Once authentication exists,
     * read the current user's id from the authenticated principal instead.
     */
    @GetMapping
    public Page<ShopOrderListItemResponse> getOrders(
            @RequestParam Integer ownerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (ownerId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerId must be positive");
        }
        return shopOrderService.getOrdersForShopOwner(ownerId, keyword, status, page, size);
    }
}
