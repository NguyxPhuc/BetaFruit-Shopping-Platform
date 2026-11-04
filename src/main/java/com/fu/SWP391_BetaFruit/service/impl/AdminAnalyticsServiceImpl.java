package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.ShopAnalyticsSummaryResponse;
import com.fu.SWP391_BetaFruit.dto.response.SystemAnalyticsResponse;
import com.fu.SWP391_BetaFruit.dto.response.TopSellingProductResponse;
import com.fu.SWP391_BetaFruit.entity.Product;
import com.fu.SWP391_BetaFruit.entity.ProductImage;
import com.fu.SWP391_BetaFruit.entity.Shop;
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
import java.util.Comparator;
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
        return getSystemAnalytics(null);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemAnalyticsResponse getSystemAnalytics(Integer shopId) {
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

        Shop targetShop = null;
        if (shopId != null) {
            targetShop = shopRepository.findById(shopId).orElse(null);
            if (targetShop == null) {
                log.warn("Không tìm thấy Shop với ID: {}, tự động chuyển về thống kê toàn sàn.", shopId);
                shopId = null;
            }
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalPlatformFee = BigDecimal.ZERO;
        long totalOrders = 0;
        long completedOrders = 0;
        long processingOrders = 0;
        long cancelledOrders = 0;
        long totalShopOwners = 0;
        long approvedShops = 0;
        long shopProductCount = 0;
        long totalCustomers = 0;
        long totalUsers = 0;
        try {
            totalUsers = userRepository.count();
        } catch (Exception ignored) {}

        List<Object[]> rows = new ArrayList<>();

        // ══════════════════════════════════════════════════════════════════════════
        // TRƯỜNG HỢP 1: XEM CHI TIẾT THEO TỪNG GIAN HÀNG CỤ THỂ (PER SHOP)
        // ══════════════════════════════════════════════════════════════════════════
        if (shopId != null && targetShop != null) {
            try {
                totalRevenue = orderRepository.sumTotalRevenueByShopAndStatuses(shopId, completedStatuses);
                totalPlatformFee = orderRepository.sumTotalPlatformFeeByShopAndStatuses(shopId, completedStatuses);
            } catch (Exception e) {
                log.warn("Lỗi tính doanh thu shop #{}: {}", shopId, e.getMessage());
            }

            if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
            if (totalPlatformFee == null) totalPlatformFee = BigDecimal.ZERO;

            if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) {
                try {
                    BigDecimal activeRev = orderRepository.sumTotalRevenueByShopAndStatuses(shopId, activeStatuses);
                    if (activeRev != null && activeRev.compareTo(BigDecimal.ZERO) > 0) {
                        totalRevenue = activeRev;
                        BigDecimal activeFee = orderRepository.sumTotalPlatformFeeByShopAndStatuses(shopId, activeStatuses);
                        if (activeFee != null) totalPlatformFee = activeFee;
                    }
                } catch (Exception ignored) {}
            }

            try {
                totalOrders = orderRepository.countByShop_ShopId(shopId);
                completedOrders = orderRepository.countByShop_ShopIdAndOrderStatusIn(shopId, completedStatuses);
                processingOrders = orderRepository.countByShop_ShopIdAndOrderStatusIn(shopId, processingStatuses);
                cancelledOrders = orderRepository.countByShop_ShopIdAndOrderStatusIn(shopId, cancelledStatuses);
                shopProductCount = productRepository.countByShop_ShopId(shopId);
                totalCustomers = orderRepository.countDistinctCustomerByShopId(shopId);
            } catch (Exception e) {
                log.warn("Lỗi đếm số lượng đơn shop #{}: {}", shopId, e.getMessage());
            }

            totalShopOwners = 1;
            approvedShops = (targetShop.getApprovalStatus() == ShopApprovalStatus.APPROVED) ? 1 : 0;

            try {
                rows = orderItemRepository.findTopSellingProductStatsByShop(shopId, completedStatuses, PageRequest.of(0, 5));
                if (rows == null || rows.isEmpty()) {
                    rows = orderItemRepository.findTopSellingProductStatsByShop(shopId, activeStatuses, PageRequest.of(0, 5));
                }
            } catch (Exception e) {
                log.warn("Lỗi truy vấn top sản phẩm bán chạy shop #{}: {}", shopId, e.getMessage());
            }
        }
        // ══════════════════════════════════════════════════════════════════════════
        // TRƯỜNG HỢP 2: XEM TỔNG QUAN TOÀN BỘ SÀN (PLATFORM-WIDE)
        // ══════════════════════════════════════════════════════════════════════════
        else {
            try {
                totalRevenue = orderRepository.sumTotalRevenueByStatuses(completedStatuses);
                totalPlatformFee = orderRepository.sumTotalPlatformFeeByStatuses(completedStatuses);
            } catch (Exception e) {
                log.warn("Lỗi tính doanh thu toàn sàn: {}", e.getMessage());
            }

            if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
            if (totalPlatformFee == null) totalPlatformFee = BigDecimal.ZERO;

            if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) {
                try {
                    BigDecimal activeRev = orderRepository.sumTotalRevenueByStatuses(activeStatuses);
                    if (activeRev != null && activeRev.compareTo(BigDecimal.ZERO) > 0) {
                        totalRevenue = activeRev;
                        BigDecimal activeFee = orderRepository.sumTotalPlatformFeeByStatuses(activeStatuses);
                        if (activeFee != null) totalPlatformFee = activeFee;
                    }
                } catch (Exception ignored) {}
            }

            try {
                totalOrders = orderRepository.count();
                completedOrders = orderRepository.countByOrderStatusIn(completedStatuses);
                processingOrders = orderRepository.countByOrderStatusIn(processingStatuses);
                cancelledOrders = orderRepository.countByOrderStatusIn(cancelledStatuses);
                totalShopOwners = userRepository.countByRoleName(RoleName.SHOP_OWNER);
                approvedShops = shopRepository.countByApprovalStatus(ShopApprovalStatus.APPROVED);
                totalCustomers = userRepository.countByRoleName(RoleName.CUSTOMER);
            } catch (Exception e) {
                log.warn("Lỗi đếm số lượng tổng quan: {}", e.getMessage());
            }

            try {
                rows = orderItemRepository.findTopSellingProductStats(completedStatuses, PageRequest.of(0, 5));
                if (rows == null || rows.isEmpty()) {
                    rows = orderItemRepository.findTopSellingProductStats(activeStatuses, PageRequest.of(0, 5));
                }
            } catch (Exception e) {
                log.warn("Lỗi truy vấn top sản phẩm bán chạy toàn sàn: {}", e.getMessage());
            }
        }

        // Tính AOV và Tỷ lệ thành công an toàn
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        long successfulDivisor = completedOrders > 0 ? completedOrders : (totalOrders - cancelledOrders);
        if (successfulDivisor > 0 && totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(successfulDivisor), 0, RoundingMode.HALF_UP);
        }

        double successRate = (totalOrders > 0) ? ((double) completedOrders / totalOrders) * 100.0 : 0.0;
        BigDecimal netRevenue = totalRevenue.subtract(totalPlatformFee);
        if (netRevenue.compareTo(BigDecimal.ZERO) < 0) {
            netRevenue = BigDecimal.ZERO;
        }

        // Xử lý danh sách Top 5 sản phẩm
        List<TopSellingProductResponse> topProducts = extractTopSellingProducts(rows);

        // ══════════════════════════════════════════════════════════════════════════
        // TỔNG HỢP BẢNG THỐNG KÊ TOÀN BỘ CÁC GIAN HÀNG (SHOP BREAKDOWN TABLE)
        // ══════════════════════════════════════════════════════════════════════════
        List<ShopAnalyticsSummaryResponse> shopSummaries = new ArrayList<>();
        try {
            List<Shop> allShops = shopRepository.findAllWithOwners();
            for (Shop s : allShops) {
                if (s == null) continue;

                BigDecimal sRev = BigDecimal.ZERO;
                BigDecimal sFee = BigDecimal.ZERO;
                try {
                    sRev = orderRepository.sumTotalRevenueByShopAndStatuses(s.getShopId(), completedStatuses);
                    sFee = orderRepository.sumTotalPlatformFeeByShopAndStatuses(s.getShopId(), completedStatuses);
                } catch (Exception ignored) {}

                if (sRev == null) sRev = BigDecimal.ZERO;
                if (sFee == null) sFee = BigDecimal.ZERO;

                if (sRev.compareTo(BigDecimal.ZERO) == 0) {
                    try {
                        BigDecimal actRev = orderRepository.sumTotalRevenueByShopAndStatuses(s.getShopId(), activeStatuses);
                        if (actRev != null && actRev.compareTo(BigDecimal.ZERO) > 0) {
                            sRev = actRev;
                            BigDecimal actFee = orderRepository.sumTotalPlatformFeeByShopAndStatuses(s.getShopId(), activeStatuses);
                            if (actFee != null) sFee = actFee;
                        }
                    } catch (Exception ignored) {}
                }

                long sTotalOrders = 0;
                long sCompleted = 0;
                long sCancelled = 0;
                long sProdCount = 0;
                try {
                    sTotalOrders = orderRepository.countByShop_ShopId(s.getShopId());
                    sCompleted = orderRepository.countByShop_ShopIdAndOrderStatusIn(s.getShopId(), completedStatuses);
                    sCancelled = orderRepository.countByShop_ShopIdAndOrderStatusIn(s.getShopId(), cancelledStatuses);
                    sProdCount = productRepository.countByShop_ShopId(s.getShopId());
                } catch (Exception ignored) {}

                double sRate = (sTotalOrders > 0) ? ((double) sCompleted / sTotalOrders) * 100.0 : 0.0;
                BigDecimal sNet = sRev.subtract(sFee);
                if (sNet.compareTo(BigDecimal.ZERO) < 0) sNet = BigDecimal.ZERO;

                String ownerName = "N/A";
                String ownerPhone = "";
                try {
                    if (s.getOwner() != null) {
                        if (s.getOwner().getFullName() != null) ownerName = s.getOwner().getFullName();
                        if (s.getOwner().getPhone() != null) ownerPhone = s.getOwner().getPhone();
                    }
                } catch (Exception ignored) {}

                shopSummaries.add(ShopAnalyticsSummaryResponse.builder()
                        .shopId(s.getShopId())
                        .shopName(s.getShopName() != null ? s.getShopName() : "Gian hàng #" + s.getShopId())
                        .ownerName(ownerName)
                        .ownerPhone(ownerPhone)
                        .status(s.getApprovalStatus() != null ? s.getApprovalStatus().name() : "DRAFT")
                        .totalRevenue(sRev)
                        .totalPlatformFee(sFee)
                        .netRevenue(sNet)
                        .totalOrders(sTotalOrders)
                        .completedOrders(sCompleted)
                        .cancelledOrders(sCancelled)
                        .successRate(Math.round(sRate * 10.0) / 10.0)
                        .productCount(sProdCount)
                        .build());
            }

            // Sắp xếp các Shop theo Doanh thu bán hàng giảm dần
            shopSummaries.sort(Comparator.comparing(ShopAnalyticsSummaryResponse::getTotalRevenue).reversed()
                    .thenComparing(Comparator.comparingLong(ShopAnalyticsSummaryResponse::getTotalOrders).reversed()));
        } catch (Exception ex) {
            log.error("Lỗi tổng hợp danh sách shop breakdown: {}", ex.getMessage(), ex);
        }

        String targetOwnerName = null;
        try {
            if (targetShop != null && targetShop.getOwner() != null) {
                targetOwnerName = targetShop.getOwner().getFullName();
            }
        } catch (Exception ignored) {}

        return SystemAnalyticsResponse.builder()
                .selectedShopId(targetShop != null ? targetShop.getShopId() : null)
                .selectedShopName(targetShop != null ? targetShop.getShopName() : null)
                .selectedShopOwner(targetOwnerName)
                .selectedShopStatus(targetShop != null && targetShop.getApprovalStatus() != null ? targetShop.getApprovalStatus().name() : null)
                .totalRevenue(totalRevenue)
                .totalPlatformFee(totalPlatformFee)
                .netRevenue(netRevenue)
                .averageOrderValue(averageOrderValue)
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .processingOrders(processingOrders)
                .cancelledOrders(cancelledOrders)
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .totalShopOwners(totalShopOwners)
                .approvedShops(approvedShops)
                .shopProductCount(shopProductCount)
                .totalCustomers(totalCustomers)
                .totalUsers(totalUsers)
                .topSellingProducts(topProducts)
                .shopSummaries(shopSummaries)
                .build();
    }

    private List<TopSellingProductResponse> extractTopSellingProducts(List<Object[]> rows) {
        List<TopSellingProductResponse> topProducts = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return topProducts;
        }

        int rank = 1;
        for (Object[] row : rows) {
            if (row == null || row[0] == null) continue;

            Integer productId = ((Number) row[0]).intValue();
            Long quantitySold = row[1] != null ? ((Number) row[1]).longValue() : 0L;

            BigDecimal revenue = BigDecimal.ZERO;
            if (row[2] != null) {
                if (row[2] instanceof BigDecimal bd) {
                    revenue = bd;
                } else if (row[2] instanceof Number num) {
                    revenue = BigDecimal.valueOf(num.doubleValue());
                } else {
                    try {
                        revenue = new BigDecimal(row[2].toString());
                    } catch (Exception ignored) {}
                }
            }

            Product product = null;
            try {
                product = productRepository.findById(productId).orElse(null);
            } catch (Exception ignored) {}
            if (product == null) continue;

            String categoryName = product.getCategory() != null ? product.getCategory().getCategoryName() : "Trái Cây";
            String shopName = product.getShop() != null ? product.getShop().getShopName() : "Gian hàng";
            Integer pShopId = product.getShop() != null ? product.getShop().getShopId() : null;

            String imageUrl = "/images/default-fruit.png";
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                imageUrl = product.getImages().stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(product.getImages().get(0).getImageUrl());
            }

            int totalStock = 0;
            if (product.getVariants() != null) {
                totalStock = product.getVariants().stream()
                        .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                        .sum();
            }

            topProducts.add(TopSellingProductResponse.builder()
                    .rank(rank++)
                    .productId(productId)
                    .shopId(pShopId)
                    .productName(product.getProductName())
                    .categoryName(categoryName)
                    .shopName(shopName)
                    .imageUrl(imageUrl)
                    .totalQuantitySold(quantitySold)
                    .totalRevenue(revenue)
                    .totalStock(totalStock)
                    .build());
        }
        return topProducts;
    }
}
