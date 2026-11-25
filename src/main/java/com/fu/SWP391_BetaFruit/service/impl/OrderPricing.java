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

    /**
     * Tác dụng: Biểu thức HQL tính tiền shop nhận để database sắp xếp toàn bộ kết quả trước khi phân trang.
     * Màn hình / luồng: Shop chọn Tiền nhận ở shop/orders; ShopOrderServiceImpl.buildSort dùng biểu thức
     * này trong truy vấn OrderRepository.searchOrdersForShopOwner. Cần giữ công thức khớp với shopAmounts.
     */
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

    /**
     * Tác dụng: Tính thành tiền một dòng = đơn giá x số lượng; dùng trong giỏ hàng, chi tiết đơn
     * shop/shipper và tính tổng giá vốn.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     */
    public static BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        requireNonNegative(unitPrice, "Đơn giá không hợp lệ");
        if (quantity < 0) throw conflict("Số lượng không hợp lệ");
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Tác dụng: Kiểm tra phí vận chuyển cấu hình không âm, tối đa 2 chữ số thập phân rồi chuẩn hóa;
     * CheckoutService.preview gọi cho màn checkout.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     */
    public static BigDecimal shippingFee(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.scale() > 2)
            throw new IllegalArgumentException("Phí vận chuyển chưa được cấu hình hợp lệ.");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Tác dụng: Tính tiền khách trả = tiền hàng + vận chuyển - giảm giá và chặn kết quả âm; dùng khi
     * preview và quote checkout.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     */
    public static BigDecimal customerTotal(BigDecimal subtotal, BigDecimal shipping, BigDecimal discount) {
        requireNonNegative(subtotal, "Giá sản phẩm không hợp lệ");
        requireNonNegative(shipping, "Phí vận chuyển không hợp lệ");
        requireNonNegative(discount, "Giảm giá không hợp lệ");
        BigDecimal amount = subtotal.add(shipping).subtract(discount);
        requireNonNegative(amount, "Tổng thanh toán không hợp lệ");
        return amount;
    }

    /**
     * Tác dụng: Đọc FinalAmount đã lưu của đơn, kiểm tra không âm; dùng khi hiển thị đơn và tạo thanh
     * toán/COD để không tính lại theo giá hiện tại.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     *
     * Ghi chú hiện có:
     * Only read the saved customer charge; do not reprice an existing order.
     */
    public static BigDecimal customerAmount(Order order) {
        BigDecimal amount = order.getFinalAmount();
        requireNonNegative(amount, "Số tiền thanh toán đã lưu của đơn hàng không hợp lệ");
        return amount;
    }

    /**
     * Tác dụng: Tính số tiền voucher giảm theo tiền hàng hoặc phí ship, áp dụng trần giảm và làm tròn xuống
     * đơn vị đồng; CheckoutService dùng khi áp mã và gợi ý voucher.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     *
     * Ghi chú hiện có:
     * Coupon availability/usage is checked separately by CheckoutCouponRules.
     */
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

    /**
     * Tác dụng: Lập báo giá tạo đơn: chọn tỷ lệ phí shop hoặc mặc định, tính phí sàn/tiền shop nhận và tổng
     * khách trả; CheckoutService.preview và place gọi.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     *
     * Ghi chú hiện có:
     * Price a new order using the shop rate, or the configured checkout fallback.
     */
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

    /**
     * Tác dụng: Tính lợi nhuận dự kiến = tiền shop nhận - tổng giá vốn; trả null khi thiếu giá vốn, dùng
     * tại chi tiết đơn hàng shop.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     */
    public static BigDecimal estimatedProfit(BigDecimal shopNetReceived, BigDecimal totalCost) {
        return totalCost == null ? null : shopNetReceived.subtract(totalCost);
    }

    /**
     * Tác dụng: Chốt PlatformFee và ShopNetReceived vào đơn khi shipper xác nhận giao thành công;
     * OrderDeliveryService.markDelivered gọi để phục vụ đối soát sau đó.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     *
     * Ghi chú hiện có:
     * Called by the delivery workflow when it freezes the figures for settlement.
     */
    public static void captureShopAmounts(Order order) {
        var amounts = calculate(order);
        if (amounts.shopNetReceived().signum() < 0) throw conflict("Tiền shop thực nhận không hợp lệ");
        order.setPlatformFee(amounts.platformFee());
        order.setShopNetReceived(amounts.shopNetReceived());
    }

    /**
     * Tác dụng: Tính phí sàn và tiền shop nhận từ đơn với tỷ lệ shop hiện tại, hoặc phí đã lưu nếu không có
     * tỷ lệ; dùng để hiển thị đơn shop và khi chốt tiền giao hàng.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     */
    public static Amounts calculate(Order order) {
        BigDecimal rate = order.getShop().getCommissionRate();
        return shopAmounts(order.getTotalAmount(), order.getDiscountAmount(), couponSource(order),
                rate, rate == null ? "PLATFORM" : "SHOP", order.getPlatformFee());
    }

    /**
     * Tác dụng: Công thức nội bộ của quote/calculate: tiền shop nhận = tiền hàng - voucher shop tài trợ -
     * phí sàn; voucher sàn không trừ vào shop, tiền ship không là doanh thu shop.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     */
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

    /**
     * Tác dụng: Xác định nguồn voucher NONE, PLATFORM hoặc SHOP và kiểm tra voucher đúng shop; dùng khi
     * tính tiền, dựng dữ liệu hiển thị và xác nhận đơn.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     */
    public static String couponSource(Order order) {
        return couponSource(order.getCoupon(), order.getShop());
    }

    /**
     * Tác dụng: Xác định nguồn voucher NONE, PLATFORM hoặc SHOP và kiểm tra voucher đúng shop; dùng khi
     * tính tiền, dựng dữ liệu hiển thị và xác nhận đơn.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     */
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

    /**
     * Tác dụng: Kiểm tra tỷ lệ phí sàn nằm trong khoảng 0..100%; quote và shopAmounts dùng trước khi tính tiền.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     */
    private static boolean validRate(BigDecimal rate) {
        return rate != null && rate.signum() >= 0 && rate.compareTo(new BigDecimal("100")) <= 0;
    }

    /**
     * Tác dụng: Chặn số tiền null hoặc âm trong các phép tính tiền hàng/tổng khách trả; tạo lỗi nghiệp vụ
     * qua conflict.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     */
    private static void requireNonNegative(BigDecimal amount, String message) {
        if (amount == null || amount.signum() < 0) throw conflict(message);
    }

    /**
     * Tác dụng: Tạo lỗi HTTP 409 cho dữ liệu tiền không hợp lệ; các hàm kiểm tra trong OrderPricing dùng để
     * báo lỗi cho luồng gọi.
     * Màn hình / luồng: Dùng chung cho giỏ hàng, checkout, đơn hàng shop và đơn giao hàng; các hàm gọi nhau
     * để thống nhất công thức tiền.
     */
    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
