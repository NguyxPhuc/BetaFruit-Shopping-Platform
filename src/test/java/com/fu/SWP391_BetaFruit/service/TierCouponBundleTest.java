package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.CustomerMembershipResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.impl.MembershipTierServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử Nghiệp vụ: Gói Voucher Đặc Quyền Hội Viên Chuẩn Shopee Rewards (N-N Dynamic Tier Bundle)")
class TierCouponBundleTest {

    @Mock
    private MembershipTierRepository membershipTierRepository;

    @Mock
    private CustomerMembershipRepository customerMembershipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private TierCouponRepository tierCouponRepository;

    @InjectMocks
    private MembershipTierServiceImpl membershipTierService;

    private MembershipTier silverTier;
    private MembershipTier goldTier;
    private Coupon platformCoupon1;
    private Coupon platformCoupon2;
    private Coupon shopCoupon;

    @BeforeEach
    void setUp() {
        silverTier = new MembershipTier(2, "Hạng Bạc", BigDecimal.valueOf(2_000_000), 3);
        goldTier = new MembershipTier(3, "Hạng Vàng", BigDecimal.valueOf(5_000_000), 10);

        // Coupon 1: Toàn sàn (Platform, shop == null)
        platformCoupon1 = new Coupon();
        platformCoupon1.setCouponId(101);
        platformCoupon1.setCouponCode("BETAFRUIT10");
        platformCoupon1.setShop(null);
        platformCoupon1.setDiscountType(DiscountType.PERCENT);
        platformCoupon1.setDiscountValue(BigDecimal.valueOf(10));
        platformCoupon1.setIsActive(true);

        // Coupon 2: Toàn sàn Freeship (Platform, shop == null)
        platformCoupon2 = new Coupon();
        platformCoupon2.setCouponId(102);
        platformCoupon2.setCouponCode("GOLD_FREESHIP");
        platformCoupon2.setShop(null);
        platformCoupon2.setCouponCategory(CouponCategory.SHIPPING);
        platformCoupon2.setDiscountType(DiscountType.FIXED_AMOUNT);
        platformCoupon2.setDiscountValue(BigDecimal.valueOf(40_000));
        platformCoupon2.setIsActive(true);

        // Coupon 3: Của riêng một Shop (ShopId != null)
        Shop mockShop = new Shop();
        mockShop.setShopId(99);
        shopCoupon = new Coupon();
        shopCoupon.setCouponId(103);
        shopCoupon.setCouponCode("SHOP_DISCOUNT");
        shopCoupon.setShop(mockShop);
        shopCoupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        shopCoupon.setDiscountValue(BigDecimal.valueOf(20_000));
        shopCoupon.setIsActive(true);
    }

    @Test
    @DisplayName("Admin chỉ được hiển thị các mã Toàn Sàn (ShopId IS NULL) đang hoạt động")
    void testAdminCanViewOnlyPlatformCoupons() {
        when(couponRepository.findByShopIsNullAndIsActiveTrue())
                .thenReturn(List.of(platformCoupon1, platformCoupon2));

        List<Coupon> platformCoupons = membershipTierService.getAvailablePlatformCoupons();

        assertNotNull(platformCoupons);
        assertEquals(2, platformCoupons.size());
        assertTrue(platformCoupons.stream().allMatch(c -> c.getShop() == null));
        verify(couponRepository).findByShopIsNullAndIsActiveTrue();
    }

    @Test
    @DisplayName("Admin cấu hình gán nhiều mã Toàn Sàn vào Gói của Hạng Vàng thành công")
    void testAdminConfiguresVouchersForTierSuccessfully() {
        when(membershipTierRepository.findById(3)).thenReturn(Optional.of(goldTier));
        when(couponRepository.findAllById(List.of(101, 102))).thenReturn(List.of(platformCoupon1, platformCoupon2));

        membershipTierService.updateTierCoupons(3, List.of(101, 102));

        // Kiểm tra xóa liên kết cũ
        verify(tierCouponRepository).deleteByTier_TierId(3);

        // Kiểm tra lưu liên kết mới
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TierCoupon>> captor = ArgumentCaptor.forClass(List.class);
        verify(tierCouponRepository).saveAll(captor.capture());

        List<TierCoupon> savedList = captor.getValue();
        assertEquals(2, savedList.size());
        assertEquals("Hạng Vàng", savedList.get(0).getTier().getTierName());
        assertEquals("BETAFRUIT10", savedList.get(0).getCoupon().getCouponCode());
        assertEquals("GOLD_FREESHIP", savedList.get(1).getCoupon().getCouponCode());
    }

    @Test
    @DisplayName("Bảo vệ hệ thống: Chặn Admin không được gán mã riêng của Shop vào Gói Hội Viên của Sàn")
    void testAdminConfiguresShopCouponThrowsException() {
        when(membershipTierRepository.findById(3)).thenReturn(Optional.of(goldTier));
        when(couponRepository.findAllById(List.of(103))).thenReturn(List.of(shopCoupon));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                membershipTierService.updateTierCoupons(3, List.of(103))
        );

        assertTrue(ex.getMessage().contains("là mã riêng của Shop, không thể đưa vào gói quyền lợi Hạng"));
        verify(tierCouponRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Khách hàng đã dùng 1 trong 2 voucher -> Voucher đó hiển thị isUsed = true, voucher còn lại isUsed = false")
    void testCustomerTierVoucherUsedStatusDetection() {
        User user = new User();
        user.setUserId(10);
        user.setFullName("Nguyễn Văn Khách");

        CustomerMembership membership = new CustomerMembership();
        membership.setUser(user);
        membership.setTier(goldTier);
        membership.setTotalCumulativeSpend(BigDecimal.valueOf(6_000_000));

        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(customerMembershipRepository.findByUserId(10)).thenReturn(Optional.of(membership));
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc())
                .thenReturn(List.of(
                        new MembershipTier(1, "Hạng Đồng", BigDecimal.ZERO, 0),
                        silverTier,
                        goldTier
                ));

        TierCoupon tc1 = new TierCoupon(goldTier, platformCoupon1);
        TierCoupon tc2 = new TierCoupon(goldTier, platformCoupon2);
        when(tierCouponRepository.findByTier_TierId(3)).thenReturn(List.of(tc1, tc2));

        when(orderRepository.countValidUsedCouponByCustomer(10, 101)).thenReturn(1L);
        when(orderRepository.countValidUsedCouponByCustomer(10, 102)).thenReturn(0L);

        CustomerMembershipResponse response = membershipTierService.getCustomerMembershipData(10);

        assertNotNull(response);
        assertNotNull(response.getTierVouchers());
        assertEquals(2, response.getTierVouchers().size());

        assertTrue(response.getTierVouchers().get(0).isUsed(), "Mã BETAFRUIT10 đã dùng phải có isUsed = true");
        assertFalse(response.getTierVouchers().get(1).isUsed(), "Mã GOLD_FREESHIP chưa dùng phải có isUsed = false");
    }

    @Test
    @DisplayName("Khách hàng vào trang /membership thấy chính xác các voucher động trong gói do Admin gán")
    void testCustomerViewsDynamicVouchersConfiguredByAdmin() {
        User user = new User();
        user.setUserId(10);
        user.setFullName("Nguyễn Văn Khách");

        CustomerMembership membership = new CustomerMembership();
        membership.setUser(user);
        membership.setTier(goldTier);
        membership.setTotalCumulativeSpend(BigDecimal.valueOf(6_000_000));

        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(customerMembershipRepository.findByUserId(10)).thenReturn(Optional.of(membership));
        when(orderRepository.countByCustomer_UserIdAndOrderStatusIn(eq(10), any())).thenReturn(5L);
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc())
                .thenReturn(List.of(
                        new MembershipTier(1, "Hạng Đồng", BigDecimal.ZERO, 0),
                        silverTier,
                        goldTier
                ));

        // Giả lập Admin đã cấu hình 2 voucher trong gói của Hạng Vàng
        TierCoupon tc1 = new TierCoupon(goldTier, platformCoupon1);
        TierCoupon tc2 = new TierCoupon(goldTier, platformCoupon2);
        when(tierCouponRepository.findByTier_TierId(3)).thenReturn(List.of(tc1, tc2));

        CustomerMembershipResponse response = membershipTierService.getCustomerMembershipData(10);

        assertNotNull(response);
        assertEquals("Hạng Vàng", response.getCurrentTierName());
        assertNotNull(response.getTierVouchers());
        assertEquals(2, response.getTierVouchers().size());

        // Kiểm tra đúng 2 mã do Admin gán hiển thị lên
        assertEquals("BETAFRUIT10", response.getTierVouchers().get(0).getCouponCode());
        assertEquals("Giảm 10%", response.getTierVouchers().get(0).getTitle());

        assertEquals("GOLD_FREESHIP", response.getTierVouchers().get(1).getCouponCode());
        assertEquals("FREESHIP", response.getTierVouchers().get(1).getBadgeType());
    }

    @Test
    @DisplayName("Admin bỏ tích tất cả voucher: xóa sạch mã trong gói một cách an toàn")
    void testAdminUnchecksAllCouponsClearsBundle() {
        when(membershipTierRepository.findById(3)).thenReturn(Optional.of(goldTier));

        membershipTierService.updateTierCoupons(3, Collections.emptyList());

        verify(tierCouponRepository).deleteByTier_TierId(3);
        verify(tierCouponRepository, never()).saveAll(any());
    }
    @Test
    @DisplayName("Admin cấu hình số lượt sử dụng (Quota) tùy chỉnh cho từng mã voucher trong Hạng")
    void testAdminConfiguresCustomQuotaForTierCoupons() {
        when(membershipTierRepository.findById(3)).thenReturn(Optional.of(goldTier));
        when(couponRepository.findAllById(List.of(101, 102))).thenReturn(List.of(platformCoupon1, platformCoupon2));

        Map<Integer, Integer> quotaMap = Map.of(101, 2, 102, 3);
        membershipTierService.updateTierCoupons(3, List.of(101, 102), quotaMap);

        verify(tierCouponRepository).deleteByTier_TierId(3);
        ArgumentCaptor<List<TierCoupon>> captor = ArgumentCaptor.forClass(List.class);
        verify(tierCouponRepository).saveAll(captor.capture());

        List<TierCoupon> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals(2, saved.get(0).getUsageQuota());
        assertEquals(3, saved.get(1).getUsageQuota());
    }

    @Test
    @DisplayName("Khách hàng hiển thị số lượt còn lại và tiến trình theo Quota đã được cấp")
    void testCustomerVoucherQuotaProgressAndUsageRemaining() {
        User user = new User();
        user.setUserId(10);
        user.setFullName("Khách VIP");

        CustomerMembership membership = new CustomerMembership();
        membership.setUser(user);
        membership.setTier(goldTier);
        membership.setTotalCumulativeSpend(BigDecimal.valueOf(6_000_000));

        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(customerMembershipRepository.findByUserId(10)).thenReturn(Optional.of(membership));
        when(orderRepository.countByCustomer_UserIdAndOrderStatusIn(eq(10), any())).thenReturn(5L);
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc())
                .thenReturn(List.of(new MembershipTier(1, "Hạng Đồng", BigDecimal.ZERO, 0), silverTier, goldTier));

        // TierCoupon với Quota = 3
        TierCoupon tc = new TierCoupon(goldTier, platformCoupon2, 3);
        when(tierCouponRepository.findByTier_TierId(3)).thenReturn(List.of(tc));

        // Khách hàng đã sử dụng 1 lần trong đơn hàng thành công
        when(orderRepository.countValidUsedCouponByCustomer(10, 102)).thenReturn(1L);

        CustomerMembershipResponse response = membershipTierService.getCustomerMembershipData(10);

        assertNotNull(response);
        assertEquals(1, response.getTierVouchers().size());
        var v = response.getTierVouchers().get(0);
        assertEquals(3, v.getTotalQuota());
        assertEquals(1, v.getUsedCount());
        assertEquals(2, v.getRemainingQuota());
        assertFalse(v.isUsed()); // Chưa hết lượt
        assertEquals("x3 lượt", v.getQuotaBadgeText());
        assertEquals("Còn lại 2/3 lượt", v.getUsageProgressText());
    }

    @Test
    @DisplayName("Kiểm tra phân giải định mức và chặn khi đã dùng hết sạch Quota khi Checkout")
    void testCheckoutTierQuotaEnforcement() {
        TierCoupon tc = new TierCoupon(goldTier, platformCoupon2, 3);
        List<TierCoupon> bundle = List.of(tc);

        // Phân giải quota của Hạng Vàng = 3
        int quota = com.fu.SWP391_BetaFruit.service.impl.CheckoutCouponRules.resolveTierQuota(bundle, goldTier, 1);
        assertEquals(3, quota);

        // Lần 1 và Lần 2 (used = 1, 2) -> Cho phép
        assertDoesNotThrow(() -> com.fu.SWP391_BetaFruit.service.impl.CheckoutCouponRules.validateUserUsageLimit(1, quota));
        assertDoesNotThrow(() -> com.fu.SWP391_BetaFruit.service.impl.CheckoutCouponRules.validateUserUsageLimit(2, quota));

        // Lần 4 (đã dùng đủ 3 lần, used = 3) -> Ném ngoại lệ chặn lịch sự
        var ex = assertThrows(IllegalArgumentException.class,
                () -> com.fu.SWP391_BetaFruit.service.impl.CheckoutCouponRules.validateUserUsageLimit(3, quota));
        assertTrue(ex.getMessage().contains("Bạn đã sử dụng hết 3 lượt cho mã ưu đãi này"));
    }
}
