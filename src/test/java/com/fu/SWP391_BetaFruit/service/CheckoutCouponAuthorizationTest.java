package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.CartItemResponse;
import com.fu.SWP391_BetaFruit.dto.response.CartSummaryResponse;
import com.fu.SWP391_BetaFruit.dto.response.CheckoutResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Kiểm thử Nghiệp vụ: Phân quyền & Chống lạm dụng Coupon khi Checkout (Không sửa Database)")
class CheckoutCouponAuthorizationTest {

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
    @Mock TierCouponRepository tierCoupons;
    @Mock CustomerMembershipRepository customerMemberships;
    @Mock MembershipTierRepository membershipTiers;

    @InjectMocks
    CheckoutService checkoutService;

    private User customer;
    private User shopOwner;
    private Shop shop;
    private MembershipTier bronzeTier;
    private MembershipTier silverTier;
    private MembershipTier goldTier;
    private MembershipTier diamondTier;
    private CartItem cartItem;
    private CartItemResponse cartItemResponse;
    private Coupon publicCoupon;
    private Coupon goldVipCoupon;
    private Coupon shopCoupon;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setUserId(10);
        customer.setFullName("Nguyen Van Khach");

        shopOwner = new User();
        shopOwner.setUserId(99);
        shopOwner.setFullName("Chu Cua Hang");

        shop = new Shop();
        shop.setShopId(1);
        shop.setShopName("Beta Fruit Shop");
        shop.setOwner(shopOwner);

        Product product = new Product();
        product.setProductId(1);
        product.setShop(shop);

        ProductVariant variant = new ProductVariant();
        variant.setVariantId(1);
        variant.setProduct(product);
        variant.setCostPrice(new BigDecimal("50000"));

        Cart cart = new Cart();
        cart.setCartId(100);
        cart.setCustomer(customer);

        cartItem = new CartItem();
        cartItem.setCartItemId(5);
        cartItem.setCart(cart);
        cartItem.setVariant(variant);

        cartItemResponse = new CartItemResponse(
                5, 1, "Cam Sành", "1kg", 2, "Beta Fruit Shop",
                1, 10, new BigDecimal("100000"), new BigDecimal("200000"), true, null, null
        );

        when(carts.getSummary(customer.getUserId()))
                .thenReturn(new CartSummaryResponse(List.of(cartItemResponse), 2, new BigDecimal("200000"), true));
        when(cartRepository.findByCustomer_UserId(customer.getUserId()))
                .thenReturn(Optional.of(cart));
        when(cartItems.findSummaryItems(cart.getCartId()))
                .thenReturn(List.of(cartItem));

        bronzeTier = new MembershipTier(1, "Hạng Đồng", BigDecimal.ZERO, 0);
        silverTier = new MembershipTier(2, "Hạng Bạc", new BigDecimal("2000000"), 3);
        goldTier = new MembershipTier(3, "Hạng Vàng", new BigDecimal("5000000"), 10);
        diamondTier = new MembershipTier(4, "Hạng Kim Cương", new BigDecimal("10000000"), 20);

        // Coupon 1: Public toàn sàn (không gán vào TierCoupon nào)
        publicCoupon = new Coupon();
        publicCoupon.setCouponId(201);
        publicCoupon.setCouponCode("BETAPUBLIC");
        publicCoupon.setCouponCategory(CouponCategory.PRODUCT);
        publicCoupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        publicCoupon.setDiscountValue(new BigDecimal("20000"));
        publicCoupon.setIsActive(true);
        publicCoupon.setStartDate(LocalDateTime.now().minusDays(1));
        publicCoupon.setEndDate(LocalDateTime.now().plusDays(10));
        publicCoupon.setUsageLimit(1000);
        publicCoupon.setUsedCount(5);

        // Coupon 2: Gói đặc quyền Hạng Vàng (GOLD_VIP)
        goldVipCoupon = new Coupon();
        goldVipCoupon.setCouponId(202);
        goldVipCoupon.setCouponCode("GOLD_VIP");
        goldVipCoupon.setCouponCategory(CouponCategory.PRODUCT);
        goldVipCoupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        goldVipCoupon.setDiscountValue(new BigDecimal("50000"));
        goldVipCoupon.setIsActive(true);
        goldVipCoupon.setStartDate(LocalDateTime.now().minusDays(1));
        goldVipCoupon.setEndDate(LocalDateTime.now().plusDays(10));
        goldVipCoupon.setUsageLimit(500);
        goldVipCoupon.setUsedCount(10);

        // Coupon 3: Mã riêng của Shop
        shopCoupon = new Coupon();
        shopCoupon.setCouponId(203);
        shopCoupon.setCouponCode("SHOP_FRUIT_20");
        shopCoupon.setShop(shop);
        shopCoupon.setCouponCategory(CouponCategory.PRODUCT);
        shopCoupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        shopCoupon.setDiscountValue(new BigDecimal("20000"));
        shopCoupon.setIsActive(true);
        shopCoupon.setStartDate(LocalDateTime.now().minusDays(1));
        shopCoupon.setEndDate(LocalDateTime.now().plusDays(10));
        shopCoupon.setUsageLimit(200);
        shopCoupon.setUsedCount(2);
    }

    @Test
    @DisplayName("TC_01: Mã công khai (Public) - Bất kỳ khách hàng nào cũng được áp dụng thành công")
    void testPublicCouponCanBeUsedByAnyone() {
        when(coupons.findByCouponCodeIgnoreCase("BETAPUBLIC")).thenReturn(Optional.of(publicCoupon));
        when(tierCoupons.findByCoupon_CouponId(201)).thenReturn(List.of()); // Không bị ràng buộc Tier
        when(orders.countValidUsedCouponByCustomer(10, 201)).thenReturn(0L); // Chưa dùng lần nào

        CheckoutResponse response = checkoutService.preview(10, List.of(5), "BETAPUBLIC");

        assertNotNull(response);
        assertNotNull(response.coupon());
        assertEquals("BETAPUBLIC", response.coupon().code());
        assertEquals(new BigDecimal("20000"), response.coupon().discountAmount());
    }

    @Test
    @DisplayName("TC_02: Hội viên Hạng Đồng (Bronze) cố dùng mã Hạng Vàng (GOLD_VIP) -> Bị chặn với thông báo rõ ràng")
    void testBronzeMemberCannotUseGoldVipCoupon() {
        when(coupons.findByCouponCodeIgnoreCase("GOLD_VIP")).thenReturn(Optional.of(goldVipCoupon));
        when(tierCoupons.findByCoupon_CouponId(202)).thenReturn(List.of(new TierCoupon(goldTier, goldVipCoupon)));

        CustomerMembership bronzeMembership = new CustomerMembership(10, customer, BigDecimal.ZERO, bronzeTier);
        when(customerMemberships.findByUserId(10)).thenReturn(Optional.of(bronzeMembership));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                checkoutService.preview(10, List.of(5), "GOLD_VIP")
        );

        assertTrue(ex.getMessage().contains("chỉ dành riêng cho hội viên [Hạng Vàng] trở lên"));
        assertTrue(ex.getMessage().contains("Hạng hiện tại của bạn là [Hạng Đồng]"));
    }

    @Test
    @DisplayName("TC_03: Hội viên Hạng Vàng (Gold) dùng mã Hạng Vàng (GOLD_VIP) -> Áp dụng thành công")
    void testGoldMemberCanUseGoldVipCoupon() {
        when(coupons.findByCouponCodeIgnoreCase("GOLD_VIP")).thenReturn(Optional.of(goldVipCoupon));
        when(tierCoupons.findByCoupon_CouponId(202)).thenReturn(List.of(new TierCoupon(goldTier, goldVipCoupon)));
        when(orders.countValidUsedCouponByCustomer(10, 202)).thenReturn(0L);

        CustomerMembership goldMembership = new CustomerMembership(10, customer, new BigDecimal("6000000"), goldTier);
        when(customerMemberships.findByUserId(10)).thenReturn(Optional.of(goldMembership));

        CheckoutResponse response = checkoutService.preview(10, List.of(5), "GOLD_VIP");

        assertNotNull(response);
        assertNotNull(response.coupon());
        assertEquals("GOLD_VIP", response.coupon().code());
        assertEquals(new BigDecimal("50000"), response.coupon().discountAmount());
    }

    @Test
    @DisplayName("TC_04: Hội viên Hạng Kim Cương (Diamond - hạng cao hơn) dùng mã Hạng Vàng -> Được hưởng quyền lợi lũy tiến")
    void testDiamondMemberCanUseGoldTierCoupon() {
        when(coupons.findByCouponCodeIgnoreCase("GOLD_VIP")).thenReturn(Optional.of(goldVipCoupon));
        when(tierCoupons.findByCoupon_CouponId(202)).thenReturn(List.of(new TierCoupon(goldTier, goldVipCoupon)));
        when(orders.countValidUsedCouponByCustomer(10, 202)).thenReturn(0L);

        CustomerMembership diamondMembership = new CustomerMembership(10, customer, new BigDecimal("12000000"), diamondTier);
        when(customerMemberships.findByUserId(10)).thenReturn(Optional.of(diamondMembership));

        CheckoutResponse response = checkoutService.preview(10, List.of(5), "GOLD_VIP");

        assertNotNull(response);
        assertNotNull(response.coupon());
        assertEquals("GOLD_VIP", response.coupon().code());
    }

    @Test
    @DisplayName("TC_05: Giới hạn 1 lần/tài khoản - Khách đã có đơn hàng hợp lệ trước đó dùng mã này -> Bị chặn")
    void testUserCannotExceedPerUserUsageLimit() {
        when(coupons.findByCouponCodeIgnoreCase("BETAPUBLIC")).thenReturn(Optional.of(publicCoupon));
        when(tierCoupons.findByCoupon_CouponId(201)).thenReturn(List.of());
        // Giả lập tài khoản 10 đã có 1 đơn hợp lệ dùng mã này
        when(orders.countValidUsedCouponByCustomer(10, 201)).thenReturn(1L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                checkoutService.preview(10, List.of(5), "BETAPUBLIC")
        );

        assertTrue(ex.getMessage().contains("Bạn đã sử dụng mã giảm giá này rồi"));
        assertTrue(ex.getMessage().contains("tối đa 1 lần"));
    }

    @Test
    @DisplayName("TC_06: Hoàn lượt dùng khi đơn bị hủy - Nếu đơn trước bị CANCELLED/REJECTED -> countValid = 0 -> Dùng lại được")
    void testUserCanReuseCouponIfPreviousOrderWasCancelled() {
        when(coupons.findByCouponCodeIgnoreCase("BETAPUBLIC")).thenReturn(Optional.of(publicCoupon));
        when(tierCoupons.findByCoupon_CouponId(201)).thenReturn(List.of());
        // Đơn cũ đã bị HỦY nên query countValid loại trừ, trả về 0
        when(orders.countValidUsedCouponByCustomer(10, 201)).thenReturn(0L);

        CheckoutResponse response = checkoutService.preview(10, List.of(5), "BETAPUBLIC");

        assertNotNull(response);
        assertEquals("BETAPUBLIC", response.coupon().code());
    }

    @Test
    @DisplayName("TC_07: Chống gian lận - Chủ Shop (OwnerId = 99) dùng mã của chính Shop mình -> Bị chặn ngay")
    void testShopOwnerCannotUseOwnShopCoupon() {
        when(coupons.findByCouponCodeIgnoreCase("SHOP_FRUIT_20")).thenReturn(Optional.of(shopCoupon));

        // Khách hàng đang checkout chính là chủ sở hữu của shop (UserId = 99)
        when(carts.getSummary(99))
                .thenReturn(new CartSummaryResponse(List.of(cartItemResponse), 2, new BigDecimal("200000"), true));
        when(cartRepository.findByCustomer_UserId(99))
                .thenReturn(Optional.of(cartItem.getCart()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                checkoutService.preview(99, List.of(5), "SHOP_FRUIT_20")
        );

        assertTrue(ex.getMessage().contains("Chủ cửa hàng không thể sử dụng mã giảm giá của chính cửa hàng mình"));
    }
}
