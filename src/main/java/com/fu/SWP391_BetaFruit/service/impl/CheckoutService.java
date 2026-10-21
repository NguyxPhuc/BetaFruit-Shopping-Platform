package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.customer.CartItemResponse;
import com.fu.SWP391_BetaFruit.dto.response.customer.CheckoutResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Value("${checkout.shipping-fee:0}")
    private BigDecimal shippingFee = BigDecimal.ZERO;
    @Value("${checkout.default-commission-rate:0}")
    private BigDecimal defaultCommissionRate = BigDecimal.ZERO;

    public CheckoutResponse preview(Integer customerId, List<Integer> selectedIds) {
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
        BigDecimal fee = money(shippingFee);
        List<CheckoutResponse.ShopGroup> shops = new ArrayList<>();
        for (var group : groups.entrySet()) {
            var items = group.getValue().stream().map(i -> byId.get(i.getCartItemId())).toList();
            var total = items.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            shops.add(new CheckoutResponse.ShopGroup(group.getKey(),
                    group.getValue().getFirst().getVariant().getProduct().getShop().getShopName(),
                    items, total, fee, total.add(fee)));
        }
        BigDecimal subtotal = lines.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = fee.multiply(BigDecimal.valueOf(shops.size()));
        return new CheckoutResponse(List.copyOf(shops), lines.stream().map(CartItemResponse::itemId).toList(),
                lines.stream().mapToLong(CartItemResponse::quantity).sum(), subtotal, shipping, subtotal.add(shipping));
    }

    public List<Address> shippingAddresses(Integer customerId) {
        carts.getSummary(customerId);
        return addresses.findByUser_UserIdAndAddressTypeAndIsActiveTrue(customerId, AddressType.SHIPPING)
                .stream().sorted(Comparator.comparing(a -> !Boolean.TRUE.equals(a.getIsDefault()))).toList();
    }

    @Transactional
    public List<CheckoutResponse.PlacedOrder> place(Integer customerId, CheckoutResponse reviewed, Integer addressId) {
        // Cart mutations take this same lock. Two tabs cannot consume the same cart lines twice.
        User customer = users.findCartCustomerForUpdate(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại."));
        var current = preview(customerId, reviewed.itemIds());
        if (!current.equals(reviewed))
            throw new IllegalArgumentException("Giá, số lượng hoặc tồn kho đã thay đổi. Vui lòng kiểm tra lại checkout.");
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
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setShippingFee(group.shippingFee());
            order.setFinalAmount(group.finalAmount());
            BigDecimal rate = shop.getCommissionRate() == null ? defaultCommissionRate : shop.getCommissionRate();
            if (rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0)
                throw new IllegalArgumentException("Tỷ lệ phí sàn của cửa hàng không hợp lệ.");
            order.setPlatformFee(group.subtotal().multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_UP));
            order.setShopNetReceived(group.subtotal().subtract(order.getPlatformFee()));
            order.setPaymentMethod(PaymentMethod.COD);
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
                    "Khách hàng đặt hàng từ giỏ hàng (COD)", null));
            PaymentTransaction payment = new PaymentTransaction();
            payment.setOrder(order);
            payment.setGateway("COD");
            payment.setTransactionRef("COD-" + UUID.randomUUID());
            payment.setAmount(group.finalAmount());
            payment.setStatus(PaymentStatus.PENDING);
            payments.save(payment);
            result.add(new CheckoutResponse.PlacedOrder(order.getOrderId(), shop.getShopName(), group.finalAmount()));
        }
        // Remove only purchased lines, after all orders were created in this transaction.
        cartItems.deleteAll(selected);
        return List.copyOf(result);
    }

    private List<CartItem> selectedItems(Integer customerId, Set<Integer> ids) {
        Cart cart = cartRepository.findByCustomer_UserId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Giỏ hàng đang trống."));
        return cartItems.findSummaryItems(cart.getCartId()).stream().filter(i -> ids.contains(i.getCartItemId())).toList();
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.scale() > 2)
            throw new IllegalArgumentException("Phí vận chuyển chưa được cấu hình hợp lệ.");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
