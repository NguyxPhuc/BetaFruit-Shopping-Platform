package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.ShopAnalyticsResponse;
import com.fu.SWP391_BetaFruit.dto.response.TopSellingProductResponse;
import com.fu.SWP391_BetaFruit.entity.Product;
import com.fu.SWP391_BetaFruit.entity.ProductImage;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.OrderItemRepository;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.ProductRepository;
import com.fu.SWP391_BetaFruit.repository.ProductVariantRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.service.ShopAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopAnalyticsServiceImpl implements ShopAnalyticsService {

    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public ShopAnalyticsResponse getShopAnalyticsForOwner(Integer ownerUserId) {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("Không xác định được ID tài khoản chủ cửa hàng.");
        }

        Shop shop = shopRepository.findByOwner_UserId(ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin gian hàng liên kết với tài khoản #" + ownerUserId));

        Integer shopId = shop.getShopId();

        List<OrderStatus> completedStatuses = List.of(OrderStatus.SUCCESS, OrderStatus.DELIVERED);
        List<OrderStatus> processingStatuses = List.of(
                OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
                OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.DELIVERING
        );
        List<OrderStatus> cancelledStatuses = List.of(
                OrderStatus.CANCELLED, OrderStatus.FAILED, OrderStatus.REJECTED
        );
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
                OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.DELIVERING,
                OrderStatus.DELIVERED, OrderStatus.SUCCESS
        );

        // 1. DOANH THU & PHÍ SÀN
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalPlatformFee = BigDecimal.ZERO;
        try {
            totalRevenue = orderRepository.sumTotalRevenueByShopAndStatuses(shopId, completedStatuses);
            totalPlatformFee = orderRepository.sumTotalPlatformFeeByShopAndStatuses(shopId, completedStatuses);
        } catch (Exception e) {
            log.warn("Lỗi tính doanh thu hoàn tất cho shop #{}: {}", shopId, e.getMessage());
        }

        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        if (totalPlatformFee == null) totalPlatformFee = BigDecimal.ZERO;

        if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) {
            try {
                BigDecimal actRev = orderRepository.sumTotalRevenueByShopAndStatuses(shopId, activeStatuses);
                if (actRev != null && actRev.compareTo(BigDecimal.ZERO) > 0) {
                    totalRevenue = actRev;
                    BigDecimal actFee = orderRepository.sumTotalPlatformFeeByShopAndStatuses(shopId, activeStatuses);
                    if (actFee != null) totalPlatformFee = actFee;
                }
            } catch (Exception ignored) {}
        }

        BigDecimal netRevenue = totalRevenue.subtract(totalPlatformFee);
        if (netRevenue.compareTo(BigDecimal.ZERO) < 0) netRevenue = BigDecimal.ZERO;

        // 2. ĐƠN HÀNG
        long totalOrders = 0;
        long completedOrders = 0;
        long processingOrders = 0;
        long cancelledOrders = 0;
        try {
            totalOrders = orderRepository.countByShop_ShopId(shopId);
            completedOrders = orderRepository.countByShop_ShopIdAndOrderStatusIn(shopId, completedStatuses);
            processingOrders = orderRepository.countByShop_ShopIdAndOrderStatusIn(shopId, processingStatuses);
            cancelledOrders = orderRepository.countByShop_ShopIdAndOrderStatusIn(shopId, cancelledStatuses);
        } catch (Exception e) {
            log.warn("Lỗi đếm số lượng đơn cho shop #{}: {}", shopId, e.getMessage());
        }

        double successRate = (totalOrders > 0) ? ((double) completedOrders / totalOrders) * 100.0 : 0.0;
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (completedOrders > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(completedOrders), 0, RoundingMode.HALF_UP);
        } else if (totalOrders > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, RoundingMode.HALF_UP);
        }

        // 3. KHO HÀNG & KHÁCH HÀNG
        long productCount = 0;
        long totalStock = 0;
        long lowStockCount = 0;
        long totalCustomers = 0;
        try {
            productCount = productRepository.countByShop_ShopId(shopId);
            totalStock = productVariantRepository.sumStockQuantityByShopId(shopId);
            lowStockCount = productVariantRepository.countLowStockVariantsByShopId(shopId);
            totalCustomers = orderRepository.countDistinctCustomerByShopId(shopId);
        } catch (Exception e) {
            log.warn("Lỗi đếm kho hàng/khách hàng cho shop #{}: {}", shopId, e.getMessage());
        }

        // 4. TOP 5 HOA QUẢ BÁN CHẠY NHẤT
        List<TopSellingProductResponse> topSellingProducts = new ArrayList<>();
        Map<Integer, Long> soldMap = new HashMap<>();
        Map<Integer, BigDecimal> revMap = new HashMap<>();

        try {
            List<Object[]> rows = orderItemRepository.findTopSellingProductStatsByShop(shopId, completedStatuses, PageRequest.of(0, 50));
            if (rows == null || rows.isEmpty()) {
                rows = orderItemRepository.findTopSellingProductStatsByShop(shopId, activeStatuses, PageRequest.of(0, 50));
            }
            if (rows != null) {
                for (Object[] r : rows) {
                    Integer prodId = (Integer) r[0];
                    Long soldQty = (r[1] instanceof Number) ? ((Number) r[1]).longValue() : 0L;
                    BigDecimal prodRev = (r[2] instanceof BigDecimal) ? (BigDecimal) r[2] : (r[2] instanceof Number ? BigDecimal.valueOf(((Number) r[2]).doubleValue()) : BigDecimal.ZERO);
                    soldMap.put(prodId, soldQty);
                    revMap.put(prodId, prodRev);
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi truy vấn doanh số sản phẩm shop #{}: {}", shopId, e.getMessage());
        }

        // Tạo Top 5 dựa trên soldMap
        List<Map.Entry<Integer, Long>> sortedSold = new ArrayList<>(soldMap.entrySet());
        sortedSold.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        int rank = 1;
        Set<Integer> topIds = new HashSet<>();
        for (Map.Entry<Integer, Long> entry : sortedSold) {
            if (topSellingProducts.size() >= 5) break;
            Integer pId = entry.getKey();
            Product p = productRepository.findById(pId).orElse(null);
            if (p != null) {
                topIds.add(pId);
                int pStock = 0;
                try {
                    pStock = productVariantRepository.findByProduct_ProductId(pId).stream()
                            .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                            .sum();
                } catch (Exception ignored) {}

                topSellingProducts.add(TopSellingProductResponse.builder()
                        .rank(rank++)
                        .productId(pId)
                        .productName(p.getProductName())
                        .imageUrl(resolveProductImage(p))
                        .categoryName(p.getCategory() != null ? p.getCategory().getCategoryName() : "Hoa Quả")
                        .shopName(shop.getShopName())
                        .shopId(shop.getShopId())
                        .totalQuantitySold(entry.getValue())
                        .totalRevenue(revMap.getOrDefault(pId, BigDecimal.ZERO))
                        .totalStock(pStock)
                        .build());
            }
        }

        // 5. TOÀN BỘ SẢN PHẨM CỦA GIAN HÀNG (ALL PRODUCTS BREAKDOWN)
        List<TopSellingProductResponse> allProducts = new ArrayList<>();
        try {
            List<Product> shopProds = productRepository.findByShop_ShopIdOrderByProductIdDesc(shopId);
            int allRank = 1;
            for (Product p : shopProds) {
                if (p == null) continue;
                Integer pId = p.getProductId();
                long soldQty = soldMap.getOrDefault(pId, 0L);
                BigDecimal prodRev = revMap.getOrDefault(pId, BigDecimal.ZERO);
                int pStock = 0;
                try {
                    pStock = productVariantRepository.findByProduct_ProductId(pId).stream()
                            .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                            .sum();
                } catch (Exception ignored) {}

                allProducts.add(TopSellingProductResponse.builder()
                        .rank(allRank++)
                        .productId(pId)
                        .productName(p.getProductName())
                        .imageUrl(resolveProductImage(p))
                        .categoryName(p.getCategory() != null ? p.getCategory().getCategoryName() : "Hoa Quả")
                        .shopName(shop.getShopName())
                        .shopId(shop.getShopId())
                        .totalQuantitySold(soldQty)
                        .totalRevenue(prodRev)
                        .totalStock(pStock)
                        .build());
            }

            // Nếu topSellingProducts chưa đủ 5 sản phẩm (do đơn hàng ít hoặc chưa có đơn), bổ sung từ danh sách sản phẩm của shop
            if (topSellingProducts.size() < 5) {
                for (TopSellingProductResponse ap : allProducts) {
                    if (topSellingProducts.size() >= 5) break;
                    if (!topIds.contains(ap.getProductId())) {
                        topSellingProducts.add(TopSellingProductResponse.builder()
                                .rank(rank++)
                                .productId(ap.getProductId())
                                .productName(ap.getProductName())
                                .imageUrl(ap.getImageUrl())
                                .categoryName(ap.getCategoryName())
                                .shopName(ap.getShopName())
                                .shopId(ap.getShopId())
                                .totalQuantitySold(ap.getTotalQuantitySold())
                                .totalRevenue(ap.getTotalRevenue())
                                .totalStock(ap.getTotalStock())
                                .build());
                        topIds.add(ap.getProductId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi tải toàn bộ sản phẩm cho shop #{}: {}", shopId, e.getMessage());
        }

        String ownerName = (shop.getOwner() != null && shop.getOwner().getFullName() != null)
                ? shop.getOwner().getFullName() : "N/A";
        String ownerPhone = (shop.getOwner() != null && shop.getOwner().getPhone() != null)
                ? shop.getOwner().getPhone() : "";

        return ShopAnalyticsResponse.builder()
                .shopId(shop.getShopId())
                .shopName(shop.getShopName())
                .ownerName(ownerName)
                .ownerPhone(ownerPhone)
                .shopStatus(shop.getApprovalStatus() != null ? shop.getApprovalStatus().name() : "APPROVED")
                .availableBalance(shop.getAvailableBalance() != null ? shop.getAvailableBalance() : BigDecimal.ZERO)
                .totalRevenue(totalRevenue)
                .totalPlatformFee(totalPlatformFee)
                .netRevenue(netRevenue)
                .averageOrderValue(averageOrderValue)
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .processingOrders(processingOrders)
                .cancelledOrders(cancelledOrders)
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .productCount(productCount)
                .totalStockQuantity(totalStock)
                .lowStockCount(lowStockCount)
                .totalCustomers(totalCustomers)
                .topSellingProducts(topSellingProducts)
                .allProducts(allProducts)
                .build();
    }

    private String resolveProductImage(Product product) {
        if (product == null) return "/images/default-fruit.png";
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(product.getImages().get(0).getImageUrl());
        }
        return "/images/default-fruit.png";
    }
}
