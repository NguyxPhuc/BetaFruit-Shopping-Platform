package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.AvailableCheckoutCouponResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.CartService;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutAvailableCouponsTest {

    @Mock private CartService carts;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItems;
    @Mock private UserRepository users;
    @Mock private AddressRepository addresses;
    @Mock private OrderRepository orders;
    @Mock private OrderItemRepository orderItems;
    @Mock private OrderStatusHistoryRepository histories;
    @Mock private PaymentTransactionRepository payments;
    @Mock private CheckoutCouponRepository coupons;
    @Mock private NotificationService notifications;
    @Mock private TierCouponRepository tierCoupons;
    @Mock private CustomerMembershipRepository customerMemberships;
    @Mock private MembershipTierRepository membershipTiers;

    @InjectMocks
    private CheckoutService checkoutService;

    private MembershipTier silverTier;
    private Coupon freeshipCoupon;
    private Coupon discountCoupon;

    @BeforeEach
    void setUp() {
        silverTier = new MembershipTier();
        silverTier.setTierId(2);
        silverTier.setTierName("Hạng Bạc");
        silverTier.setMinSpentRequired(new BigDecimal("1000000"));

        freeshipCoupon = new Coupon();
        freeshipCoupon.setCouponId(101);
        freeshipCoupon.setCouponCode("FREESHIP30K");
        freeshipCoupon.setCouponCategory(CouponCategory.SHIPPING);
        freeshipCoupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        freeshipCoupon.setDiscountValue(new BigDecimal("30000"));
        freeshipCoupon.setMinOrderValue(new BigDecimal("150000"));
        freeshipCoupon.setIsActive(true);
        freeshipCoupon.setStartDate(LocalDateTime.now().minusDays(1));
        freeshipCoupon.setEndDate(LocalDateTime.now().plusDays(10));

        discountCoupon = new Coupon();
        discountCoupon.setCouponId(102);
        discountCoupon.setCouponCode("BETAFRUIT10");
        discountCoupon.setCouponCategory(CouponCategory.PRODUCT);
        discountCoupon.setDiscountType(DiscountType.PERCENT);
        discountCoupon.setDiscountValue(new BigDecimal("10"));
        discountCoupon.setMaxDiscountAmount(new BigDecimal("50000"));
        discountCoupon.setMinOrderValue(new BigDecimal("200000"));
        discountCoupon.setIsActive(true);
        discountCoupon.setStartDate(LocalDateTime.now().minusDays(1));
        discountCoupon.setEndDate(LocalDateTime.now().plusDays(10));
    }

    @Test
    @DisplayName("Lấy danh sách mã khả dụng - Phân loại đúng mã đủ điều kiện và chưa đủ điều kiện theo đơn tối thiểu")
    void testGetAvailableCoupons_MinOrderEvaluation() {
        CustomerMembership membership = new CustomerMembership();
        membership.setTier(silverTier);
        when(customerMemberships.findByUserId(10)).thenReturn(Optional.of(membership));

        when(coupons.findByShopIsNullAndIsActiveTrue()).thenReturn(List.of(freeshipCoupon, discountCoupon));
        when(coupons.findByShop_ShopIdAndIsActiveTrue(anyInt())).thenReturn(List.of());
        when(tierCoupons.findByTier_TierId(2)).thenReturn(List.of());

        // Đơn hàng subtotal 160.000 (đủ cho FREESHIP30K: 150k, nhưng thiếu cho BETAFRUIT10: 200k)
        BigDecimal subtotal = new BigDecimal("160000");
        BigDecimal shippingFee = new BigDecimal("30000");

        List<AvailableCheckoutCouponResponse> result = checkoutService.getAvailableCouponsForCustomer(
                10, 1, subtotal, shippingFee);

        assertNotNull(result);
        assertEquals(2, result.size());

        // Mã đủ điều kiện phải xếp trước
        AvailableCheckoutCouponResponse first = result.get(0);
        assertEquals("FREESHIP30K", first.getCode());
        assertTrue(first.isEligible(), "FREESHIP30K phải đủ điều kiện vì subtotal 160k > minOrder 150k");

        // Mã chưa đủ điều kiện phải xếp sau kèm lý do mua thêm
        AvailableCheckoutCouponResponse second = result.get(1);
        assertEquals("BETAFRUIT10", second.getCode());
        assertFalse(second.isEligible(), "BETAFRUIT10 chưa đủ điều kiện vì subtotal 160k < minOrder 200k");
        assertTrue(second.getIneligibleReason().contains("Mua thêm"), "Phải có thông báo mua thêm tiền");
    }

    @Test
    @DisplayName("Lấy danh sách mã - Mã đã hết lượt sử dụng theo quota phải bị đánh dấu ineligible")
    void testGetAvailableCoupons_QuotaExceeded() {
        CustomerMembership membership = new CustomerMembership();
        membership.setTier(silverTier);
        when(customerMemberships.findByUserId(10)).thenReturn(Optional.of(membership));

        when(coupons.findByShopIsNullAndIsActiveTrue()).thenReturn(List.of(freeshipCoupon));
        when(coupons.findByShop_ShopIdAndIsActiveTrue(anyInt())).thenReturn(List.of());
        when(tierCoupons.findByTier_TierId(2)).thenReturn(List.of());

        // Khách hàng đã dùng 1 lần (quota mặc định 1 lần)
        when(orders.countValidUsedCouponByCustomer(10, 101)).thenReturn(1L);

        List<AvailableCheckoutCouponResponse> result = checkoutService.getAvailableCouponsForCustomer(
                10, 1, new BigDecimal("200000"), new BigDecimal("30000"));

        assertNotNull(result);
        assertEquals(1, result.size());
        AvailableCheckoutCouponResponse item = result.get(0);
        assertFalse(item.isEligible(), "Mã đã dùng 1/1 lượt phải không đủ điều kiện");
        assertTrue(item.getIneligibleReason().contains("Đã dùng hết"), "Phải thông báo đã dùng hết lượt");
    }
}
