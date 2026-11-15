package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.MembershipTierRequest;
import com.fu.SWP391_BetaFruit.dto.response.CustomerMembershipResponse;
import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.entity.CustomerMembership;
import com.fu.SWP391_BetaFruit.entity.MembershipTier;
import com.fu.SWP391_BetaFruit.entity.TierCoupon;
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
@DisplayName("Kiểm thử Nghiệp vụ: Quản lý Hạng thành viên & Gói Voucher Đặc quyền (FE-09)")
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
        bronzeTier = new MembershipTier(1, "Thành viên Đồng", BigDecimal.ZERO, 0);
        silverTier = new MembershipTier(2, "Thành viên Bạc", new BigDecimal("1000000.00"), 3);
        goldTier = new MembershipTier(3, "Thành viên Vàng", new BigDecimal("5000000.00"), 10);
        diamondTier = new MembershipTier(4, "Thành viên Kim Cương", new BigDecimal("15000000.00"), 25);
    }

    @Test
    @DisplayName("Case 1: Admin tạo Hạng Vàng mới -> Lưu thành công ngưỡng chi tiêu và số đơn yêu cầu")
    void testCreateTier_ShouldSaveWithMinOrdersRequired() {
        MembershipTierRequest request = new MembershipTierRequest();
        request.setTierName("Thành viên Vàng");
        request.setMinSpentRequired(new BigDecimal("5000000.00"));
        request.setMinOrdersRequired(10);

        when(membershipTierRepository.findAll()).thenReturn(List.of(bronzeTier, silverTier));
        when(membershipTierRepository.save(any(MembershipTier.class))).thenAnswer(invocation -> {
            MembershipTier t = invocation.getArgument(0);
            t.setTierId(3);
            return t;
        });

        membershipTierService.createTier(request);

        ArgumentCaptor<MembershipTier> tierCaptor = ArgumentCaptor.forClass(MembershipTier.class);
        verify(membershipTierRepository, times(1)).save(tierCaptor.capture());

        MembershipTier created = tierCaptor.getValue();
        assertEquals("Thành viên Vàng", created.getTierName());
        assertEquals(new BigDecimal("5000000.00"), created.getMinSpentRequired());
        assertEquals(10, created.getMinOrdersRequired());
    }

    @Test
    @DisplayName("Case 2: Admin cập nhật Hạng Vàng -> Cập nhật thành công số đơn yêu cầu")
    void testUpdateTier_ShouldUpdateMinOrdersRequired() {
        MembershipTierRequest request = new MembershipTierRequest();
        request.setTierName("Thành viên Vàng");
        request.setMinSpentRequired(new BigDecimal("5000000.00"));
        request.setMinOrdersRequired(12);

        when(membershipTierRepository.findById(3)).thenReturn(Optional.of(goldTier));
        when(membershipTierRepository.findAll()).thenReturn(List.of(bronzeTier, silverTier, goldTier));
        when(membershipTierRepository.save(any(MembershipTier.class))).thenReturn(goldTier);

        membershipTierService.updateTier(3, request);

        ArgumentCaptor<MembershipTier> tierCaptor = ArgumentCaptor.forClass(MembershipTier.class);
        verify(membershipTierRepository, times(1)).save(tierCaptor.capture());

        MembershipTier updated = tierCaptor.getValue();
        assertEquals("Thành viên Vàng", updated.getTierName());
        assertEquals(12, updated.getMinOrdersRequired());
    }

    @Test
    @DisplayName("Case 3: Xóa Hạng thành viên -> Xóa liên kết TierCoupon và xóa hạng")
    void testDeleteTier_ShouldDeleteTierAndAssociatedCoupons() {
        when(membershipTierRepository.findById(3)).thenReturn(Optional.of(goldTier));
        when(customerMembershipRepository.existsByTier_TierId(3)).thenReturn(false);

        membershipTierService.deleteTier(3);

        verify(tierCouponRepository, times(1)).deleteByTier_TierId(3);
        verify(membershipTierRepository, times(1)).delete(goldTier);
    }

    @Test
    @DisplayName("Case 4: Khách hàng Hạng Vàng xem danh sách Voucher đặc quyền theo cấu hình TierCoupon")
    void testGetCustomerMembershipData_IncludesVipCoupon() {
        User user = new User();
        user.setUserId(20);
        user.setUsername("gold_user");

        CustomerMembership membership = new CustomerMembership(20, user, new BigDecimal("6000000.00"), goldTier);

        when(userRepository.findById(20)).thenReturn(Optional.of(user));
        when(orderRepository.countByCustomer_UserIdAndOrderStatusIn(eq(20), any())).thenReturn(10L);
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

        TierCoupon tierCoupon = new TierCoupon(goldTier, dbCoupon);
        when(tierCouponRepository.findByTier_TierId(3)).thenReturn(List.of(tierCoupon));

        CustomerMembershipResponse response = membershipTierService.getCustomerMembershipData(20);

        assertNotNull(response);
        assertNotNull(response.getTierVouchers());
        boolean hasGoldVip = response.getTierVouchers().stream()
                .anyMatch(v -> "GOLD_VIP".equals(v.getCouponCode()));
        assertTrue(hasGoldVip, "Danh sách voucher của Hạng Vàng phải có mã GOLD_VIP");
    }
}
