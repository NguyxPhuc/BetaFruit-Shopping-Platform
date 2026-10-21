package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Order money calculations. All amounts are VND.
 * Customer pays = merchandise + original shipping fee - coupon discount (once).
 * Platform fee = merchandise * commission rate / 100, rounded HALF_UP to 2 decimals.
 * Shop receives = merchandise - shop-funded coupon discount - platform fee.
 * Platform-funded coupons never reduce shop receipts; shipping is not shop revenue.
 * Existing orders are charged using their saved FinalAmount, not today's prices/coupons.
 */
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

    public record Quote(BigDecimal subtotal, BigDecimal shippingFee, BigDecimal discountAmount,
                        BigDecimal finalAmount, Amounts shop) {}

    public static BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        requireNonNegative(unitPrice, "Đơn giá không hợp lệ");
        if (quantity < 0) throw conflict("Số lượng không hợp lệ");
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public static BigDecimal shippingFee(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.scale() > 2)
            throw new IllegalArgumentException("Phí vận chuyển chưa được cấu hình hợp lệ.");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal customerTotal(BigDecimal subtotal, BigDecimal shipping, BigDecimal discount) {
        requireNonNegative(subtotal, "Giá sản phẩm không hợp lệ");
        requireNonNegative(shipping, "Phí vận chuyển không hợp lệ");
        requireNonNegative(discount, "Giảm giá không hợp lệ");
        BigDecimal amount = subtotal.add(shipping).subtract(discount);
        requireNonNegative(amount, "Tổng thanh toán không hợp lệ");
        return amount;
    }

    /** Only read the saved customer charge; do not reprice an existing order. */
    public static BigDecimal customerAmount(Order order) {
        BigDecimal amount = order.getFinalAmount();
        requireNonNegative(amount, "Số tiền thanh toán đã lưu của đơn hàng không hợp lệ");
        return amount;
    }

    /** Coupon availability/usage is checked separately by CheckoutCouponRules. */
    public static BigDecimal couponDiscount(Coupon coupon, BigDecimal subtotal, BigDecimal shipping) {
        if (coupon.getCouponCategory() == null)
            throw new IllegalArgumentException("Mã giảm giá chưa có loại áp dụng.");
        BigDecimal base = coupon.getCouponCategory() == CouponCategory.SHIPPING ? shipping : subtotal;
        BigDecimal value = coupon.getDiscountValue();
        if (value == null || value.signum() <= 0 || base.signum() < 0)
            throw new IllegalArgumentException("Mức giảm giá không hợp lệ.");
        BigDecimal amount;
        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            if (value.compareTo(new BigDecimal("100")) > 0 || coupon.getMaxDiscountAmount() == null
                    || coupon.getMaxDiscountAmount().signum() <= 0)
                throw new IllegalArgumentException("Mã phần trăm thiếu mức giảm tối đa hợp lệ.");
            amount = base.multiply(value).movePointLeft(2).min(coupon.getMaxDiscountAmount());
        } else if (coupon.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            amount = value;
        } else {
            throw new IllegalArgumentException("Hình thức giảm giá không hợp lệ.");
        }
        // Discounts are whole dong and never exceed the product/shipping charge.
        return amount.min(base).setScale(0, RoundingMode.DOWN);
    }

    /** Price a new order using the shop rate, or the configured checkout fallback. */
    public static Quote quote(Shop shop, Coupon coupon, BigDecimal subtotal, BigDecimal shipping,
                              BigDecimal discount, BigDecimal defaultCommissionRate) {
        if (coupon != null && coupon.getShop() != null && discount.compareTo(subtotal) > 0)
            throw new IllegalArgumentException("Mức giảm của cửa hàng vượt số tiền cửa hàng có thể tài trợ.");
        BigDecimal rate = shop.getCommissionRate() == null ? defaultCommissionRate : shop.getCommissionRate();
        if (!validRate(rate))
            throw new IllegalArgumentException("Tỷ lệ phí sàn của cửa hàng không hợp lệ.");
        var amounts = shopAmounts(subtotal, discount, couponSource(coupon, shop), rate,
                shop.getCommissionRate() == null ? "PLATFORM" : "SHOP", null);
        if (amounts.shopNetReceived().signum() < 0)
            throw new IllegalArgumentException("Mức giảm của cửa hàng vượt số tiền cửa hàng có thể tài trợ.");
        return new Quote(subtotal, shipping, discount, customerTotal(subtotal, shipping, discount), amounts);
    }

    public static BigDecimal estimatedProfit(BigDecimal shopNetReceived, BigDecimal totalCost) {
        return totalCost == null ? null : shopNetReceived.subtract(totalCost);
    }

    /** Called by the delivery workflow when it freezes the figures for settlement. */
    public static void captureShopAmounts(Order order) {
        var amounts = calculate(order);
        if (amounts.shopNetReceived().signum() < 0) throw conflict("Tiền shop thực nhận không hợp lệ");
        order.setPlatformFee(amounts.platformFee());
        order.setShopNetReceived(amounts.shopNetReceived());
    }

    public static Amounts calculate(Order order) {
        BigDecimal rate = order.getShop().getCommissionRate();
        return shopAmounts(order.getTotalAmount(), order.getDiscountAmount(), couponSource(order),
                rate, rate == null ? "PLATFORM" : "SHOP", order.getPlatformFee());
    }

    private static Amounts shopAmounts(BigDecimal total, BigDecimal couponDiscount, String source,
                                       BigDecimal rate, String rateSource, BigDecimal storedFee) {
        if (rate != null && !validRate(rate)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tỷ lệ phí sàn phải từ 0 đến 100%");
        }
        BigDecimal discount = "SHOP".equals(source) && couponDiscount != null
                ? couponDiscount : BigDecimal.ZERO;
        if (total == null || total.signum() < 0 || discount.signum() < 0 || discount.compareTo(total) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giá sản phẩm hoặc giảm giá của shop không hợp lệ");
        }
        // PlatformFee is a stored currency amount in the existing database, not a rate.
        BigDecimal fee;
        if (rate != null) {
            fee = total.multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        } else {
            fee = storedFee;
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
        return couponSource(order.getCoupon(), order.getShop());
    }

    private static String couponSource(Coupon coupon, Shop shop) {
        if (coupon == null) return "NONE";
        // Read only the identifier of the original lazy association: platform coupons
        // may use ShopId = 0 without a corresponding Shop row. Never fetch its details.
        var couponShop = coupon.getShop();
        Integer couponShopId = couponShop == null ? null : couponShop.getShopId();
        if (couponShopId == null || couponShopId == 0) return "PLATFORM";
        if (couponShopId < 0 || shop == null
                || !Objects.equals(couponShopId, shop.getShopId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Voucher của shop chỉ được áp dụng cho đơn hàng của chính shop đó");
        }
        return "SHOP";
    }

    private static boolean validRate(BigDecimal rate) {
        return rate != null && rate.signum() >= 0 && rate.compareTo(new BigDecimal("100")) <= 0;
    }

    private static void requireNonNegative(BigDecimal amount, String message) {
        if (amount == null || amount.signum() < 0) throw conflict(message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
