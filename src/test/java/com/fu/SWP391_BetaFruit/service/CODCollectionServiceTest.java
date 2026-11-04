package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.impl.CODCollectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử Nghiệp vụ: Thu & Đối Soát COD (FE-06.4 COD Remittance)")
class CODCollectionServiceTest {

    @Mock
    private CODCollectionRepository codCollectionRepository;

    @Mock
    private CODDetailRepository codDetailRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private ShipperRepository shipperRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CODCollectionServiceImpl codCollectionService;

    private User mockStaff;
    private Shipper mockShipper;
    private Order mockCodOrder1;
    private Order mockCodOrder2;

    @BeforeEach
    void setUp() {
        mockStaff = new User();
        mockStaff.setUserId(2);
        mockStaff.setFullName("Staff Minh");

        User shipperUser = new User();
        shipperUser.setUserId(6);
        shipperUser.setFullName("Shipper Nam");

        mockShipper = new Shipper();
        mockShipper.setUserId(6);
        mockShipper.setUser(shipperUser);
        mockShipper.setCodDebt(new BigDecimal("1000000"));

        mockCodOrder1 = new Order();
        mockCodOrder1.setOrderId(101);
        mockCodOrder1.setFinalAmount(new BigDecimal("350000"));
        mockCodOrder1.setPaymentMethod(PaymentMethod.COD);
        mockCodOrder1.setOrderStatus(OrderStatus.DELIVERED);
        mockCodOrder1.setIsCodRemitted(false);

        mockCodOrder2 = new Order();
        mockCodOrder2.setOrderId(102);
        mockCodOrder2.setFinalAmount(new BigDecimal("250000"));
        mockCodOrder2.setPaymentMethod(PaymentMethod.COD);
        mockCodOrder2.setOrderStatus(OrderStatus.DELIVERED);
        mockCodOrder2.setIsCodRemitted(false);
    }

    @Test
    @DisplayName("Nộp COD thành công: Tạo phiếu thu, tạo chi tiết đơn, cấn trừ công nợ Shipper chính xác")
    void remitCodCollection_Success() {
        when(userRepository.findById(2)).thenReturn(Optional.of(mockStaff));
        when(shipperRepository.findById(6)).thenReturn(Optional.of(mockShipper));
        when(orderRepository.findById(101)).thenReturn(Optional.of(mockCodOrder1));
        when(orderRepository.findById(102)).thenReturn(Optional.of(mockCodOrder2));

        CODCollection savedCollection = new CODCollection();
        savedCollection.setCollectionId(99);
        savedCollection.setShipper(mockShipper);
        savedCollection.setCollectedBy(mockStaff);
        savedCollection.setAmountCollected(new BigDecimal("600000"));

        when(codCollectionRepository.save(any(CODCollection.class))).thenReturn(savedCollection);

        CODCollection result = codCollectionService.remitCodCollection(2, 6, List.of(101, 102));

        assertNotNull(result);
        assertEquals(99, result.getCollectionId());
        assertEquals(new BigDecimal("600000"), result.getAmountCollected());

        // Kiểm tra trạng thái đơn được đánh dấu đã nộp COD
        assertTrue(mockCodOrder1.getIsCodRemitted());
        assertTrue(mockCodOrder2.getIsCodRemitted());
        verify(orderRepository, times(1)).save(mockCodOrder1);
        verify(orderRepository, times(1)).save(mockCodOrder2);

        // Kiểm tra chi tiết phiếu thu được lưu
        verify(codDetailRepository, times(2)).save(any(CODDetail.class));

        // Kiểm tra công nợ shipper bị trừ chính xác: 1.000.000 - 600.000 = 400.000
        assertEquals(new BigDecimal("400000"), mockShipper.getCodDebt());
        verify(shipperRepository, times(1)).save(mockShipper);

        // Kiểm tra thông báo được gửi cho Shipper
        verify(notificationService).createBaseNotification(eq(mockShipper.getUser()), any(), any(), any(), eq("/shipper/orders/cod-history"));
    }

    @Test
    @DisplayName("Nộp COD thất bại khi đơn hàng đã được nộp trước đó")
    void remitCodCollection_AlreadyRemitted_ThrowsException() {
        mockCodOrder1.setIsCodRemitted(true);

        when(userRepository.findById(2)).thenReturn(Optional.of(mockStaff));
        when(shipperRepository.findById(6)).thenReturn(Optional.of(mockShipper));
        when(orderRepository.findById(101)).thenReturn(Optional.of(mockCodOrder1));

        assertThrows(ResponseStatusException.class, () ->
                codCollectionService.remitCodCollection(2, 6, List.of(101))
        );

        verify(codCollectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nộp COD thất bại khi đơn hàng thanh toán online VNPay")
    void remitCodCollection_NonCodOrder_ThrowsException() {
        mockCodOrder1.setPaymentMethod(PaymentMethod.VNPAY);

        when(userRepository.findById(2)).thenReturn(Optional.of(mockStaff));
        when(shipperRepository.findById(6)).thenReturn(Optional.of(mockShipper));
        when(orderRepository.findById(101)).thenReturn(Optional.of(mockCodOrder1));

        assertThrows(ResponseStatusException.class, () ->
                codCollectionService.remitCodCollection(2, 6, List.of(101))
        );

        verify(codCollectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nộp COD thất bại khi danh sách đơn rỗng")
    void remitCodCollection_EmptyList_ThrowsException() {
        assertThrows(ResponseStatusException.class, () ->
                codCollectionService.remitCodCollection(2, 6, List.of())
        );
    }

    @Test
    @DisplayName("Lấy số dư công nợ của tài xế")
    void getShipperDebt_ReturnsCorrectAmount() {
        when(shipperRepository.findById(6)).thenReturn(Optional.of(mockShipper));

        BigDecimal debt = codCollectionService.getShipperDebt(6);
        assertEquals(new BigDecimal("1000000"), debt);
    }
}
