package com.fu.SWP391_BetaFruit.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartSummaryResponse(List<CartItemResponse> items, long totalQuantity,
        BigDecimal totalAmount, boolean valid) {
    public List<ShopCartResponse> getShops() {
        var grouped = new java.util.LinkedHashMap<Integer, java.util.List<CartItemResponse>>();
        for (CartItemResponse item : items) {
            grouped.computeIfAbsent(item.shopId(), id -> new java.util.ArrayList<>()).add(item);
        }
        return grouped.entrySet().stream().map(entry -> {
            var shopItems = List.copyOf(entry.getValue());
            long quantity = shopItems.stream().filter(CartItemResponse::available)
                    .mapToLong(CartItemResponse::quantity).sum();
            BigDecimal amount = shopItems.stream().filter(CartItemResponse::available)
                    .map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new ShopCartResponse(entry.getKey(), shopItems.getFirst().shopName(), shopItems,
                    quantity, amount, shopItems.stream().allMatch(CartItemResponse::available));
        }).toList();
    }
}
