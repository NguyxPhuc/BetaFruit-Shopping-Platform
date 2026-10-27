package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.CartItemResponse;
import com.fu.SWP391_BetaFruit.dto.response.CheckoutResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutService {
    private final CartService carts;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItems;
    private final UserRepository users;
    private final AddressRepository addresses;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderStatusHistoryRepository histories;
    private final PaymentTransactionRepository payments;
    private final CheckoutCouponRepository coupons;

    @Value("${checkout.shipping-fee:0}")
    private BigDecimal shippingFee = BigDecimal.ZERO;
    @Value("${checkout.default-commission-rate:0}")
    private BigDecimal defaultCommissionRate = BigDecimal.ZERO;

    public CheckoutResponse preview(Integer customerId, List<Integer> selectedIds) {
        return preview(customerId, selectedIds, null);
    }

    public CheckoutResponse preview(Integer customerId, List<Integer> selectedIds, String couponCode) {
        var summary = carts.getSummary(customerId); // Also validates active CUSTOMER access.
        var selected = selectedIds == null ? Set.<Integer>of() : new HashSet<>(selectedIds);
        if (selected.isEmpty() || selected.contains(null))
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một sản phẩm trong giỏ hàng.");
        var lines = summary.items().stream().filter(i -> selected.contains(i.itemId())).toList();
        if (lines.size() != selected.size())
            throw new IllegalArgumentException("Sản phẩm đã thay đổi hoặc không thuộc giỏ hàng của bạn.");
        for (var line : lines) {
            if (!line.available()) throw new IllegalArgumentException(line.productName() + ": " + line.warning());
        }
        var entities = selectedItems(customerId, selected);
        var byId = lines.stream().collect(Collectors.toMap(CartItemResponse::itemId, Function.identity()));
        Map<Integer, List<CartItem>> groups = entities.stream().collect(Collectors.groupingBy(
                i -> i.getVariant().getProduct().getShop().getShopId(), LinkedHashMap::new, Collectors.toList()));
        if (groups.size() > 1)
            throw new IllegalArgumentException("Chỉ được thanh toán các sản phẩm cùng một cửa hàng trong mỗi lần checkout. "
                    + "Vui lòng bỏ chọn sản phẩm của cửa hàng khác.");
        BigDecimal fee = OrderPricing.shippingFee(shippingFee);
        List<CheckoutResponse.ShopGroup> shops = new ArrayList<>();
        for (var group : groups.entrySet()) {
            var items = group.getValue().stream().map(i -> byId.get(i.getCartItemId())).toList();
            var total = items.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            shops.add(new CheckoutResponse.ShopGroup(group.getKey(),
                    group.getValue().getFirst().getVariant().getProduct().getShop().getShopName(),
                    items, total, fee, OrderPricing.customerTotal(total, fee, BigDecimal.ZERO)));
        }
        BigDecimal subtotal = lines.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = fee.multiply(BigDecimal.valueOf(shops.size()));
        var quote = new CheckoutResponse(List.copyOf(shops), lines.stream().map(CartItemResponse::itemId).toList(),
                lines.stream().mapToLong(CartItemResponse::quantity).sum(), subtotal, shipping,
                OrderPricing.customerTotal(subtotal, shipping, BigDecimal.ZERO));
        if (couponCode == null || couponCode.isBlank()) return quote;
        Coupon coupon = coupons.findByCouponCodeIgnoreCase(couponCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá."));
        var group = shops.getFirst();
        // Admin coupons are identified by ShopId = null, matching createAdminCoupon().
        // No creator UserId is stored in the current coupon schema.
        boolean platform = coupon.getShop() == null;
        if (!platform && !Objects.equals(coupon.getShop().getShopId(), group.shopId()))
            throw new IllegalArgumentException("Mã giảm giá chỉ áp dụng cho sản phẩm của cửa hàng phát hành.");
        if (coupon.getCouponCategory() == null)
            throw new IllegalArgumentException("Mã giảm giá chưa có loại áp dụng.");
        CheckoutCouponRules.validate(coupon, subtotal, java.time.LocalDateTime.now());
        BigDecimal discount = OrderPricing.couponDiscount(coupon, subtotal, shipping);
        if (discount.signum() == 0)
            throw new IllegalArgumentException("Mã giảm giá không làm giảm số tiền đơn hàng này.");
        Shop shop = entities.getFirst().getVariant().getProduct().getShop();
        var pricing = OrderPricing.quote(shop, coupon, subtotal, shipping, discount, defaultCommissionRate);
        BigDecimal finalAmount = pricing.finalAmount();
        var discountedGroup = new CheckoutResponse.ShopGroup(group.shopId(), group.shopName(), group.items(),
                subtotal, shipping, finalAmount);
        return new CheckoutResponse(List.of(discountedGroup), quote.itemIds(), quote.quantity(), subtotal,
                shipping, finalAmount, new CheckoutResponse.AppliedCoupon(coupon.getCouponId(),
                coupon.getCouponCode(), coupon.getCouponCategory(), discount, platform));
    }

    public List<Address> shippingAddresses(Integer customerId) {
        carts.getSummary(customerId);
        return addresses.findByUser_UserIdAndAddressTypeAndIsActiveTrue(customerId, AddressType.SHIPPING)
                .stream().sorted(Comparator.comparing(a -> !Boolean.TRUE.equals(a.getIsDefault()))).toList();
    }

    @Transactional
    public List<CheckoutResponse.PlacedOrder> place(Integer customerId, CheckoutResponse reviewed, Integer addressId) {
        return place(customerId, reviewed, addressId, PaymentMethod.COD);
    }

    @Transactional
    public List<CheckoutResponse.PlacedOrder> place(Integer customerId, CheckoutResponse reviewed, Integer addressId,
                                                   PaymentMethod paymentMethod) {
        if (paymentMethod == null) throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán.");
        // Cart mutations take this same lock. Two tabs cannot consume the same cart lines twice.
        User customer = users.findCartCustomerForUpdate(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại."));
        Coupon coupon = reviewed.coupon() == null ? null : coupons.findLockedById(reviewed.coupon().couponId())
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không còn tồn tại."));
        var current = coupon == null ? preview(customerId, reviewed.itemIds())
                : preview(customerId, reviewed.itemIds(), reviewed.coupon().code());
        if (!current.equals(reviewed))
            throw new IllegalArgumentException("Giá, số lượng hoặc tồn kho đã thay đổi. Vui lòng kiểm tra lại checkout.");
        if (paymentMethod == PaymentMethod.VNPAY && current.finalAmount().signum() == 0)
            throw new IllegalArgumentException("Đơn hàng đã được giảm còn 0đ. Vui lòng chọn COD để hoàn tất đơn hàng.");
        if (addressId == null) throw new IllegalArgumentException("Vui lòng chọn địa chỉ nhận hàng.");
        Address address = addresses.findByUser_UserIdAndAddressIdAndIsActive(customerId, addressId, true)
                .filter(a -> a.getAddressType() == AddressType.SHIPPING)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ nhận hàng không hợp lệ hoặc đã bị xóa."));
        String snapshot = String.join(" | ", address.getReceiverName(), address.getReceiverPhone(),
                address.getAddressLine(), address.getWard(), address.getDistrict(), address.getCity());
        var selected = selectedItems(customerId, new HashSet<>(current.itemIds()));
        var byId = selected.stream().collect(Collectors.toMap(CartItem::getCartItemId, Function.identity()));
        List<CheckoutResponse.PlacedOrder> result = new ArrayList<>();
        for (var group : current.shops()) {
            Shop shop = byId.get(group.items().getFirst().itemId()).getVariant().getProduct().getShop();
            Order order = new Order();
            order.setCustomer(customer);
            order.setShop(shop);
            order.setSnapshotShippingAddress(snapshot);
            order.setTotalAmount(group.subtotal());
            order.setCoupon(coupon);
            order.setDiscountAmount(current.discountAmount());
            order.setShippingFee(group.shippingFee());
            var pricing = OrderPricing.quote(shop, coupon, group.subtotal(), group.shippingFee(),
                    current.discountAmount(), defaultCommissionRate);
            order.setFinalAmount(pricing.finalAmount());
            order.setPlatformFee(pricing.shop().platformFee());
            order.setShopNetReceived(pricing.shop().shopNetReceived());
            order.setPaymentMethod(paymentMethod);
            order.setOrderStatus(OrderStatus.PENDING);
            order.setIsSettled(false);
            order.setIsCodRemitted(false);
            orders.save(order);
            for (var line : group.items()) {
                var variant = byId.get(line.itemId()).getVariant();
                if (variant.getCostPrice() == null || variant.getCostPrice().signum() < 0)
                    throw new IllegalArgumentException("Giá vốn sản phẩm không hợp lệ. Vui lòng liên hệ cửa hàng.");
                orderItems.save(new OrderItem(null, order, variant, line.quantity(), line.unitPrice(),
                        variant.getCostPrice(), line.productName(), line.variantName()));
            }
            histories.save(new OrderStatusHistory(null, order, customer, null, OrderStatus.PENDING,
                    "Khách hàng đặt hàng từ giỏ hàng (" + paymentMethod.name() + ")", null));
            if (paymentMethod == PaymentMethod.COD) {
                PaymentTransaction payment = new PaymentTransaction();
                payment.setOrder(order);
                payment.setGateway("COD");
                payment.setTransactionRef("COD-" + UUID.randomUUID());
                payment.setAmount(OrderPricing.customerAmount(order));
                payment.setStatus(PaymentStatus.PENDING);
                payments.save(payment);
            }
            result.add(new CheckoutResponse.PlacedOrder(order.getOrderId(), shop.getShopName(),
                    OrderPricing.customerAmount(order)));
        }
        // Remove only purchased lines, after all orders were created in this transaction.
        if (coupon != null) {
            // The locked managed coupon is flushed in the same transaction as the order.
            // Reserve one use per created order, never again on payment retry/callback.
            coupon.setUsedCount((coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) + 1);
        }
        cartItems.deleteAll(selected);
        return List.copyOf(result);
    }

    private List<CartItem> selectedItems(Integer customerId, Set<Integer> ids) {
        Cart cart = cartRepository.findByCustomer_UserId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Giỏ hàng đang trống."));
        return cartItems.findSummaryItems(cart.getCartId()).stream().filter(i -> ids.contains(i.getCartItemId())).toList();
    }

}
