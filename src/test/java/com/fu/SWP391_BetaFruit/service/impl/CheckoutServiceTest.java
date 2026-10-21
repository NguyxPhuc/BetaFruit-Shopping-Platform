package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.*;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckoutServiceTest {
    CartService carts = mock(CartService.class);
    CartRepository cartRepository = mock(CartRepository.class);
    CartItemRepository cartItems = mock(CartItemRepository.class);
    UserRepository users = mock(UserRepository.class);
    AddressRepository addresses = mock(AddressRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    OrderItemRepository orderItems = mock(OrderItemRepository.class);
    OrderStatusHistoryRepository histories = mock(OrderStatusHistoryRepository.class);
    PaymentTransactionRepository payments = mock(PaymentTransactionRepository.class);
    CheckoutCouponRepository coupons = mock(CheckoutCouponRepository.class);
    CheckoutService service = new CheckoutService(carts, cartRepository, cartItems, users, addresses,
            orders, orderItems, histories, payments, coupons);
    User customer = new User();
    List<CartItem> entities = new ArrayList<>();
    List<CartItemResponse> lines = new ArrayList<>();

    @BeforeEach
    void setup() {
        customer.setUserId(7);
        Cart cart = new Cart(); cart.setCartId(8); cart.setCustomer(customer);
        when(cartRepository.findByCustomer_UserId(7)).thenReturn(Optional.of(cart));
        when(users.findCartCustomerForUpdate(7)).thenReturn(Optional.of(customer));
        when(cartItems.findSummaryItems(8)).thenAnswer(i -> entities);
        when(carts.getSummary(7)).thenAnswer(i -> new CartSummaryResponse(List.copyOf(lines), 3,
                new BigDecimal("300000"), true));
        addLine(1, 10, "Shop A", 2, "100000");
        addLine(2, 10, "Shop A", 1, "100000");
        Address address = new Address();
        address.setAddressType(AddressType.SHIPPING);
        address.setReceiverName("Nguyễn An"); address.setReceiverPhone("0901234567");
        address.setAddressLine("12 Lê Lợi"); address.setWard("Phường 1");
        address.setDistrict("Quận 1"); address.setCity("TP.HCM");
        when(addresses.findByUser_UserIdAndAddressIdAndIsActive(7, 9, true)).thenReturn(Optional.of(address));
        when(orders.save(any())).thenAnswer(i -> { Order o = i.getArgument(0); o.setOrderId(o.getShop().getShopId()); return o; });
    }

    void addLine(int id, int shopId, String shopName, int quantity, String price) {
        Shop shop = new Shop(); shop.setShopId(shopId); shop.setShopName(shopName); shop.setCommissionRate(new BigDecimal("5"));
        Product product = new Product(); product.setShop(shop);
        ProductVariant variant = new ProductVariant(); variant.setProduct(product); variant.setCostPrice(new BigDecimal("40000"));
        variant.setStockQuantity(10);
        CartItem item = new CartItem(); item.setCartItemId(id); item.setVariant(variant); item.setQuantity(quantity);
        entities.add(item);
        BigDecimal unitPrice = new BigDecimal(price);
        lines.add(new CartItemResponse(id, id, "Táo " + id, "1 kg", shopId, shopName, quantity, 10, unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(quantity)), true, null, null));
    }

    Coupon addCoupon(String code, CouponCategory category, boolean platform, String value) {
        Coupon c = new Coupon(); c.setCouponId(51); c.setCouponCode(code);
        c.setCouponCategory(category); c.setDiscountType(DiscountType.FIXED_AMOUNT);
        c.setDiscountValue(new BigDecimal(value)); c.setUsageLimit(10); c.setUsedCount(0);
        c.setIsActive(true); c.setStartDate(java.time.LocalDateTime.now().minusDays(1));
        c.setEndDate(java.time.LocalDateTime.now().plusDays(1));
        if (!platform) c.setShop(entities.getFirst().getVariant().getProduct().getShop());
        when(coupons.findByCouponCodeIgnoreCase(code)).thenReturn(Optional.of(c));
        when(coupons.findLockedById(51)).thenReturn(Optional.of(c));
        return c;
    }

    @Test void shopProductCouponReducesCustomerAndShopAmountsAndCodAndReservesOneUse() {
        var coupon = addCoupon("SHOP20", CouponCategory.PRODUCT, false, "20000");
        var quote = service.preview(7, List.of(1, 2), " SHOP20 ");
        assertEquals(0, quote.finalAmount().compareTo(new BigDecimal("280000")));
        assertEquals(0, coupon.getUsedCount()); // Preview does not spend a use.
        var placed = service.place(7, quote, 9);
        var saved = ArgumentCaptor.forClass(Order.class); verify(orders).save(saved.capture());
        var order = saved.getValue();
        assertSame(coupon, order.getCoupon());
        assertEquals(0, order.getShopNetReceived().compareTo(new BigDecimal("265000")));
        assertEquals(order.getShopNetReceived(), OrderPricing.calculate(order).shopNetReceived());
        assertEquals(0, placed.getFirst().finalAmount().compareTo(new BigDecimal("280000")));
        var payment = ArgumentCaptor.forClass(PaymentTransaction.class); verify(payments).save(payment.capture());
        assertEquals(order.getFinalAmount(), payment.getValue().getAmount());
        assertEquals(1, coupon.getUsedCount());
        verify(coupons).findLockedById(51);
    }

    @Test void adminProductCouponDoesNotReduceShopReceiptsAndVnpayUsesDiscountedStoredTotal() {
        addCoupon("ADMIN20", CouponCategory.PRODUCT, true, "20000");
        var quote = service.preview(7, List.of(1, 2), "ADMIN20");
        service.place(7, quote, 9, PaymentMethod.VNPAY);
        var saved = ArgumentCaptor.forClass(Order.class); verify(orders).save(saved.capture());
        var order = saved.getValue();
        assertEquals(0, order.getFinalAmount().compareTo(new BigDecimal("280000")));
        assertEquals(0, order.getShopNetReceived().compareTo(new BigDecimal("285000")));
        assertEquals(order.getShopNetReceived(), OrderPricing.calculate(order).shopNetReceived());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
        verifyNoInteractions(payments);
    }

    @Test void shippingCouponSubtractsOnceAndOnlyShopFundedShippingReducesShopReceipts() {
        ReflectionTestUtils.setField(service, "shippingFee", new BigDecimal("15000"));
        var coupon = addCoupon("SHIP", CouponCategory.SHIPPING, false, "50000");
        var shopQuote = service.preview(7, List.of(1, 2), "SHIP");
        assertEquals(0, shopQuote.discountAmount().compareTo(new BigDecimal("15000")));
        assertEquals(0, shopQuote.finalAmount().compareTo(new BigDecimal("300000")));
        service.place(7, shopQuote, 9);
        var saved = ArgumentCaptor.forClass(Order.class); verify(orders).save(saved.capture());
        var order = saved.getValue();
        assertEquals(0, order.getShippingFee().compareTo(new BigDecimal("15000")));
        assertEquals(0, order.getShopNetReceived().compareTo(new BigDecimal("270000")));
        assertEquals(0, OrderPaymentRules.codAmount(order).compareTo(new BigDecimal("300000")));
        coupon.setShop(null);
        var adminPricing = OrderPricing.calculate(order);
        assertEquals(0, adminPricing.shopNetReceived().compareTo(new BigDecimal("285000")));
    }

    @Test void otherShopCouponAndZeroShippingBenefitAreRejectedWithoutWrites() {
        var coupon = addCoupon("OTHER", CouponCategory.PRODUCT, false, "20000");
        Shop foreign = new Shop(); foreign.setShopId(99); coupon.setShop(foreign);
        assertThrows(IllegalArgumentException.class, () -> service.preview(7, List.of(1), "OTHER"));
        coupon.setShop(null); coupon.setCouponCategory(CouponCategory.SHIPPING);
        assertThrows(IllegalArgumentException.class, () -> service.preview(7, List.of(1), "OTHER"));
        verifyNoInteractions(orders, orderItems, histories, payments);
    }

    @Test void adminShippingCouponWorksForAnotherShopAndKeepsItsNetReceipts() {
        ReflectionTestUtils.setField(service, "shippingFee", new BigDecimal("15000"));
        entities.forEach(item -> item.getVariant().getProduct().getShop().setShopId(20));
        var coupon = addCoupon("GLOBALSHIP", CouponCategory.SHIPPING, true, "15000");
        var quote = service.preview(7, List.of(1, 2), "GLOBALSHIP");
        service.place(7, quote, 9);
        var saved = ArgumentCaptor.forClass(Order.class); verify(orders).save(saved.capture());
        var order = saved.getValue();
        assertEquals(20, order.getShop().getShopId());
        assertEquals(0, order.getFinalAmount().compareTo(new BigDecimal("300000")));
        assertEquals(0, order.getShopNetReceived().compareTo(new BigDecimal("285000")));
        assertEquals(order.getShopNetReceived(), OrderPricing.calculate(order).shopNetReceived());
        assertEquals(1, coupon.getUsedCount());
    }

    @Test void shopCannotSponsorShippingBeyondItsMerchandiseReceipts() {
        ReflectionTestUtils.setField(service, "shippingFee", new BigDecimal("500000"));
        addCoupon("BIGSHIP", CouponCategory.SHIPPING, false, "500000");
        var error = assertThrows(IllegalArgumentException.class,
                () -> service.preview(7, List.of(1, 2), "BIGSHIP"));
        assertTrue(error.getMessage().contains("tài trợ"));
        verifyNoInteractions(orders, orderItems, histories, payments);
    }

    @Test void fullySponsoredOrderCanUseCodWithoutOpeningZeroAmountVnpayPayment() {
        var coupon = addCoupon("FREE", CouponCategory.PRODUCT, true, "300000");
        var quote = service.preview(7, List.of(1, 2), "FREE");
        assertEquals(0, quote.finalAmount().signum());
        assertThrows(IllegalArgumentException.class, () -> service.place(7, quote, 9, PaymentMethod.VNPAY));
        verifyNoInteractions(orders, payments);
        assertEquals(0, coupon.getUsedCount());
        service.place(7, quote, 9, PaymentMethod.COD);
        var saved = ArgumentCaptor.forClass(Order.class); verify(orders).save(saved.capture());
        assertEquals(0, saved.getValue().getFinalAmount().signum());
        assertEquals(0, saved.getValue().getShopNetReceived().compareTo(new BigDecimal("285000")));
        assertEquals(1, coupon.getUsedCount());
    }

    @Test void exhaustedOrChangedCouponIsRejectedAgainAtPlacement() {
        var coupon = addCoupon("LIMIT", CouponCategory.PRODUCT, false, "20000");
        var quote = service.preview(7, List.of(1, 2), "LIMIT");
        coupon.setUsedCount(10);
        assertThrows(IllegalArgumentException.class, () -> service.place(7, quote, 9));
        coupon.setUsedCount(0); coupon.setDiscountValue(new BigDecimal("10000"));
        assertThrows(IllegalArgumentException.class, () -> service.place(7, quote, 9));
        verifyNoInteractions(orders, orderItems, histories, payments);
        verify(cartItems, never()).deleteAll(any());
        assertEquals(0, coupon.getUsedCount());
    }

    @Test void invalidAddressDoesNotConsumeCouponAndNegativeShopNetIsRejected() {
        var coupon = addCoupon("NET", CouponCategory.PRODUCT, false, "20000");
        var quote = service.preview(7, List.of(1), "NET");
        assertThrows(IllegalArgumentException.class, () -> service.place(7, quote, 999));
        assertEquals(0, coupon.getUsedCount());
        coupon.setDiscountValue(new BigDecimal("200000"));
        assertThrows(IllegalArgumentException.class, () -> service.preview(7, List.of(1), "NET"));
        verifyNoInteractions(orders, orderItems, histories, payments);
    }

    @Test void sameShopLinesCreateOneQuoteWithOneShippingFee() {
        ReflectionTestUtils.setField(service, "shippingFee", new BigDecimal("15000"));
        var quote = service.preview(7, List.of(1, 2));
        assertEquals(1, quote.shops().size());
        assertEquals(0, new BigDecimal("315000").compareTo(quote.finalAmount()));
        assertEquals(List.of(1), service.preview(7, List.of(1, 1)).itemIds());
    }

    @Test void emptyAndForeignSelectionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.preview(7, List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.preview(7, List.of(999)));
        verifyNoInteractions(orders);
    }

    @Test void unavailableItemCannotBeOrdered() {
        var line = lines.getFirst();
        lines.set(0, new CartItemResponse(1, 1, line.productName(), line.variantName(), line.shopId(), line.shopName(),
                2, 0, line.unitPrice(), BigDecimal.ZERO, false, "Hết hàng", line.imageUrl()));
        assertThrows(IllegalArgumentException.class, () -> service.preview(7, List.of(1)));
    }

    @Test void createsPendingCodOrdersWithSnapshotsAndKeepsStockForShopConfirmation() {
        var quote = service.preview(7, List.of(1, 2));
        var placed = service.place(7, quote, 9);
        assertEquals(1, placed.size());
        var saved = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(saved.capture());
        var order = saved.getAllValues().getFirst();
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
        assertEquals(PaymentMethod.COD, order.getPaymentMethod());
        assertTrue(order.getSnapshotShippingAddress().contains("0901234567"));
        assertEquals(0, new BigDecimal("15000").compareTo(order.getPlatformFee()));
        assertEquals(0, new BigDecimal("285000").compareTo(order.getShopNetReceived()));
        var item = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItems, times(2)).save(item.capture());
        assertEquals("Táo 1", item.getAllValues().getFirst().getSnapshotProductName());
        assertEquals(new BigDecimal("100000"), item.getAllValues().getFirst().getPriceSnapshot());
        assertEquals(10, entities.getFirst().getVariant().getStockQuantity());
        verify(histories).save(any()); verify(payments).save(any());
        verify(cartItems).deleteAll(entities);
    }

    @Test void unselectedLinesStayInCart() {
        service.place(7, service.preview(7, List.of(2)), 9);
        verify(cartItems).deleteAll(List.of(entities.get(1)));
        verify(orders).save(any());
    }

    @Test void vnpayCreatesPendingOrdersWithoutCodTransactionsOrStockDeduction() {
        var quote = service.preview(7, List.of(1, 2));
        var placed = service.place(7, quote, 9, PaymentMethod.VNPAY);
        assertEquals(1, placed.size());
        var saved = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(saved.capture());
        for (Order order : saved.getAllValues()) {
            assertEquals(PaymentMethod.VNPAY, order.getPaymentMethod());
            assertEquals(OrderStatus.PENDING, order.getOrderStatus());
        }
        assertEquals(10, entities.getFirst().getVariant().getStockQuantity());
        verifyNoInteractions(payments);
        verify(cartItems).deleteAll(entities);
    }

    @Test void changedPriceRequiresReviewBeforeCreatingOrders() {
        var quote = service.preview(7, List.of(1));
        lines.clear(); entities.clear(); addLine(1, 10, "Shop A", 2, "110000");
        assertThrows(IllegalArgumentException.class, () -> service.place(7, quote, 9));
        verifyNoInteractions(orders, orderItems, histories, payments);
        verify(cartItems, never()).deleteAll(any());
    }

    @Test void differentShopIdsAreRejectedEvenWithIdenticalShopNames() {
        entities.get(1).getVariant().getProduct().getShop().setShopId(20);
        var error = assertThrows(IllegalArgumentException.class, () -> service.preview(7, List.of(1, 2)));
        assertTrue(error.getMessage().contains("cùng một cửa hàng"));
        verifyNoInteractions(orders, orderItems, histories, payments);
        verify(cartItems, never()).deleteAll(any());
    }

    @Test void staleReviewedCartCannotCreateMultiShopOrdersForCodOrVnpay() {
        var reviewed = service.preview(7, List.of(1, 2));
        entities.get(1).getVariant().getProduct().getShop().setShopId(20);
        for (PaymentMethod method : PaymentMethod.values()) {
            assertThrows(IllegalArgumentException.class, () -> service.place(7, reviewed, 9, method));
        }
        verifyNoInteractions(orders, orderItems, histories, payments);
        verify(cartItems, never()).deleteAll(any());
    }

    @Test void inactiveForeignOrPickupAddressCannotBeUsed() {
        var quote = service.preview(7, List.of(1));
        assertThrows(IllegalArgumentException.class, () -> service.place(7, quote, 999));
        assertThrows(IllegalArgumentException.class, () -> service.place(7, quote, null));
        Address pickup = new Address(); pickup.setAddressType(AddressType.PICKUP);
        when(addresses.findByUser_UserIdAndAddressIdAndIsActive(7, 9, true)).thenReturn(Optional.of(pickup));
        assertThrows(IllegalArgumentException.class, () -> service.place(7, quote, 9));
        verifyNoInteractions(orders);
    }

    @Test void reusedCartLinesCannotCreateAnotherOrder() {
        var quote = service.preview(7, List.of(1));
        service.place(7, quote, 9);
        lines.removeFirst(); entities.removeFirst();
        assertThrows(IllegalArgumentException.class, () -> service.place(7, quote, 9));
        verify(orders).save(any());
    }
}
