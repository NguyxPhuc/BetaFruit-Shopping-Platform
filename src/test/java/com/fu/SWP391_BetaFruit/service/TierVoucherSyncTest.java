package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.MembershipTierRequest;
import com.fu.SWP391_BetaFruit.dto.response.CustomerMembershipResponse;
import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.entity.CustomerMembership;
import com.fu.SWP391_BetaFruit.entity.MembershipTier;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.CouponRepository;
import com.fu.SWP391_BetaFruit.repository.CustomerMembershipRepository;
import com.fu.SWP391_BetaFruit.repository.MembershipTierRepository;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.TierCouponRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử Nghiệp vụ: Tự động đồng bộ Coupon VIP Đặc quyền Hội viên (FE-09)")
class TierVoucherSyncTest {

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

    private MembershipTier bronzeTier;
    private MembershipTier silverTier;
    private MembershipTier goldTier;
    private MembershipTier diamondTier;

    @BeforeEach
    void setUp() {
        bronzeTier = new MembershipTier(1, "Thành viên Đồng", BigDecimal.ZERO, BigDecimal.ZERO);
        silverTier = new MembershipTier(2, "Thành viên Bạc", new BigDecimal("1000000.00"), new BigDecimal("3.00"));
        goldTier = new MembershipTier(3, "Thành viên Vàng", new BigDecimal("5000000.00"), new BigDecimal("6.00"));
        diamondTier = new MembershipTier(4, "Thành viên Kim Cương", new BigDecimal("15000000.00"), new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Case 1: Admin tạo Hạng Vàng mới -> Tự động sinh Coupon GOLD_VIP sàn tài trợ")
    void testCreateTier_ShouldAutomaticallyCreatePlatformCoupon() {
        MembershipTierRequest request = new MembershipTierRequest();
        request.setTierName("Thành viên Vàng");
        request.setMinSpentRequired(new BigDecimal("5000000.00"));
        request.setDiscountPercent(new BigDecimal("6.00"));

        when(membershipTierRepository.findAll()).thenReturn(List.of(bronzeTier, silverTier));
        when(membershipTierRepository.save(any(MembershipTier.class))).thenAnswer(invocation -> {
            MembershipTier t = invocation.getArgument(0);
            t.setTierId(3);
            return t;
        });
        when(couponRepository.findByCouponCodeIgnoreCase("GOLD_VIP")).thenReturn(Optional.empty());

        membershipTierService.createTier(request);

        ArgumentCaptor<Coupon> couponCaptor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository, times(1)).save(couponCaptor.capture());

        Coupon created = couponCaptor.getValue();
        assertEquals("GOLD_VIP", created.getCouponCode());
        assertNull(created.getShop(), "Voucher VIP hội viên phải do Sàn tài trợ (shop == null)");
        assertEquals(CouponCategory.PRODUCT, created.getCouponCategory());
        assertEquals(DiscountType.PERCENT, created.getDiscountType());
        assertEquals(new BigDecimal("6.00"), created.getDiscountValue());
        assertEquals(new BigDecimal("50000"), created.getMaxDiscountAmount());
        assertEquals(4, created.getUsageLimit());
        assertTrue(created.getIsActive());
    }

    @Test
    @DisplayName("Case 2: Admin cập nhật % Hạng Vàng từ 6% lên 7% -> Tự động update Coupon GOLD_VIP")
    void testUpdateTier_ShouldUpdateExistingPlatformCoupon() {
        MembershipTierRequest request = new MembershipTierRequest();
        request.setTierName("Thành viên Vàng");
        request.setMinSpentRequired(new BigDecimal("5000000.00"));
        request.setDiscountPercent(new BigDecimal("7.00"));

        Coupon existingCoupon = new Coupon();
        existingCoupon.setCouponId(99);
        existingCoupon.setCouponCode("GOLD_VIP");
        existingCoupon.setDiscountValue(new BigDecimal("6.00"));
        existingCoupon.setMaxDiscountAmount(new BigDecimal("50000"));

        when(membershipTierRepository.findById(3)).thenReturn(Optional.of(goldTier));
        when(membershipTierRepository.findAll()).thenReturn(List.of(bronzeTier, silverTier, goldTier));
        when(membershipTierRepository.save(any(MembershipTier.class))).thenReturn(goldTier);
        when(couponRepository.findByCouponCodeIgnoreCase("GOLD_VIP")).thenReturn(Optional.of(existingCoupon));

        membershipTierService.updateTier(3, request);

        ArgumentCaptor<Coupon> couponCaptor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository, times(1)).save(couponCaptor.capture());

        Coupon updated = couponCaptor.getValue();
        assertEquals("GOLD_VIP", updated.getCouponCode());
        assertEquals(new BigDecimal("7.00"), updated.getDiscountValue());
        assertTrue(updated.getIsActive());
    }

    @Test
    @DisplayName("Case 3: Xóa Hạng thành viên -> Vô hiệu hóa (deactivate) Coupon VIP tương ứng")
    void testDeleteTier_ShouldDeactivateVipCoupon() {
        Coupon existingCoupon = new Coupon();
        existingCoupon.setCouponId(99);
        existingCoupon.setCouponCode("GOLD_VIP");
        existingCoupon.setIsActive(true);

        when(membershipTierRepository.findById(3)).thenReturn(Optional.of(goldTier));
        when(customerMembershipRepository.existsByTier_TierId(3)).thenReturn(false);
        when(couponRepository.findByCouponCodeIgnoreCase("GOLD_VIP")).thenReturn(Optional.of(existingCoupon));

        membershipTierService.deleteTier(3);

        verify(membershipTierRepository, times(1)).delete(goldTier);
        verify(couponRepository, times(1)).save(existingCoupon);
        assertFalse(existingCoupon.getIsActive(), "Coupon phải được chuyển sang không hoạt động khi xóa hạng");
    }

    @Test
    @DisplayName("Case 4: Khách hàng Hạng Vàng xem danh sách Voucher đặc quyền -> Có chứa GOLD_VIP")
    void testGetCustomerMembershipData_IncludesVipCoupon() {
        User user = new User();
        user.setUserId(20);
        user.setUsername("gold_user");

        CustomerMembership membership = new CustomerMembership(20, user, new BigDecimal("6000000.00"), goldTier);

        when(userRepository.findById(20)).thenReturn(Optional.of(user));
        when(orderRepository.countByCustomer_UserIdAndOrderStatus(20, OrderStatus.SUCCESS)).thenReturn(10L);
        when(customerMembershipRepository.findByUserId(20)).thenReturn(Optional.of(membership));
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc())
                .thenReturn(List.of(bronzeTier, silverTier, goldTier, diamondTier));

        Coupon dbCoupon = new Coupon();
        dbCoupon.setCouponCode("GOLD_VIP");
        dbCoupon.setDiscountType(DiscountType.PERCENT);
        dbCoupon.setDiscountValue(new BigDecimal("6.00"));
        dbCoupon.setMaxDiscountAmount(new BigDecimal("50000"));
        dbCoupon.setMinOrderValue(new BigDecimal("100000"));
        dbCoupon.setIsActive(true);

        when(couponRepository.findByCouponCodeIgnoreCase(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            if ("GOLD_VIP".equalsIgnoreCase(code)) {
                return Optional.of(dbCoupon);
            }
            return Optional.empty();
        });

        CustomerMembershipResponse response = membershipTierService.getCustomerMembershipData(20);

        assertNotNull(response);
        assertNotNull(response.getTierVouchers());
        boolean hasGoldVip = response.getTierVouchers().stream()
                .anyMatch(v -> "GOLD_VIP".equals(v.getCouponCode()));
        assertTrue(hasGoldVip, "Danh sách voucher của Hạng Vàng phải có mã GOLD_VIP");
    }
}
