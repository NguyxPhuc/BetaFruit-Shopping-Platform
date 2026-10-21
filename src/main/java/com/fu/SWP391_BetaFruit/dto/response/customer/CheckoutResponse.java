package com.fu.SWP391_BetaFruit.dto.response.customer;

import java.math.BigDecimal;
import java.util.List;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;

public record CheckoutResponse(List<ShopGroup> shops, List<Integer> itemIds, long quantity,
                               BigDecimal subtotal, BigDecimal shippingFee, BigDecimal finalAmount,
                               AppliedCoupon coupon) {
    public CheckoutResponse(List<ShopGroup> shops, List<Integer> itemIds, long quantity,
                            BigDecimal subtotal, BigDecimal shippingFee, BigDecimal finalAmount) {
        this(shops, itemIds, quantity, subtotal, shippingFee, finalAmount, null);
    }
    public BigDecimal discountAmount() { return coupon == null ? BigDecimal.ZERO : coupon.discountAmount(); }
    public record AppliedCoupon(Integer couponId, String code, CouponCategory category,
                                BigDecimal discountAmount, boolean platform) {}
    public record ShopGroup(Integer shopId, String shopName, List<CartItemResponse> items,
                            BigDecimal subtotal, BigDecimal shippingFee, BigDecimal finalAmount) {}
    public record PlacedOrder(Integer orderId, String shopName, BigDecimal finalAmount) {}
}
