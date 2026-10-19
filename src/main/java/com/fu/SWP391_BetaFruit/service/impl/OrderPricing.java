package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared coupon ownership rules for order reads and acceptance. */
public final class OrderPricing {
    private OrderPricing() {}

    // HQL equivalent of calculate(), used before pagination when sorting by net receipts.
    public static final String NET_SORT_EXPRESSION = "o.totalAmount - "
            + "case when fk(c.shop) = s.shopId then coalesce(o.discountAmount, 0) else 0 end - "
            + "case when s.commissionRate is not null "
            + "then round(o.totalAmount * s.commissionRate / 100, 2) else o.platformFee end";

    public record Amounts(BigDecimal commissionRate, String commissionSource,
                          BigDecimal platformFee, BigDecimal shopDiscountAmount,
                          BigDecimal shopNetReceived, String couponSource) {}

    public static Amounts calculate(Order order) {
        String source = couponSource(order);
        BigDecimal rate = order.getShop().getCommissionRate();
        String rateSource = rate == null ? "PLATFORM" : "SHOP";
        if (rate != null && (rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tỷ lệ phí sàn phải từ 0 đến 100%");
        }
        BigDecimal total = order.getTotalAmount();
        BigDecimal discount = "SHOP".equals(source) && order.getDiscountAmount() != null
                ? order.getDiscountAmount() : BigDecimal.ZERO;
        if (total == null || total.signum() < 0 || discount.signum() < 0 || discount.compareTo(total) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giá sản phẩm hoặc giảm giá của shop không hợp lệ");
        }
        // PlatformFee is a stored currency amount in the existing database, not a rate.
        BigDecimal fee;
        if (rate != null) {
            fee = total.multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        } else {
            fee = order.getPlatformFee();
            if (fee == null || fee.signum() < 0 || fee.compareTo(total) > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Khoản phí sàn đã lưu không hợp lệ");
            }
            fee = fee.setScale(2, RoundingMode.HALF_UP);
            rate = total.signum() == 0 ? BigDecimal.ZERO
                    : fee.multiply(new BigDecimal("100")).divide(total, 6, RoundingMode.HALF_UP);
        }
        BigDecimal net = total.subtract(discount).subtract(fee).setScale(2, RoundingMode.HALF_UP);
        return new Amounts(rate, rateSource, fee, discount, net, source);
    }

    public static String couponSource(Order order) {
        if (order.getCoupon() == null) return "NONE";
        // Read only the identifier of the original lazy association: platform coupons
        // may use ShopId = 0 without a corresponding Shop row. Never fetch its details.
        var couponShop = order.getCoupon().getShop();
        Integer couponShopId = couponShop == null ? null : couponShop.getShopId();
        if (couponShopId == null || couponShopId == 0) return "PLATFORM";
        if (couponShopId < 0 || order.getShop() == null
                || !Objects.equals(couponShopId, order.getShop().getShopId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Voucher của shop chỉ được áp dụng cho đơn hàng của chính shop đó");
        }
        return "SHOP";
    }
}
