package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.*;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutNotificationTest {
    @Mock CartService carts;
    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItems;
    @Mock UserRepository users;
    @Mock AddressRepository addresses;
    @Mock OrderRepository orders;
    @Mock OrderItemRepository orderItems;
    @Mock OrderStatusHistoryRepository histories;
    @Mock PaymentTransactionRepository payments;
    @Mock CheckoutCouponRepository coupons;
    @Mock NotificationService notifications;
    @InjectMocks CheckoutService service;

    @BeforeEach
    void setUp() {
        User customer = new User();
        customer.setUserId(10);
        Shop shop = new Shop();
        shop.setShopId(1);
        shop.setShopName("Fruit shop");
        Product product = new Product();
        product.setShop(shop);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setCostPrice(new BigDecimal("5000"));
        Cart cart = new Cart();
        cart.setCartId(3);
        CartItem item = new CartItem();
        item.setCartItemId(4);
        item.setVariant(variant);
        CartItemResponse line = new CartItemResponse(4, 2, "Orange", "1kg", 1, "Fruit shop",
                1, 10, new BigDecimal("10000"), new BigDecimal("10000"), true, null, null);
        when(carts.getSummary(10)).thenReturn(new CartSummaryResponse(List.of(line), 1, line.subtotal(), true));
        when(cartRepository.findByCustomer_UserId(10)).thenReturn(Optional.of(cart));
        when(cartItems.findSummaryItems(3)).thenReturn(List.of(item));
        when(users.findCartCustomerForUpdate(10)).thenReturn(Optional.of(customer));
        Address address = new Address();
        address.setAddressType(AddressType.SHIPPING);
        address.setReceiverName("Customer");
        address.setReceiverPhone("0900000000");
        address.setAddressLine("123 Street");
        address.setWard("Ward");
        address.setDistrict("District");
        address.setCity("City");
        when(addresses.findByUser_UserIdAndAddressIdAndIsActive(10, 5, true)).thenReturn(Optional.of(address));
        when(orders.save(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(99);
            return order;
        });
    }

    @Test
    void codCheckoutCreatesOrderAndAlertsOwnerInSameTransaction() {
        CheckoutResponse preview = service.preview(10, List.of(4));
        var placed = service.place(10, preview, 5, PaymentMethod.COD);
        assertEquals(99, placed.getFirst().orderId());
        ArgumentCaptor<Order> alert = ArgumentCaptor.forClass(Order.class);
        verify(notifications).notifyNewOrder(alert.capture());
        assertEquals(PaymentMethod.COD, alert.getValue().getPaymentMethod());
        assertEquals(99, alert.getValue().getOrderId());
        verify(payments).save(any());
    }

    @Test
    void vnpayCheckoutWaitsForSuccessfulPaymentBeforeAlertingOwner() {
        CheckoutResponse preview = service.preview(10, List.of(4));
        service.place(10, preview, 5, PaymentMethod.VNPAY);
        verifyNoInteractions(notifications);
        verifyNoInteractions(payments);
    }
}
