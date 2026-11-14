package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.AvailableCheckoutCouponResponse;
import com.fu.SWP391_BetaFruit.dto.response.CartItemResponse;
import com.fu.SWP391_BetaFruit.dto.response.CheckoutResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.CartService;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final NotificationService notifications;
    private final TierCouponRepository tierCoupons;
    private final CustomerMembershipRepository customerMemberships;
    private final MembershipTierRepository membershipTiers;

    @Value("${checkout.shipping-fee:0}")
    private BigDecimal shippingFee = BigDecimal.ZERO;
    @Value("${checkout.default-commission-rate:0}")
    private BigDecimal defaultCommissionRate = BigDecimal.ZERO;
    @Value("${checkout.coupon.default-limit-per-user:1}")
    private int defaultCouponLimitPerUser = 1;

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

        Shop shop = entities.getFirst().getVariant().getProduct().getShop();
        CheckoutCouponRules.validateShopOwnerFraud(coupon, shop, customerId);
        CheckoutCouponRules.validate(coupon, subtotal, java.time.LocalDateTime.now());
        int allowedLimit = defaultCouponLimitPerUser;
        if (tierCoupons != null) {
            List<TierCoupon> couponTiers = tierCoupons.findByCoupon_CouponId(coupon.getCouponId());
            if (!couponTiers.isEmpty()) {
                MembershipTier customerTier = null;
                if (customerMemberships != null) {
                    var memberOpt = customerMemberships.findByUserId(customerId);
                    if (memberOpt.isPresent()) {
                        customerTier = memberOpt.get().getTier();
                    }
                }
                if (customerTier == null && membershipTiers != null) {
                    customerTier = membershipTiers.findTopByOrderByMinSpentRequiredAsc();
                }
                CheckoutCouponRules.validateTierEligibility(coupon, customerTier, couponTiers);
                allowedLimit = CheckoutCouponRules.resolveTierQuota(couponTiers, customerTier, defaultCouponLimitPerUser);
            }
        }
        if (orders != null && customerId != null) {
            long usedCount = orders.countValidUsedCouponByCustomer(customerId, coupon.getCouponId());
            CheckoutCouponRules.validateUserUsageLimit(usedCount, allowedLimit);
        }
        BigDecimal discount = OrderPricing.couponDiscount(coupon, subtotal, shipping);
        if (discount.signum() == 0)
            throw new IllegalArgumentException("Mã giảm giá không làm giảm số tiền đơn hàng này.");
        var pricing = OrderPricing.quote(shop, coupon, subtotal, shipping, discount, defaultCommissionRate);
        BigDecimal finalAmount = pricing.finalAmount();
        var discountedGroup = new CheckoutResponse.ShopGroup(group.shopId(), group.shopName(), group.items(),
                subtotal, shipping, finalAmount);
        return new CheckoutResponse(List.of(discountedGroup), quote.itemIds(), quote.quantity(), subtotal,
                shipping, finalAmount, new CheckoutResponse.AppliedCoupon(coupon.getCouponId(),
                coupon.getCouponCode(), coupon.getCouponCategory(), discount, platform,
                coupon.getDiscountType(), coupon.getDiscountValue(), coupon.getMinOrderValue(),
                coupon.getMaxDiscountAmount(), coupon.getStartDate(), coupon.getEndDate(),
                platform ? null : coupon.getShop().getShopName()));
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
                notifications.notifyNewOrder(order);
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

    public List<AvailableCheckoutCouponResponse> getAvailableCouponsForCustomer(
            Integer customerId, Integer shopId, BigDecimal subtotal, BigDecimal shippingFee) {
        if (customerId == null || subtotal == null) {
            return Collections.emptyList();
        }
        LocalDateTime now = LocalDateTime.now();
        BigDecimal currentShipping = shippingFee != null ? shippingFee : BigDecimal.ZERO;

        // 1. Get Customer Membership Tier
        MembershipTier customerTier = null;
        if (customerMemberships != null) {
            var memberOpt = customerMemberships.findByUserId(customerId);
            if (memberOpt.isPresent()) {
                customerTier = memberOpt.get().getTier();
            }
        }
        if (customerTier == null && membershipTiers != null) {
            customerTier = membershipTiers.findTopByOrderByMinSpentRequiredAsc();
        }

        // 2. Collect candidate coupons (Map by couponId to avoid duplicates)
        Map<Integer, Coupon> candidateMap = new LinkedHashMap<>();

        // 2a. Platform coupons
        if (coupons != null) {
            List<Coupon> platformCoupons = coupons.findByShopIsNullAndIsActiveTrue();
            if (platformCoupons != null) {
                for (Coupon c : platformCoupons) {
                    if (c != null && c.getCouponId() != null) candidateMap.put(c.getCouponId(), c);
                }
            }
            // 2b. Shop coupons
            if (shopId != null) {
                List<Coupon> shopCoupons = coupons.findByShop_ShopIdAndIsActiveTrue(shopId);
                if (shopCoupons != null) {
                    for (Coupon c : shopCoupons) {
                        if (c != null && c.getCouponId() != null) candidateMap.put(c.getCouponId(), c);
                    }
                }
            }
        }

        // 2c. Tier coupons for customer's tier
        if (tierCoupons != null && customerTier != null && customerTier.getTierId() != null) {
            List<TierCoupon> configuredList = tierCoupons.findByTier_TierId(customerTier.getTierId());
            if (configuredList != null) {
                for (TierCoupon tc : configuredList) {
                    Coupon c = tc.getCoupon();
                    if (c != null && Boolean.TRUE.equals(c.getIsActive()) && c.getCouponId() != null) {
                        candidateMap.put(c.getCouponId(), c);
                    }
                }
            }
        }

        // 3. Evaluate each candidate coupon
        List<AvailableCheckoutCouponResponse> result = new ArrayList<>();

        for (Coupon c : candidateMap.values()) {
            if (c == null || !Boolean.TRUE.equals(c.getIsActive())) continue;

            boolean eligible = true;
            String ineligibleReason = null;

            // Check validity period
            if (c.getStartDate() != null && now.isBefore(c.getStartDate())) {
                eligible = false;
                ineligibleReason = "Chưa đến thời gian sử dụng";
            } else if (c.getEndDate() != null && now.isAfter(c.getEndDate())) {
                eligible = false;
                ineligibleReason = "Mã giảm giá đã hết hạn";
            }

            // Check overall usage limit
            int usedCountAll = c.getUsedCount() == null ? 0 : c.getUsedCount();
            if (eligible && c.getUsageLimit() != null && usedCountAll >= c.getUsageLimit()) {
                eligible = false;
                ineligibleReason = "Mã đã hết lượt sử dụng toàn hệ thống";
            }

            // Check per-user usage / Tier quota
            int allowedLimit = defaultCouponLimitPerUser;
            List<TierCoupon> cTierCoupons = (tierCoupons != null) ? tierCoupons.findByCoupon_CouponId(c.getCouponId()) : List.of();
            if (!cTierCoupons.isEmpty()) {
                // Validate tier eligibility
                try {
                    CheckoutCouponRules.validateTierEligibility(c, customerTier, cTierCoupons);
                    allowedLimit = CheckoutCouponRules.resolveTierQuota(cTierCoupons, customerTier, defaultCouponLimitPerUser);
                } catch (IllegalArgumentException ex) {
                    eligible = false;
                    ineligibleReason = ex.getMessage();
                }
            }

            long userUsedCount = 0;
            if (orders != null && c.getCouponId() != null) {
                userUsedCount = orders.countValidUsedCouponByCustomer(customerId, c.getCouponId());
            }

            long remainingQuota = Math.max(0, allowedLimit - userUsedCount);
            String usageProgressText = "Còn lại " + remainingQuota + "/" + allowedLimit + " lượt";

            if (eligible && userUsedCount >= allowedLimit) {
                eligible = false;
                ineligibleReason = "Đã dùng hết (" + userUsedCount + "/" + allowedLimit + " lượt)";
                usageProgressText = "Đã dùng hết (" + userUsedCount + "/" + allowedLimit + " lượt)";
            }

            // Check minimum order value
            if (eligible && c.getMinOrderValue() != null && subtotal.compareTo(c.getMinOrderValue()) < 0) {
                eligible = false;
                BigDecimal diff = c.getMinOrderValue().subtract(subtotal);
                ineligibleReason = "Mua thêm " + formatCurrency(diff) + " ₫ để sử dụng";
            }

            // Calculate estimated discount
            BigDecimal estimatedDiscount = BigDecimal.ZERO;
            if (eligible) {
                try {
                    estimatedDiscount = OrderPricing.couponDiscount(c, subtotal, currentShipping);
                    if (estimatedDiscount.signum() <= 0) {
                        eligible = false;
                        ineligibleReason = (c.getCouponCategory() == CouponCategory.SHIPPING && currentShipping.signum() == 0)
                                ? "Đơn hàng này đang được miễn phí vận chuyển"
                                : "Mã không làm giảm số tiền đơn hàng này";
                    }
                } catch (Exception ex) {
                    eligible = false;
                    ineligibleReason = ex.getMessage();
                }
            }

            // Determine Scope Label
            String scopeLabel = "Toàn sàn";
            if (c.getShop() != null && c.getShop().getShopName() != null) {
                scopeLabel = "Shop: " + c.getShop().getShopName();
            } else if (!cTierCoupons.isEmpty()) {
                String tiers = cTierCoupons.stream()
                        .map(tc -> tc.getTier() != null ? tc.getTier().getTierName() : "")
                        .filter(s -> !s.isBlank())
                        .distinct()
                        .collect(Collectors.joining(", "));
                scopeLabel = tiers.isEmpty() ? "Đặc quyền Hội viên" : "Đặc quyền " + tiers;
            }

            // Determine Title
            String title;
            if (c.getCouponCategory() == CouponCategory.SHIPPING) {
                title = "Freeship " + (c.getDiscountType() == DiscountType.PERCENT
                        ? (c.getDiscountValue() != null ? c.getDiscountValue().stripTrailingZeros().toPlainString() : "0") + "%"
                        : formatCurrency(c.getDiscountValue()) + " ₫");
            } else {
                title = "Giảm " + (c.getDiscountType() == DiscountType.PERCENT
                        ? (c.getDiscountValue() != null ? c.getDiscountValue().stripTrailingZeros().toPlainString() : "0") + "%"
                        : formatCurrency(c.getDiscountValue()) + " ₫");
            }

            String badgeType = (c.getCouponCategory() == CouponCategory.SHIPPING) ? "FREESHIP" : "DISCOUNT";

            result.add(AvailableCheckoutCouponResponse.builder()
                    .couponId(c.getCouponId())
                    .code(c.getCouponCode())
                    .title(title)
                    .category(c.getCouponCategory())
                    .discountType(c.getDiscountType())
                    .discountValue(c.getDiscountValue())
                    .maxDiscountAmount(c.getMaxDiscountAmount())
                    .minOrderValue(c.getMinOrderValue())
                    .scopeLabel(scopeLabel)
                    .eligible(eligible)
                    .ineligibleReason(ineligibleReason)
                    .estimatedDiscount(estimatedDiscount)
                    .badgeType(badgeType)
                    .endDate(c.getEndDate())
                    .usageProgressText(usageProgressText)
                    .build());
        }

        // Sort: Eligible first, then higher estimated discount first, then by code
        result.sort((a, b) -> {
            if (a.isEligible() != b.isEligible()) {
                return a.isEligible() ? -1 : 1;
            }
            int cmp = b.getEstimatedDiscount().compareTo(a.getEstimatedDiscount());
            if (cmp != 0) return cmp;
            return a.getCode().compareToIgnoreCase(b.getCode());
        });

        return result;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format(Locale.GERMANY, "%,d", amount.longValue());
    }

    private List<CartItem> selectedItems(Integer customerId, Set<Integer> ids) {
        Cart cart = cartRepository.findByCustomer_UserId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Giỏ hàng đang trống."));
        return cartItems.findSummaryItems(cart.getCartId()).stream().filter(i -> ids.contains(i.getCartItemId())).toList();
    }

}
