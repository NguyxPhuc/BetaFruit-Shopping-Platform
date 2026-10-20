package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.UserRequest;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.RoleName;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử nghiệp vụ tạo người dùng (createUser) trong UserService")
class UserServiceCreateTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CustomerMembershipRepository customerMembershipRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private MembershipTierRepository tierRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Request null -> ném IllegalArgumentException")
    void createUser_WhenRequestIsNull_ShouldThrowException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(null));
        assertEquals("Dữ liệu người dùng không hợp lệ!", ex.getMessage());
    }

    @Test
    @DisplayName("Tên đăng nhập đã tồn tại -> ném lỗi trùng username")
    void createUser_WhenUsernameExists_ShouldThrowException() {
        UserRequest request = new UserRequest();
        request.setUsername("existinguser");
        request.setEmail("user@example.com");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
        assertTrue(ex.getMessage().contains("Tên đăng nhập 'existinguser' đã tồn tại"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Email đã tồn tại -> ném lỗi trùng email")
    void createUser_WhenEmailExists_ShouldThrowException() {
        UserRequest request = new UserRequest();
        request.setUsername("newuser");
        request.setEmail("duplicate@example.com");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
        assertTrue(ex.getMessage().contains("Email 'duplicate@example.com' đã được sử dụng"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Danh sách vai trò rỗng -> ném lỗi yêu cầu chọn vai trò")
    void createUser_WhenRoleIdsEmpty_ShouldThrowException() {
        UserRequest request = new UserRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setRoleIds(Collections.emptyList());

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
        assertEquals("Vui lòng chọn ít nhất 1 vai trò cho người dùng!", ex.getMessage());
    }

    @Test
    @DisplayName("Vai trò bị tắt (inactive) -> ném lỗi không cho tạo")
    void createUser_WhenRoleIsInactive_ShouldThrowException() {
        UserRequest request = new UserRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setRoleIds(List.of(1L));

        Role inactiveRole = new Role();
        inactiveRole.setRoleId(1L);
        inactiveRole.setRoleName(RoleName.DELIVERY_STAFF);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(roleRepository.findAllById(List.of(1L))).thenReturn(List.of(inactiveRole));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
        assertTrue(ex.getMessage().contains("đang ở trạng thái ẩn hoặc tạm ngưng hoạt động"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo tài khoản CUSTOMER thành công -> Tạo User, Cart và CustomerMembership")
    void createUser_WhenCustomerRole_ShouldCreateCartAndMembership() {
        UserRequest request = new UserRequest();
        request.setUsername("customer1");
        request.setPassword("Password@123");
        request.setFullName("Nguyen Van A");
        request.setEmail("customer1@example.com");
        request.setPhone("0912 345 678");
        request.setRoleIds(List.of(2L));

        Role customerRole = new Role();
        customerRole.setRoleId(2L);
        customerRole.setRoleName(RoleName.CUSTOMER);

        MembershipTier defaultTier = new MembershipTier();
        defaultTier.setTierId(1);
        defaultTier.setTierName("Bronze");
        defaultTier.setMinSpentRequired(BigDecimal.ZERO);

        when(userRepository.existsByUsername("customer1")).thenReturn(false);
        when(userRepository.existsByEmail("customer1@example.com")).thenReturn(false);
        when(roleRepository.findAllById(List.of(2L))).thenReturn(List.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tierRepository.findTopByOrderByMinSpentRequiredAsc()).thenReturn(defaultTier);

        assertDoesNotThrow(() -> userService.createUser(request));

        verify(userRepository).save(any(User.class));
        verify(cartRepository).save(any(Cart.class));
        verify(customerMembershipRepository).save(any(CustomerMembership.class));
    }

    @Test
    @DisplayName("Tạo tài khoản ADMIN thành công -> Lưu User, không tạo Cart và Membership")
    void createUser_WhenAdminRole_ShouldNotCreateCartOrMembership() {
        UserRequest request = new UserRequest();
        request.setUsername("admin1");
        request.setPassword("AdminPass@123");
        request.setFullName("Quản Trị Viên");
        request.setEmail("admin1@example.com");
        request.setRoleIds(List.of(3L));

        Role adminRole = new Role();
        adminRole.setRoleId(3L);
        adminRole.setRoleName(RoleName.ADMIN);

        when(userRepository.existsByUsername("admin1")).thenReturn(false);
        when(userRepository.existsByEmail("admin1@example.com")).thenReturn(false);
        when(roleRepository.findAllById(List.of(3L))).thenReturn(List.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> userService.createUser(request));

        verify(userRepository).save(any(User.class));
        verify(cartRepository, never()).save(any(Cart.class));
        verify(customerMembershipRepository, never()).save(any(CustomerMembership.class));
    }
}
