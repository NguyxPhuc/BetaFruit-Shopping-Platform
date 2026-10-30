package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.CouponDashboardResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Kiểm thử Nghiệp vụ: Dashboard Thống Kê Hiệu Quả & Chi Phí Voucher")
class VoucherDashboardServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CustomerMembershipRepository customerMembershipRepository;

    @Mock
    private MembershipTierRepository membershipTierRepository;

    @Mock
    private TierCouponRepository tierCouponRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Shop mockShop;
    private Coupon platformCoupon;
    private Coupon shopCoupon;
    private Order platformOrder;
    private Order shopOrder;

    @BeforeEach
    void setUp() {
        mockShop = new Shop();
        mockShop.setShopId(10);
        mockShop.setShopName("Nông Trại Trái Cây Đà Lạt");

        User owner = new User();
        owner.setUserId(100);
        owner.setUsername("dalat_farm");
        mockShop.setOwner(owner);

        platformCoupon = new Coupon();
        platformCoupon.setCouponId(1);
        platformCoupon.setCouponCode("BETAFREESHIP");
        platformCoupon.setCouponCategory(CouponCategory.SHIPPING);
        platformCoupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        platformCoupon.setDiscountValue(BigDecimal.valueOf(25000));
        platformCoupon.setUsageLimit(100);
        platformCoupon.setUsedCount(15);
        platformCoupon.setIsActive(true);
        platformCoupon.setShop(null); // Sàn BetaFruit tài trợ

        shopCoupon = new Coupon();
        shopCoupon.setCouponId(2);
        shopCoupon.setCouponCode("SHOPGIAM10");
        shopCoupon.setCouponCategory(CouponCategory.PRODUCT);
        shopCoupon.setDiscountType(DiscountType.PERCENT);
        shopCoupon.setDiscountValue(BigDecimal.valueOf(10));
        shopCoupon.setUsageLimit(50);
        shopCoupon.setUsedCount(8);
        shopCoupon.setIsActive(true);
        shopCoupon.setShop(mockShop); // Shop tự tài trợ

        platformOrder = new Order();
        platformOrder.setOrderId(501);
        platformOrder.setCoupon(platformCoupon);
        platformOrder.setDiscountAmount(BigDecimal.valueOf(25000));
        platformOrder.setTotalAmount(BigDecimal.valueOf(300000));
        platformOrder.setShippingFee(BigDecimal.valueOf(25000));
        platformOrder.setFinalAmount(BigDecimal.valueOf(300000));
        platformOrder.setOrderStatus(OrderStatus.SUCCESS);
        platformOrder.setPaymentMethod(PaymentMethod.COD);
        platformOrder.setCreatedAt(LocalDateTime.now().minusDays(1));

        shopOrder = new Order();
        shopOrder.setOrderId(502);
        shopOrder.setCoupon(shopCoupon);
        shopOrder.setShop(mockShop);
        shopOrder.setDiscountAmount(BigDecimal.valueOf(30000));
        shopOrder.setTotalAmount(BigDecimal.valueOf(400000));
        shopOrder.setShippingFee(BigDecimal.valueOf(20000));
        shopOrder.setFinalAmount(BigDecimal.valueOf(390000));
        shopOrder.setOrderStatus(OrderStatus.SUCCESS);
        shopOrder.setPaymentMethod(PaymentMethod.VNPAY);
        shopOrder.setCreatedAt(LocalDateTime.now().minusDays(2));
    }

    @Test
    @DisplayName("Admin Dashboard: Thống kê đầy đủ các chỉ số khi có dữ liệu đơn hàng áp mã")
    void getAdminVoucherDashboardData_SuccessWithOrders() {
        // Arrange
        when(orderRepository.sumTotalVoucherRevenue()).thenReturn(BigDecimal.valueOf(700000));
        when(orderRepository.sumPlatformSponsoredDiscount()).thenReturn(BigDecimal.valueOf(25000));
        when(orderRepository.sumShopSponsoredDiscount()).thenReturn(BigDecimal.valueOf(30000));
        when(orderRepository.countVoucherOrders()).thenReturn(2L);
        when(orderRepository.countTotalValidOrders()).thenReturn(10L);
        when(orderItemRepository.sumTotalCostPriceForVoucherOrders()).thenReturn(BigDecimal.valueOf(350000));

        when(orderRepository.findAllVoucherOrders()).thenReturn(List.of(platformOrder, shopOrder));
        when(couponRepository.findAll()).thenReturn(List.of(platformCoupon, shopCoupon));
        when(orderItemRepository.findTopFruitProductsForVoucherOrders()).thenReturn(Collections.emptyList());
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc()).thenReturn(Collections.emptyList());

        // Act
        CouponDashboardResponse response = couponService.getAdminVoucherDashboardData();

        // Assert
        assertNotNull(response);
        assertNotNull(response.getSummary());

        // 1. Kiểm tra Doanh thu và Chi phí trợ giá
        assertEquals(BigDecimal.valueOf(700000), response.getSummary().getTotalVoucherRevenue());
        assertEquals(BigDecimal.valueOf(55000), response.getSummary().getTotalDiscountAmount());
        assertEquals(BigDecimal.valueOf(25000), response.getSummary().getPlatformDiscountAmount());
        assertEquals(BigDecimal.valueOf(30000), response.getSummary().getShopDiscountAmount());

        // 2. ROI: 700.000 / 55.000 = 12.73
        assertEquals(new BigDecimal("12.73"), response.getSummary().getReturnOnInvestment());

        // 3. Lợi nhuận gộp: 700.000 - 350.000 (cost) - 55.000 (discount) = 295.000
        assertEquals(BigDecimal.valueOf(295000), response.getSummary().getEstimatedGrossProfit());

        // 4. Số đơn & Tỷ lệ đơn: 2 / 10 = 20.0%
        assertEquals(2L, response.getSummary().getVoucherOrderCount());
        assertEquals(10L, response.getSummary().getTotalOrderCount());
        assertEquals(new BigDecimal("20.0"), response.getSummary().getVoucherOrderRate());

        // 5. Giá trị đơn TB (AOV) & Tiết kiệm TB
        assertEquals(BigDecimal.valueOf(350000), response.getSummary().getAverageOrderValue());
        assertEquals(BigDecimal.valueOf(27500), response.getSummary().getAverageCustomerSavings());

        // 6. Leaderboard
        assertNotNull(response.getLeaderboard());
        assertEquals(2, response.getLeaderboard().size());

        // 7. Phân bổ Freeship vs Giảm đơn
        assertEquals(BigDecimal.valueOf(25000), response.getCategoryDistribution().getShippingDiscountTotal());
        assertEquals(BigDecimal.valueOf(30000), response.getCategoryDistribution().getProductDiscountTotal());

        // 8. Phương thức thanh toán COD vs VNPAY
        assertEquals(BigDecimal.valueOf(300000), response.getPaymentMethodDistribution().getCodRevenueTotal());
        assertEquals(BigDecimal.valueOf(400000), response.getPaymentMethodDistribution().getOnlineRevenueTotal());

        // 9. Chart JSON strings
        assertNotNull(response.getTrendLabelsJson());
        assertNotNull(response.getTrendRevenueJson());
        assertNotNull(response.getTrendDiscountJson());
    }

    @Test
    @DisplayName("Admin Dashboard: Xử lý an toàn khi hệ thống chưa có đơn hàng nào (Tránh lỗi chia cho 0)")
    void getAdminVoucherDashboardData_EmptyOrders_SafelyHandlesZeroDivision() {
        // Arrange
        when(orderRepository.sumTotalVoucherRevenue()).thenReturn(BigDecimal.ZERO);
        when(orderRepository.sumPlatformSponsoredDiscount()).thenReturn(BigDecimal.ZERO);
        when(orderRepository.sumShopSponsoredDiscount()).thenReturn(BigDecimal.ZERO);
        when(orderRepository.countVoucherOrders()).thenReturn(0L);
        when(orderRepository.countTotalValidOrders()).thenReturn(0L);
        when(orderItemRepository.sumTotalCostPriceForVoucherOrders()).thenReturn(BigDecimal.ZERO);

        when(orderRepository.findAllVoucherOrders()).thenReturn(Collections.emptyList());
        when(couponRepository.findAll()).thenReturn(Collections.emptyList());
        when(orderItemRepository.findTopFruitProductsForVoucherOrders()).thenReturn(Collections.emptyList());
        when(membershipTierRepository.findAllByOrderByMinSpentRequiredAsc()).thenReturn(Collections.emptyList());

        // Act
        CouponDashboardResponse response = couponService.getAdminVoucherDashboardData();

        // Assert
        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getSummary().getTotalVoucherRevenue());
        assertEquals(BigDecimal.ZERO, response.getSummary().getTotalDiscountAmount());
        assertEquals(BigDecimal.ZERO, response.getSummary().getReturnOnInvestment());
        assertEquals(BigDecimal.ZERO, response.getSummary().getVoucherOrderRate());
        assertEquals(BigDecimal.ZERO, response.getSummary().getAverageOrderValue());
        assertEquals(BigDecimal.ZERO, response.getSummary().getAverageCustomerSavings());
        assertEquals(BigDecimal.ZERO, response.getSummary().getEstimatedGrossProfit());
        assertTrue(response.getLeaderboard().isEmpty());
    }

    @Test
    @DisplayName("Shop Dashboard: Lấy dữ liệu thống kê thành công cho người bán")
    void getShopVoucherDashboardData_Success() {
        // Arrange
        Integer userId = 100;
        when(shopRepository.findByOwner_UserId(userId)).thenReturn(Optional.of(mockShop));

        when(orderRepository.sumTotalVoucherRevenueForShop(mockShop.getShopId())).thenReturn(BigDecimal.valueOf(400000));
        when(orderRepository.sumShopSponsoredDiscountForShop(mockShop.getShopId())).thenReturn(BigDecimal.valueOf(30000));
        when(orderRepository.sumPlatformSponsoredDiscountForShop(mockShop.getShopId())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.countVoucherOrdersForShop(mockShop.getShopId())).thenReturn(1L);
        when(orderRepository.countTotalValidOrdersForShop(mockShop.getShopId())).thenReturn(5L);
        when(orderItemRepository.sumTotalCostPriceForShopVoucherOrders(mockShop.getShopId())).thenReturn(BigDecimal.valueOf(200000));

        when(orderRepository.findAllVoucherOrdersByShopId(mockShop.getShopId())).thenReturn(List.of(shopOrder));
        when(couponRepository.findByShop_ShopId(eq(mockShop.getShopId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(shopCoupon)));
        when(orderItemRepository.findTopFruitProductsForShopVoucherOrders(mockShop.getShopId())).thenReturn(Collections.emptyList());

        // Act
        CouponDashboardResponse response = couponService.getShopVoucherDashboardData(userId);

        // Assert
        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(400000), response.getSummary().getTotalVoucherRevenue());
        assertEquals(BigDecimal.valueOf(30000), response.getSummary().getShopDiscountAmount());
        assertEquals(new BigDecimal("13.33"), response.getSummary().getReturnOnInvestment());
        assertEquals(new BigDecimal("20.0"), response.getSummary().getVoucherOrderRate()); // 1 / 5 = 20%
        // Gross Profit: 400.000 - 200.000 - 30.000 = 170.000
        assertEquals(BigDecimal.valueOf(170000), response.getSummary().getEstimatedGrossProfit());
        assertEquals(1, response.getLeaderboard().size());
        assertEquals("SHOPGIAM10", response.getLeaderboard().get(0).getCouponCode());
    }

    @Test
    @DisplayName("Shop Dashboard: Báo lỗi IllegalArgumentException khi không tìm thấy thông tin cửa hàng")
    void getShopVoucherDashboardData_ShopNotFound_ThrowsException() {
        // Arrange
        Integer invalidUserId = 999;
        when(shopRepository.findByOwner_UserId(invalidUserId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            couponService.getShopVoucherDashboardData(invalidUserId);
        });

        assertEquals("Tài khoản của bạn chưa đăng ký cửa hàng hoặc cửa hàng không tồn tại.", exception.getMessage());
    }
}
