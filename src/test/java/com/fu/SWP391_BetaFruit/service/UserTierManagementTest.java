package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.UserTierStatResponse;
import com.fu.SWP391_BetaFruit.entity.MembershipTier;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import com.fu.SWP391_BetaFruit.repository.CustomerMembershipRepository;
import com.fu.SWP391_BetaFruit.repository.MembershipTierRepository;
import com.fu.SWP391_BetaFruit.repository.RoleRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.impl.UserServiceImpl;
import com.fu.SWP391_BetaFruit.utils.RedirectUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử tính năng thống kê và lọc người dùng theo hạng thành viên")
class UserTierManagementTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CustomerMembershipRepository customerMembershipRepository;

    @Mock
    private MembershipTierRepository tierRepository;

    @Mock
    private MembershipTierService membershipTierService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Thống kê số lượng người dùng theo từng hạng và nhóm nội bộ thành công")
    void getAdminUserPageData_ShouldCalculateTierDistributionStats() {
        MembershipTier bronze = new MembershipTier(1, "Thành viên Đồng", BigDecimal.ZERO, 0);
        MembershipTier gold = new MembershipTier(3, "Thành viên Vàng", new BigDecimal("5000000"), 5);
        List<MembershipTier> mockTiers = List.of(bronze, gold);

        when(tierRepository.findAllByOrderByMinSpentRequiredAsc()).thenReturn(mockTiers);
        when(userRepository.count()).thenReturn(20L);
        when(customerMembershipRepository.countByTier_TierId(1)).thenReturn(10L);
        when(customerMembershipRepository.countByTier_TierId(3)).thenReturn(4L);
        when(roleRepository.findAll()).thenReturn(Collections.emptyList());
        when(userRepository.filterUsers(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        Map<String, Object> result = userService.getAdminUserPageData(null, null, "ALL", 1, 1, 10, "userId", "asc");

        assertNotNull(result);
        assertTrue(result.containsKey("tierStats"));
        @SuppressWarnings("unchecked")
        List<UserTierStatResponse> stats = (List<UserTierStatResponse>) result.get("tierStats");

        // 2 hạng đã tạo + 1 nhóm chưa phân hạng / nội bộ = 3
        assertEquals(3, stats.size());

        UserTierStatResponse bronzeStat = stats.get(0);
        assertEquals(1, bronzeStat.getTierId());
        assertEquals("Thành viên Đồng", bronzeStat.getTierName());
        assertEquals(10L, bronzeStat.getCount());

        UserTierStatResponse goldStat = stats.get(1);
        assertEquals(3, goldStat.getTierId());
        assertEquals("Thành viên Vàng", goldStat.getTierName());
        assertEquals(4L, goldStat.getCount());

        UserTierStatResponse unrankedStat = stats.get(2);
        assertEquals(-1, unrankedStat.getTierId());
        assertEquals("Nội bộ / Chưa có", unrankedStat.getTierName());
        assertEquals(6L, unrankedStat.getCount()); // 20 - (10 + 4) = 6
    }

    @Test
    @DisplayName("Lọc người dùng theo tierId truyền đúng tham số xuống UserRepository")
    void getAdminUserPageData_WithTierId_ShouldPassTierIdToRepository() {
        when(tierRepository.findAllByOrderByMinSpentRequiredAsc()).thenReturn(Collections.emptyList());
        when(userRepository.count()).thenReturn(5L);
        when(roleRepository.findAll()).thenReturn(Collections.emptyList());
        when(userRepository.filterUsers(eq("nguyen"), eq(2L), eq(UserStatus.ACTIVE), eq(3), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        Map<String, Object> result = userService.getAdminUserPageData("nguyen", 2L, "ACTIVE", 3, 1, 5, "userId", "asc");

        assertEquals(3, result.get("currentTierId"));
        verify(userRepository).filterUsers(eq("nguyen"), eq(2L), eq(UserStatus.ACTIVE), eq(3), any(Pageable.class));
    }

    @Test
    @DisplayName("getAdminUserPageData tự động gọi syncAllCustomersMembershipTier để đồng bộ DB")
    void getAdminUserPageData_ShouldTriggerAutoSync() {
        when(tierRepository.findAllByOrderByMinSpentRequiredAsc()).thenReturn(Collections.emptyList());
        when(userRepository.count()).thenReturn(0L);
        when(roleRepository.findAll()).thenReturn(Collections.emptyList());
        when(userRepository.filterUsers(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        userService.getAdminUserPageData(null, null, "ALL", null, 1, 5, "userId", "asc");

        verify(membershipTierService).syncAllCustomersMembershipTier();
    }

    @Test
    @DisplayName("RedirectUtils tạo URL giữ nguyên tham số tierId an toàn")
    void buildUserRedirectUrl_WithTierId_ShouldIncludeTierIdParam() {
        String urlWithTier = RedirectUtils.buildUserRedirectUrl(2, "linh", 1L, "ACTIVE", 4);
        assertTrue(urlWithTier.contains("tierId=4"));
        assertTrue(urlWithTier.contains("page=2"));
        assertTrue(urlWithTier.contains("keyword=linh"));

        String urlWithoutTier = RedirectUtils.buildUserRedirectUrl(1, null, null, null, null);
        assertFalse(urlWithoutTier.contains("tierId"));

        String urlUnranked = RedirectUtils.buildUserRedirectUrl(1, null, null, null, -1);
        assertTrue(urlUnranked.contains("tierId=-1"));
    }
}
