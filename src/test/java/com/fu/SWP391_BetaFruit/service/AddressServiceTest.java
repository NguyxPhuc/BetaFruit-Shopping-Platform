package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.AddressRequest;
import com.fu.SWP391_BetaFruit.dto.response.AddressResponse;
import com.fu.SWP391_BetaFruit.entity.Address;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.AddressType;
import com.fu.SWP391_BetaFruit.repository.AddressRepository;
import com.fu.SWP391_BetaFruit.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử Module Quản Lý Địa Chỉ - AddressServiceTest")
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User sampleUser;
    private AddressRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(100);
        sampleUser.setUsername("testuser");
        sampleUser.setFullName("Nguyễn Văn An");

        sampleRequest = new AddressRequest();
        sampleRequest.setReceiverName("Nguyễn Văn An");
        sampleRequest.setReceiverPhone("0987654321");
        sampleRequest.setCity("Hà Nội");
        sampleRequest.setDistrict("Cầu Giấy");
        sampleRequest.setWard("Dịch Vọng Hậu");
        sampleRequest.setAddressLine("Số 144 Xuân Thủy, Tòa nhà HITC");
        sampleRequest.setIsDefault(false);
        sampleRequest.setAddressType("SHIPPING");
    }

    @Nested
    @DisplayName("1. Kiểm thử Thêm mới địa chỉ (createAddress)")
    class CreateAddressTests {

        @Test
        @DisplayName("TC-ADD-01: Thêm địa chỉ đầu tiên của người dùng -> Phải tự động set làm Mặc định (isDefault = true)")
        void createAddress_FirstAddress_ShouldAutoSetDefault() throws Exception {
            // Given: Người dùng chưa có địa chỉ nào trước đó
            when(userService.getUserById(100)).thenReturn(sampleUser);
            when(addressRepository.countByUser_UserIdAndAddressType(100, AddressType.SHIPPING)).thenReturn(0);

            // When: Thực hiện tạo địa chỉ
            addressService.createAddress(100, sampleRequest, AddressType.SHIPPING);

            // Then: Kiểm tra tham số được lưu
            ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
            verify(addressRepository, times(1)).save(captor.capture());

            Address saved = captor.getValue();
            assertNotNull(saved);
            assertTrue(saved.getIsDefault(), "Địa chỉ đầu tiên bắt buộc phải là mặc định");
            assertTrue(saved.getIsActive(), "Địa chỉ mới tạo phải ở trạng thái Active");
            assertEquals("Nguyễn Văn An", saved.getReceiverName());
            assertEquals("0987654321", saved.getReceiverPhone());
            assertEquals(AddressType.SHIPPING, saved.getAddressType());
            verify(addressRepository, never()).clearDefaultAddress(any(), any());
        }

        @Test
        @DisplayName("TC-ADD-02: Thêm địa chỉ thứ hai và chọn làm Mặc định -> Phải gọi clearDefaultAddress của các địa chỉ cũ")
        void createAddress_SecondAddress_SetDefaultTrue_ShouldClearOldDefault() throws Exception {
            // Given: Đã có 1 địa chỉ trước đó, địa chỉ mới tick chọn Mặc định
            sampleRequest.setIsDefault(true);
            when(userService.getUserById(100)).thenReturn(sampleUser);
            when(addressRepository.countByUser_UserIdAndAddressType(100, AddressType.SHIPPING)).thenReturn(1);

            // When: Tạo địa chỉ
            addressService.createAddress(100, sampleRequest, AddressType.SHIPPING);

            // Then: Phải gọi clearDefaultAddress trước khi lưu địa chỉ mới
            verify(addressRepository, times(1)).clearDefaultAddress(100, AddressType.SHIPPING);
            ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
            verify(addressRepository, times(1)).save(captor.capture());
            assertTrue(captor.getValue().getIsDefault());
        }

        @Test
        @DisplayName("TC-ADD-03: Thêm địa chỉ khi không tìm thấy User -> Bắn ra ngoại lệ Exception")
        void createAddress_UserNotFound_ShouldThrowException() {
            when(userService.getUserById(999)).thenReturn(null);

            Exception exception = assertThrows(Exception.class, () ->
                    addressService.createAddress(999, sampleRequest, AddressType.SHIPPING)
            );

            assertEquals("Không tìm thấy người dùng trong hệ thống.", exception.getMessage());
            verify(addressRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-ADD-04: Thêm địa chỉ cho Shop với forceType = null -> Tự lấy enum từ request.addressType")
        void createAddress_ForShop_WithoutForceType() throws Exception {
            sampleRequest.setAddressType("PICKUP");
            when(userService.getUserById(100)).thenReturn(sampleUser);
            when(addressRepository.countByUser_UserIdAndAddressType(100, AddressType.PICKUP)).thenReturn(0);

            addressService.createAddress(100, sampleRequest, null);

            ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
            verify(addressRepository).save(captor.capture());
            assertEquals(AddressType.PICKUP, captor.getValue().getAddressType());
        }
    }

    @Nested
    @DisplayName("2. Kiểm thử Lấy danh sách địa chỉ theo Role (getAllAddressesByRole)")
    class GetAddressesByRoleTests {

        @Test
        @DisplayName("TC-GET-01: Lấy địa chỉ cho Customer -> Chỉ trả về địa chỉ loại SHIPPING")
        void getAllAddresses_Customer_ReturnsOnlyShipping() throws Exception {
            Address addr = new Address(1, sampleUser, "Nguyễn Văn An", "0987654321",
                    "Số 102 Cầu Giấy", "Hà Nội", "Cầu Giấy", "Quan Hoa", true, true, AddressType.SHIPPING);

            when(addressRepository.getAddressesByUser_UserIdAndIsActiveAndAddressTypeIn(
                    eq(100), eq(true), eq(List.of(AddressType.SHIPPING))
            )).thenReturn(List.of(addr));

            List<AddressResponse> result = addressService.getAllAddressesByRole(100, "customer");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("SHIPPING", result.get(0).getAddressType());
            assertEquals("0987654321", result.get(0).getReceiverPhone());
        }

        @Test
        @DisplayName("TC-GET-02: Lấy địa chỉ cho Shop -> Trả về cả địa chỉ PICKUP và RETURN")
        void getAllAddresses_Shop_ReturnsPickupAndReturn() throws Exception {
            Address pickup = new Address(1, sampleUser, "Kho Tổng", "0912345678",
                    "Cụm CN Lai Xá", "Hà Nội", "Hoài Đức", "Kim Chung", true, true, AddressType.PICKUP);
            Address returnAddr = new Address(2, sampleUser, "Kho Hoàn Hàng", "0912345678",
                    "Số 5 Tân Triều", "Hà Nội", "Thanh Trì", "Tân Triều", false, true, AddressType.RETURN);

            when(addressRepository.getAddressesByUser_UserIdAndIsActiveAndAddressTypeIn(
                    eq(100), eq(true), eq(List.of(AddressType.PICKUP, AddressType.RETURN))
            )).thenReturn(List.of(pickup, returnAddr));

            List<AddressResponse> result = addressService.getAllAddressesByRole(100, "shop");

            assertEquals(2, result.size());
            assertEquals("PICKUP", result.get(0).getAddressType());
            assertEquals("RETURN", result.get(1).getAddressType());
        }

        @Test
        @DisplayName("TC-GET-03: Role rỗng hoặc null -> Bắn ra lỗi")
        void getAllAddresses_InvalidRole_ShouldThrow() {
            assertThrows(Exception.class, () -> addressService.getAllAddressesByRole(100, null));
            assertThrows(Exception.class, () -> addressService.getAllAddressesByRole(100, ""));
        }
    }

    @Nested
    @DisplayName("3. Kiểm thử Cập nhật địa chỉ (updateAddress)")
    class UpdateAddressTests {

        @Test
        @DisplayName("TC-UPD-01: Cập nhật thông tin địa chỉ thành công và đổi sang Mặc định")
        void updateAddress_Success_SetNewDefault() throws Exception {
            Address existing = new Address(10, sampleUser, "Cũ", "0900000000",
                    "Địa chỉ cũ", "Hà Nội", "Đống Đa", "Láng Hạ", false, true, AddressType.SHIPPING);

            when(addressRepository.findByUser_UserIdAndAddressIdAndIsActive(100, 10, true))
                    .thenReturn(Optional.of(existing));

            sampleRequest.setReceiverName("Tên Mới");
            sampleRequest.setReceiverPhone("0988776655");
            sampleRequest.setIsDefault(true);

            addressService.updateAddress(100, 10, sampleRequest, AddressType.SHIPPING);

            // Kiểm tra clear default cũ và cập nhật thông tin mới
            verify(addressRepository, times(1)).clearDefaultAddress(100, AddressType.SHIPPING);
            assertEquals("Tên Mới", existing.getReceiverName());
            assertEquals("0988776655", existing.getReceiverPhone());
            assertTrue(existing.getIsDefault());
            verify(addressRepository, times(1)).save(existing);
        }

        @Test
        @DisplayName("TC-UPD-02: Cập nhật địa chỉ không tồn tại -> Ném ngoại lệ")
        void updateAddress_NotFound_ShouldThrow() {
            when(addressRepository.findByUser_UserIdAndAddressIdAndIsActive(100, 999, true))
                    .thenReturn(Optional.empty());

            Exception ex = assertThrows(Exception.class, () ->
                    addressService.updateAddress(100, 999, sampleRequest, AddressType.SHIPPING)
            );

            assertEquals("Không tìm thấy địa chỉ.", ex.getMessage());
            verify(addressRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("4. Kiểm thử Xóa địa chỉ (deleteAddress - Soft Delete)")
    class DeleteAddressTests {

        @Test
        @DisplayName("TC-DEL-01: Xóa địa chỉ không phải mặc định -> Đổi isActive = false, không ảnh hưởng default")
        void deleteAddress_NonDefault_ShouldDeactivateOnly() throws Exception {
            Address addr = new Address(15, sampleUser, "Địa chỉ phụ", "0987654321",
                    "Ngõ 10", "Hà Nội", "Hà Đông", "Mộ Lao", false, true, AddressType.SHIPPING);

            when(addressRepository.findByUser_UserIdAndAddressIdAndIsActive(100, 15, true))
                    .thenReturn(Optional.of(addr));

            addressService.deleteAddress(100, 15);

            assertFalse(addr.getIsActive(), "Địa chỉ phải chuyển sang không hoạt động (soft delete)");
            assertFalse(addr.getIsDefault());
            verify(addressRepository, times(1)).save(addr);
            verify(addressRepository, never()).findByUser_UserIdAndAddressTypeAndIsActiveTrue(any(), any());
        }

        @Test
        @DisplayName("TC-DEL-02: Xóa địa chỉ ĐANG LÀ MẶC ĐỊNH -> Tự động chỉ định một địa chỉ còn lại lên làm Mặc định mới")
        void deleteAddress_DefaultAddress_ShouldAutoPromoteAnother() throws Exception {
            Address defaultAddr = new Address(20, sampleUser, "Địa chỉ chính", "0987654321",
                    "Ngõ 1", "Hà Nội", "Cầu Giấy", "Dịch Vọng", true, true, AddressType.SHIPPING);

            Address otherAddr = new Address(21, sampleUser, "Địa chỉ cơ quan", "0987654321",
                    "Tòa Keangnam", "Hà Nội", "Nam Từ Liêm", "Mễ Trì", false, true, AddressType.SHIPPING);

            List<Address> remainingList = new ArrayList<>(List.of(otherAddr));

            when(addressRepository.findByUser_UserIdAndAddressIdAndIsActive(100, 20, true))
                    .thenReturn(Optional.of(defaultAddr));
            when(addressRepository.findByUser_UserIdAndAddressTypeAndIsActiveTrue(100, AddressType.SHIPPING))
                    .thenReturn(remainingList);

            addressService.deleteAddress(100, 20);

            // Địa chỉ bị xóa phải tắt active & default
            assertFalse(defaultAddr.getIsActive());
            assertFalse(defaultAddr.getIsDefault());

            // Địa chỉ còn lại (ID 21) phải được phong làm mặc định mới
            assertTrue(otherAddr.getIsDefault(), "Địa chỉ còn lại phải tự động được đưa lên làm mặc định");
            verify(addressRepository).save(otherAddr);
            verify(addressRepository).save(defaultAddr);
        }

        @Test
        @DisplayName("TC-DEL-03: Xóa địa chỉ không thuộc quyền sở hữu của user -> Ném ngoại lệ")
        void deleteAddress_NotFoundOrUnauthorized_ShouldThrow() {
            when(addressRepository.findByUser_UserIdAndAddressIdAndIsActive(100, 999, true))
                    .thenReturn(Optional.empty());

            assertThrows(Exception.class, () -> addressService.deleteAddress(100, 999));
        }
    }
}
