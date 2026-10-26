package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.CustomerMembershipResponse;
import com.fu.SWP391_BetaFruit.dto.response.MembershipDiscountResult;
import com.fu.SWP391_BetaFruit.dto.response.TierUpgradeResult;
import com.fu.SWP391_BetaFruit.entity.CustomerMembership;
import com.fu.SWP391_BetaFruit.entity.MembershipTier;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
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
@DisplayName("Kiểm thử Nghiệp vụ Cốt lõi: Loyalty Engine Chuẩn Shopee Rewards")
class LoyaltyEngineTest {

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

    private User testUser;
    private MembershipTier bronzeTier;
    private MembershipTier silverTier;
    private MembershipTier goldTier;
    private MembershipTier diamondTier;
    private CustomerMembership customerMembership;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(10);
        testUser.setUsername("customer_an");
        testUser.setFullName("Nguyễn Bình An");

        bronzeTier = new MembershipTier(1, "Thành viên Đồng", BigDecimal.ZERO, BigDecimal.ZERO);
        silverTier = new MembershipTier(2, "Thành viên Bạc", new BigDecimal("1000000.00"), new BigDecimal("3.00"));
        goldTier = new MembershipTier(3, "Thành viên Vàng", new BigDecimal("5000000.00"), new BigDecimal("6.00"));
        diamondTier = new MembershipTier(4, "Thành viên Kim Cương", new BigDecimal("15000000.00"), new BigDecimal("10.00"));

        customerMembership = new CustomerMembership(testUser.getUserId(), testUser, BigDecimal.ZERO, bronzeTier);
    }

    @Test
    @DisplayName("Case 1: Khách đạt ngưỡng chi tiêu 1.000.000 ₫ -> Tự động thăng hạng Bạc và gửi thông báo")
    void testUpgradeBySpendThreshold() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));
        when(customerMembershipRepository.findByUserId(10)).thenReturn(Optional.of(customerMembership));
        when(orderRepository.countByCustomer_UserIdAndOrderStatus(10, OrderStatus.SUCCESS)).thenReturn(1L);
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc())
                .thenReturn(List.of(bronzeTier, silverTier, goldTier, diamondTier));

        // Khách hoàn thành đơn 1.200.000 ₫
        TierUpgradeResult result = membershipTierService.processOrderCompleted(10, 101, new BigDecimal("1200000.00"));

        assertTrue(result.isUpgraded(), "Khách hàng phải được nâng hạng");
        assertEquals("Thành viên Bạc", result.getNewTierName());
        assertEquals(new BigDecimal("3.00"), result.getNewDiscountPercent());
        assertEquals(new BigDecimal("1200000.00"), result.getCumulativeSpend());

        // Kiểm tra việc cập nhật DB
        verify(customerMembershipRepository, times(1)).save(customerMembership);
        assertEquals(silverTier, customerMembership.getTier());

        // Kiểm tra bắn Notification
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).createBaseNotification(
                eq(testUser),
                titleCaptor.capture(),
                anyString(),
                eq(NotificationType.PROMOTION),
                eq("/membership")
        );
        assertTrue(titleCaptor.getValue().contains("Thành viên Bạc"));
    }

    @Test
    @DisplayName("Case 2: Tiêu chí kép Shopee - Chi tiêu chưa đủ nhưng đạt 3 đơn hoàn tất -> Vẫn thăng hạng Bạc")
    void testUpgradeByOrderCountThreshold_ShopeeDualCriteria() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));
        when(customerMembershipRepository.findByUserId(10)).thenReturn(Optional.of(customerMembership));
        // Chi tiêu đơn chỉ 200.000đ nhưng đây là đơn thứ 3 hoàn tất
        when(orderRepository.countByCustomer_UserIdAndOrderStatus(10, OrderStatus.SUCCESS)).thenReturn(3L);
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc())
                .thenReturn(List.of(bronzeTier, silverTier, goldTier, diamondTier));

        TierUpgradeResult result = membershipTierService.processOrderCompleted(10, 102, new BigDecimal("200000.00"));

        assertTrue(result.isUpgraded(), "Tiêu chí kép Shopee: Đạt số đơn tối thiểu (3 đơn) phải được lên hạng");
        assertEquals("Thành viên Bạc", result.getNewTierName());
        assertEquals(new BigDecimal("3.00"), result.getNewDiscountPercent());
    }

    @Test
    @DisplayName("Case 3: Thăng hạng nhảy cóc (Multi-tier Leap) - Mua đơn 16.000.000 ₫ nhảy thẳng lên Kim Cương")
    void testMultiTierLeap_ToDiamond() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));
        when(customerMembershipRepository.findByUserId(10)).thenReturn(Optional.of(customerMembership));
        when(orderRepository.countByCustomer_UserIdAndOrderStatus(10, OrderStatus.SUCCESS)).thenReturn(1L);
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc())
                .thenReturn(List.of(bronzeTier, silverTier, goldTier, diamondTier));

        TierUpgradeResult result = membershipTierService.processOrderCompleted(10, 103, new BigDecimal("16000000.00"));

        assertTrue(result.isUpgraded());
        assertEquals("Thành viên Kim Cương", result.getNewTierName());
        assertEquals(new BigDecimal("10.00"), result.getNewDiscountPercent());

        // Đảm bảo chỉ gửi đúng 1 thông báo thăng hạng cao nhất
        verify(notificationService, times(1)).createBaseNotification(
                eq(testUser),
                contains("Kim Cương"),
                anyString(),
                eq(NotificationType.PROMOTION),
                eq("/membership")
        );
    }

    @Test
    @DisplayName("Case 4: Không đủ điều kiện thăng hạng -> Chỉ tích lũy chi tiêu, không bắn thông báo")
    void testAccumulateWithoutUpgrade() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));
        when(customerMembershipRepository.findByUserId(10)).thenReturn(Optional.of(customerMembership));
        when(orderRepository.countByCustomer_UserIdAndOrderStatus(10, OrderStatus.SUCCESS)).thenReturn(1L);
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc())
                .thenReturn(List.of(bronzeTier, silverTier, goldTier, diamondTier));

        // Khách hoàn tất đơn 150.000 ₫ (chưa đủ 1tr hoặc 3 đơn để lên Bạc)
        TierUpgradeResult result = membershipTierService.processOrderCompleted(10, 104, new BigDecimal("150000.00"));

        assertFalse(result.isUpgraded(), "Chưa đủ điều kiện thì không thăng hạng");
        assertEquals(new BigDecimal("150000.00"), result.getCumulativeSpend());
        verify(notificationService, never()).createBaseNotification(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Case 5: Tính toán chiết khấu đặc quyền thành viên tại Checkout (FE-09.2)")
    void testCalculateMembershipDiscount_AtCheckout() {
        customerMembership.setTier(goldTier); // Khách hạng Vàng (giảm 6%)
        when(customerMembershipRepository.findByUserId(10)).thenReturn(Optional.of(customerMembership));

        // Khách mua đơn hoa quả tạm tính 500.000 ₫
        BigDecimal subtotal = new BigDecimal("500000.00");
        MembershipDiscountResult discountResult = membershipTierService.calculateMembershipDiscount(10, subtotal);

        assertNotNull(discountResult);
        assertEquals("Thành viên Vàng", discountResult.getTierName());
        assertEquals(new BigDecimal("6.00"), discountResult.getDiscountPercent());
        assertEquals(new BigDecimal("30000.00"), discountResult.getDiscountAmount(), "Giảm 6% của 500.000 phải là 30.000 ₫");
        assertEquals(new BigDecimal("470000.00"), discountResult.getFinalAmount(), "Thực trả phải là 470.000 ₫");
    }

    @Test
    @DisplayName("Case 6: Lấy danh sách Voucher đặc quyền theo cấp bậc (Shopee Rewards Exclusive Vouchers)")
    void testGetCustomerMembershipData_WithExclusiveTierVouchers() {
        when(userRepository.findById(10)).thenReturn(Optional.of(testUser));
        when(orderRepository.countByCustomer_UserIdAndOrderStatus(10, OrderStatus.SUCCESS)).thenReturn(5L);
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc()).thenReturn(List.of(bronzeTier, silverTier, goldTier, diamondTier));

        // Khách có 2.500.000 ₫ -> Hạng Bạc
        CustomerMembership silverMember = new CustomerMembership(10, testUser, new BigDecimal("2500000.00"), silverTier);
        when(customerMembershipRepository.findByUserId(10)).thenReturn(Optional.of(silverMember));

        CustomerMembershipResponse response = membershipTierService.getCustomerMembershipData(10);

        assertNotNull(response);
        assertEquals("Thành viên Bạc", response.getCurrentTierName());
        assertNotNull(response.getTierVouchers(), "Danh sách voucher đặc quyền không được null");
        assertFalse(response.getTierVouchers().isEmpty(), "Hạng Bạc phải có ít nhất 1 voucher đặc quyền");
        assertEquals("SILVER_FREESHIP", response.getTierVouchers().get(0).getCouponCode());
        assertEquals("FREESHIP", response.getTierVouchers().get(0).getBadgeType());
    }
}
