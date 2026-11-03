package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.SystemAnalyticsResponse;
import com.fu.SWP391_BetaFruit.dto.response.TopSellingProductResponse;
import com.fu.SWP391_BetaFruit.entity.Product;
import com.fu.SWP391_BetaFruit.entity.ProductImage;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.RoleName;
import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;
import com.fu.SWP391_BetaFruit.repository.OrderItemRepository;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.ProductRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public SystemAnalyticsResponse getSystemAnalytics() {
        // 1. TÍNH TOÁN DOANH THU TOÀN SÀN (TOTAL REVENUE & PLATFORM FEE)
        List<OrderStatus> completedStatuses = List.of(OrderStatus.SUCCESS, OrderStatus.DELIVERED);
        BigDecimal totalRevenue = orderRepository.sumTotalRevenueByStatuses(completedStatuses);
        BigDecimal totalPlatformFee = orderRepository.sumTotalPlatformFeeByStatuses(completedStatuses);

        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }
        if (totalPlatformFee == null) {
            totalPlatformFee = BigDecimal.ZERO;
        }

        // Hỗ trợ hiển thị trong môi trường kiểm thử/demo: nếu chưa có đơn hoàn tất, tính tổng đơn đang xử lý
        if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) {
            List<OrderStatus> activeStatuses = List.of(
                    OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
                    OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.DELIVERING,
                    OrderStatus.DELIVERED, OrderStatus.SUCCESS
            );
            BigDecimal activeRevenue = orderRepository.sumTotalRevenueByStatuses(activeStatuses);
            if (activeRevenue != null && activeRevenue.compareTo(BigDecimal.ZERO) > 0) {
                totalRevenue = activeRevenue;
                BigDecimal activePlatformFee = orderRepository.sumTotalPlatformFeeByStatuses(activeStatuses);
                if (activePlatformFee != null) {
                    totalPlatformFee = activePlatformFee;
                }
            }
        }

        // 2. TÍNH TOÁN SỐ LƯỢNG ĐƠN HÀNG (TOTAL ORDERS & PHÂN LOẠI)
        long totalOrders = orderRepository.count();
        long completedOrders = orderRepository.countByOrderStatusIn(completedStatuses);
        long processingOrders = orderRepository.countByOrderStatusIn(List.of(
                OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
                OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.DELIVERING
        ));
        long cancelledOrders = orderRepository.countByOrderStatusIn(List.of(
                OrderStatus.CANCELLED, OrderStatus.FAILED, OrderStatus.REJECTED
        ));

        // Giá trị trung bình đơn hàng (AOV)
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        long successfulDivisor = completedOrders > 0 ? completedOrders : (totalOrders - cancelledOrders);
        if (successfulDivisor > 0 && totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(successfulDivisor), 0, RoundingMode.HALF_UP);
        }

        // 3. TÍNH TOÁN CHỦ CỬA HÀNG (TOTAL SHOP OWNERS & SHOPS)
        long totalShopOwners = userRepository.countByRoleName(RoleName.SHOP_OWNER);
        long approvedShops = shopRepository.countByApprovalStatus(ShopApprovalStatus.APPROVED);

        // 4. TÍNH TOÁN KHÁCH HÀNG (TOTAL CUSTOMERS & USERS)
        long totalCustomers = userRepository.countByRoleName(RoleName.CUSTOMER);
        long totalUsers = userRepository.count();

        // 5. TRUY VẤN TOP 5 SẢN PHẨM BÁN CHẠY NHẤT (TOP 5 BEST SELLING PRODUCTS)
        List<Object[]> rows = orderItemRepository.findTopSellingProductStats(completedStatuses, PageRequest.of(0, 5));
        if (rows == null || rows.isEmpty()) {
            List<OrderStatus> fallbackStatuses = List.of(
                    OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
                    OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.DELIVERING,
                    OrderStatus.DELIVERED, OrderStatus.SUCCESS
            );
            rows = orderItemRepository.findTopSellingProductStats(fallbackStatuses, PageRequest.of(0, 5));
        }

        List<TopSellingProductResponse> topProducts = new ArrayList<>();
        if (rows != null && !rows.isEmpty()) {
            int rank = 1;
            for (Object[] row : rows) {
                if (row[0] == null) {
                    continue;
                }
                Integer productId = ((Number) row[0]).intValue();
                Long quantitySold = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                BigDecimal revenue = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;

                Product product = productRepository.findById(productId).orElse(null);
                if (product == null) {
                    continue;
                }

                String categoryName = product.getCategory() != null ? product.getCategory().getCategoryName() : "Trái Cây";
                String shopName = product.getShop() != null ? product.getShop().getShopName() : "Gian hàng";

                // Lấy ảnh đại diện chính của sản phẩm
                String imageUrl = "/images/default-fruit.png";
                if (product.getImages() != null && !product.getImages().isEmpty()) {
                    imageUrl = product.getImages().stream()
                            .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                            .map(ProductImage::getImageUrl)
                            .findFirst()
                            .orElse(product.getImages().get(0).getImageUrl());
                }

                // Tính tổng tồn kho hiện tại qua các biến thể
                int totalStock = 0;
                if (product.getVariants() != null) {
                    totalStock = product.getVariants().stream()
                            .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                            .sum();
                }

                topProducts.add(TopSellingProductResponse.builder()
                        .rank(rank++)
                        .productId(productId)
                        .productName(product.getProductName())
                        .categoryName(categoryName)
                        .shopName(shopName)
                        .imageUrl(imageUrl)
                        .totalQuantitySold(quantitySold)
                        .totalRevenue(revenue)
                        .totalStock(totalStock)
                        .build());
            }
        }

        return SystemAnalyticsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalPlatformFee(totalPlatformFee)
                .averageOrderValue(averageOrderValue)
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .processingOrders(processingOrders)
                .cancelledOrders(cancelledOrders)
                .totalShopOwners(totalShopOwners)
                .approvedShops(approvedShops)
                .totalCustomers(totalCustomers)
                .totalUsers(totalUsers)
                .topSellingProducts(topProducts)
                .build();
    }
}
