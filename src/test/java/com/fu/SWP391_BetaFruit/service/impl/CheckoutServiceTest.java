package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.customer.*;
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
    CheckoutService service = new CheckoutService(carts, cartRepository, cartItems, users, addresses,
            orders, orderItems, histories, payments);
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
        addLine(2, 20, "Shop B", 1, "100000");
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
        lines.add(new CartItemResponse(id, id, "Táo " + id, "1 kg", shopName, quantity, 10, unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(quantity)), true, null));
    }

    @Test void selectedLinesAreGroupedAndShippingChargedPerShop() {
        ReflectionTestUtils.setField(service, "shippingFee", new BigDecimal("15000"));
        var quote = service.preview(7, List.of(1, 2));
        assertEquals(2, quote.shops().size());
        assertEquals(0, new BigDecimal("330000").compareTo(quote.finalAmount()));
        assertEquals(List.of(1), service.preview(7, List.of(1, 1)).itemIds());
    }

    @Test void emptyAndForeignSelectionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.preview(7, List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.preview(7, List.of(999)));
        verifyNoInteractions(orders);
    }

    @Test void unavailableItemCannotBeOrdered() {
        var line = lines.getFirst();
        lines.set(0, new CartItemResponse(1, 1, line.productName(), line.variantName(), line.shopName(),
                2, 0, line.unitPrice(), BigDecimal.ZERO, false, "Hết hàng"));
        assertThrows(IllegalArgumentException.class, () -> service.preview(7, List.of(1)));
    }

    @Test void createsPendingCodOrdersWithSnapshotsAndKeepsStockForShopConfirmation() {
        var quote = service.preview(7, List.of(1, 2));
        var placed = service.place(7, quote, 9);
        assertEquals(2, placed.size());
        var saved = ArgumentCaptor.forClass(Order.class);
        verify(orders, times(2)).save(saved.capture());
        var order = saved.getAllValues().getFirst();
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
        assertEquals(PaymentMethod.COD, order.getPaymentMethod());
        assertTrue(order.getSnapshotShippingAddress().contains("0901234567"));
        assertEquals(0, new BigDecimal("10000").compareTo(order.getPlatformFee()));
        assertEquals(0, new BigDecimal("190000").compareTo(order.getShopNetReceived()));
        var item = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItems, times(2)).save(item.capture());
        assertEquals("Táo 1", item.getAllValues().getFirst().getSnapshotProductName());
        assertEquals(new BigDecimal("100000"), item.getAllValues().getFirst().getPriceSnapshot());
        assertEquals(10, entities.getFirst().getVariant().getStockQuantity());
        verify(histories, times(2)).save(any()); verify(payments, times(2)).save(any());
        verify(cartItems).deleteAll(entities);
    }

    @Test void unselectedLinesStayInCart() {
        service.place(7, service.preview(7, List.of(2)), 9);
        verify(cartItems).deleteAll(List.of(entities.get(1)));
        verify(orders).save(any());
    }

    @Test void changedPriceRequiresReviewBeforeCreatingOrders() {
        var quote = service.preview(7, List.of(1));
        lines.clear(); entities.clear(); addLine(1, 10, "Shop A", 2, "110000");
        assertThrows(IllegalArgumentException.class, () -> service.place(7, quote, 9));
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
