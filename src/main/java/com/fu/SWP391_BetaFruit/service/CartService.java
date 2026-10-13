package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.customer.CartSummaryResponse;

public interface CartService {
    long getItemCount(Integer customerId);
    CartSummaryResponse getSummary(Integer customerId);
    void addItem(Integer customerId, Integer variantId, Integer quantity);
    void updateQuantity(Integer customerId, Integer itemId, Integer quantity);
    void removeItem(Integer customerId, Integer itemId);
    void clearCart(Integer customerId);
}
