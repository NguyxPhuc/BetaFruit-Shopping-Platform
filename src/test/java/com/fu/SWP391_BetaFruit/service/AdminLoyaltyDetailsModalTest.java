package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.controller.AdminController;
import com.fu.SWP391_BetaFruit.dto.response.CustomerMembershipResponse;
import com.fu.SWP391_BetaFruit.dto.response.TierVoucherResponse;
import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.entity.CustomerMembership;
import com.fu.SWP391_BetaFruit.entity.MembershipTier;
import com.fu.SWP391_BetaFruit.entity.TierCoupon;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import com.fu.SWP391_BetaFruit.repository.CustomerMembershipRepository;
import com.fu.SWP391_BetaFruit.repository.MembershipTierRepository;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.TierCouponRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.impl.MembershipTierServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Kiểm thử API và Service hiển thị chi tiết Hồ Sơ Hội Viên trên trang Admin")
class AdminLoyaltyDetailsModalTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerMembershipRepository customerMembershipRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MembershipTierRepository membershipTierRepository;

    @Mock
    private TierCouponRepository tierCouponRepository;

    @Mock
    private MembershipTierService membershipTierService;

    @InjectMocks
    private MembershipTierServiceImpl membershipTierServiceImpl;

    @Test
    @DisplayName("AdminController trả về 200 OK khi nạp thông tin hội viên thành công")
    void adminController_GetUserLoyaltyDetails_Success() {
        AdminController adminController = new AdminController(
                null, null, null, null, membershipTierService
        );

        CustomerMembershipResponse mockResponse = CustomerMembershipResponse.builder()
                .userId(8)
                .fullName("Nguyễn Thị Kim Hoa")
                .username("customer_hoa")
                .currentTierName("Thành viên Vàng")
                .totalCumulativeSpend(new BigDecimal("3130000"))
                .completedOrdersCount(4)
                .build();

        when(membershipTierService.getCustomerMembershipData(8)).thenReturn(mockResponse);

        ResponseEntity<?> response = adminController.getUserLoyaltyDetails(8);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof CustomerMembershipResponse);
        CustomerMembershipResponse body = (CustomerMembershipResponse) response.getBody();
        assertEquals("Nguyễn Thị Kim Hoa", body.getFullName());
        assertEquals("Thành viên Vàng", body.getCurrentTierName());
    }

    @Test
    @DisplayName("AdminController trả về 400 Bad Request kèm thông báo khi không tìm thấy user")
    void adminController_GetUserLoyaltyDetails_UserNotFound() {
        AdminController adminController = new AdminController(
                null, null, null, null, membershipTierService
        );

        when(membershipTierService.getCustomerMembershipData(999))
                .thenThrow(new IllegalArgumentException("Không tìm thấy thông tin người dùng với ID: 999"));

        ResponseEntity<?> response = adminController.getUserLoyaltyDetails(999);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Không tìm thấy thông tin người dùng với ID: 999", body.get("error"));
    }

    @Test
    @DisplayName("MembershipTierServiceImpl nạp dữ liệu hội viên và gắn cờ voucher đã dùng chính xác")
    void membershipTierService_GetCustomerMembershipData_WithVouchersAndUsage() {
        User user = new User();
        user.setUserId(8);
        user.setFullName("Nguyễn Thị Kim Hoa");
        user.setUsername("customer_hoa");

        CustomerMembership membership = new CustomerMembership();
        membership.setTotalCumulativeSpend(new BigDecimal("3130000"));

        MembershipTier bronze = new MembershipTier(1, "Thành viên Đồng", BigDecimal.ZERO, 0);
        MembershipTier silver = new MembershipTier(2, "Thành viên Bạc", new BigDecimal("1000000"), 2);
        MembershipTier gold = new MembershipTier(3, "Thành viên Vàng", new BigDecimal("3000000"), 4);
        List<MembershipTier> tiers = List.of(bronze, silver, gold);

        when(userRepository.findById(8)).thenReturn(Optional.of(user));
        when(customerMembershipRepository.findByUserId(8)).thenReturn(Optional.of(membership));
        when(orderRepository.countByCustomer_UserIdAndOrderStatusIn(eq(8), any())).thenReturn(4L);
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc()).thenReturn(tiers);

        // Voucher cho Hạng Vàng
        Coupon coupon1 = new Coupon();
        coupon1.setCouponId(101);
        coupon1.setCouponCode("GOLD_FREE_SHIP");
        coupon1.setDiscountType(DiscountType.FIXED_AMOUNT);
        coupon1.setCouponCategory(CouponCategory.SHIPPING);
        coupon1.setDiscountValue(new BigDecimal("40000"));
        coupon1.setMinOrderValue(new BigDecimal("200000"));
        coupon1.setIsActive(true);
        coupon1.setEndDate(LocalDateTime.now().plusDays(30));

        TierCoupon tc1 = new TierCoupon();
        tc1.setTier(gold);
        tc1.setCoupon(coupon1);
        tc1.setUsageQuota(1);

        when(tierCouponRepository.findByTier_TierId(3)).thenReturn(List.of(tc1));
        // Khách hàng ID 8 đã dùng coupon 101 một lần
        when(orderRepository.countValidUsedCouponByCustomer(8, 101)).thenReturn(1L);

        CustomerMembershipResponse result = membershipTierServiceImpl.getCustomerMembershipData(8);

        assertNotNull(result);
        assertEquals(8, result.getUserId());
        assertEquals("Thành viên Vàng", result.getCurrentTierName());
        assertEquals(new BigDecimal("3130000"), result.getTotalCumulativeSpend());
        assertEquals(4L, result.getCompletedOrdersCount());
        assertFalse(result.isHasNextTier()); // Là hạng cao nhất trong danh sách mock

        // Kiểm tra danh sách voucher đặc quyền
        List<TierVoucherResponse> vouchers = result.getTierVouchers();
        assertNotNull(vouchers);
        assertEquals(1, vouchers.size());
        TierVoucherResponse v = vouchers.get(0);
        assertEquals("GOLD_FREE_SHIP", v.getCouponCode());
        assertEquals("Freeship 40.000 ₫", v.getTitle());
        assertTrue(v.isUsed(), "Voucher phải được đánh dấu là isUsed = true vì khách đã dùng 1 lần");
    }

    @Test
    @DisplayName("MembershipTierServiceImpl an toàn tuyệt đối với Null-Safety khi minSpentRequired bị null")
    void membershipTierService_NullSafety_ShouldNotThrowException() {
        User user = new User();
        user.setUserId(7);
        user.setFullName("customer_phuc");
        user.setUsername("customer_phuc");

        // Tier có minSpentRequired = null và minOrdersRequired = null
        MembershipTier nullTier = new MembershipTier(1, "Thành viên Mới", null, null);
        MembershipTier nextNullTier = new MembershipTier(2, "Hạng Kế Tiếp", null, null);

        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(customerMembershipRepository.findByUserId(7)).thenReturn(Optional.empty());
        when(orderRepository.countByCustomer_UserIdAndOrderStatusIn(eq(7), any())).thenReturn(0L);
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc()).thenReturn(List.of(nullTier, nextNullTier));
        when(tierCouponRepository.findByTier_TierId(anyInt())).thenReturn(List.of());

        assertDoesNotThrow(() -> {
            CustomerMembershipResponse result = membershipTierServiceImpl.getCustomerMembershipData(7);
            assertNotNull(result);
            assertEquals(BigDecimal.ZERO, result.getTotalCumulativeSpend());
            assertEquals(0L, result.getCompletedOrdersCount());
        });
    }

    @Test
    @DisplayName("syncCustomerMembershipTier tự động nâng hạng lên Kim Cương khi đủ 10 đơn >= 7 đơn và lưu DB")
    void syncCustomerMembershipTier_ShouldAutoUpgradeAndSaveToDatabase() {
        User user = new User();
        user.setUserId(9);
        user.setUsername("customer_linh");
        user.setFullName("Phạm Thùy Linh");

        MembershipTier bronze = new MembershipTier(1, "Thành viên Đồng", BigDecimal.ZERO, 0);
        MembershipTier diamond = new MembershipTier(4, "Thành viên Kim Cương", new BigDecimal("3100000"), 7);

        // Khách hàng hiện tại trong DB chỉ lưu hạng Đồng
        CustomerMembership membership = new CustomerMembership(9, user, new BigDecimal("250000"), bronze);

        when(userRepository.findById(9)).thenReturn(Optional.of(user));
        when(customerMembershipRepository.findByUserId(9)).thenReturn(Optional.of(membership));
        // Khách hàng có 10 đơn hoàn tất (>= 7 đơn của Kim Cương)
        when(orderRepository.countByCustomer_UserIdAndOrderStatusIn(eq(9), any())).thenReturn(10L);
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc()).thenReturn(List.of(bronze, diamond));
        when(customerMembershipRepository.save(any(CustomerMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerMembership synced = membershipTierServiceImpl.syncCustomerMembershipTier(9);

        assertNotNull(synced);
        assertEquals(diamond, synced.getTier());
        assertEquals("Thành viên Kim Cương", synced.getTier().getTierName());
        org.mockito.Mockito.verify(customerMembershipRepository).save(membership);
    }
}
