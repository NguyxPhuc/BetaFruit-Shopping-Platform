package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.customer.ProductFilterRequest;
import com.fu.SWP391_BetaFruit.dto.response.customer.CustomerProductCardResponse;
import com.fu.SWP391_BetaFruit.dto.response.customer.CustomerProductDetailResponse;
import com.fu.SWP391_BetaFruit.dto.response.customer.CustomerProductPageResponse;
import com.fu.SWP391_BetaFruit.dto.response.customer.CustomerReviewResponse;
import com.fu.SWP391_BetaFruit.dto.response.customer.CustomerVariantResponse;
import com.fu.SWP391_BetaFruit.entity.MasterCategory;
import com.fu.SWP391_BetaFruit.entity.Product;
import com.fu.SWP391_BetaFruit.entity.ProductImage;
import com.fu.SWP391_BetaFruit.entity.ProductVariant;
import com.fu.SWP391_BetaFruit.entity.Review;
import com.fu.SWP391_BetaFruit.repository.MasterCategoryRepository;
import com.fu.SWP391_BetaFruit.repository.ProductRepository;
import com.fu.SWP391_BetaFruit.repository.ProductVariantRepository;
import com.fu.SWP391_BetaFruit.repository.ProductImageRepository;
import com.fu.SWP391_BetaFruit.repository.ReviewRepository;
import com.fu.SWP391_BetaFruit.repository.OrderItemRepository;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.service.CustomerProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerProductServiceImpl implements CustomerProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ReviewRepository reviewRepository;
    private final MasterCategoryRepository masterCategoryRepository;
    private final OrderItemRepository orderItemRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    @Transactional(readOnly = true)
    public CustomerProductPageResponse getProductsPage(ProductFilterRequest request) {
        if (request == null) {
            request = new ProductFilterRequest();
        }
        if (request.getPage() < 1) {
            request.setPage(1);
        }
        if (request.getSize() < 1) {
            request.setSize(6);
        }
        request.setSize(Math.min(request.getSize(), 100));
        validateFilter(request);

        List<Product> products = productRepository.findActiveCustomerProducts();
        List<CustomerProductCardResponse> cardResponses = new ArrayList<>();

        for (Product product : products) {
            CustomerProductCardResponse card = mapToCardResponse(product);
            cardResponses.add(card);
        }

        // Apply in-memory filters
        final String cleanKeyword = (request.getKeyword() != null) ? request.getKeyword().trim().toLowerCase() : "";
        final Integer targetCategory = request.getCategoryId();
        final BigDecimal minPrice = request.getMinPrice();
        final BigDecimal maxPrice = request.getMaxPrice();
        final Integer minRating = request.getRating();
        final String availability = request.getAvailability();

        List<CustomerProductCardResponse> filtered = cardResponses.stream().filter(card -> {
            // Keyword filter
            if (!cleanKeyword.isEmpty()) {
                boolean matchName = card.getProductName() != null && card.getProductName().toLowerCase().contains(cleanKeyword);
                if (!matchName) {
                    return false;
                }
            }

            // Category filter
            if (targetCategory != null && targetCategory > 0) {
                if (card.getCategoryId() == null || !card.getCategoryId().equals(targetCategory)) {
                    return false;
                }
            }

            // Price filter
            if (minPrice != null) {
                if (card.getDisplayPrice() == null || card.getDisplayPrice().compareTo(minPrice) < 0) {
                    return false;
                }
            }
            if (maxPrice != null) {
                if (card.getDisplayPrice() == null || card.getDisplayPrice().compareTo(maxPrice) > 0) {
                    return false;
                }
            }

            // Rating filter
            if (minRating != null && minRating > 0) {
                if (card.getRating() == null || card.getReviewCount() == 0 || card.getRating() < minRating.doubleValue()) {
                    return false;
                }
            }

            // Availability filter
            if (availability != null && !availability.trim().isEmpty() && !"all".equalsIgnoreCase(availability)) {
                if ("in_stock".equalsIgnoreCase(availability) && !Boolean.TRUE.equals(card.getInStock())) {
                    return false;
                }
                if ("out_of_stock".equalsIgnoreCase(availability) && Boolean.TRUE.equals(card.getInStock())) {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());

        // Sorting
        String sortBy = request.getSortBy();
        if (sortBy != null) {
            switch (sortBy.toLowerCase()) {
                case "price_asc":
                    filtered.sort(Comparator.comparing(CustomerProductCardResponse::getDisplayPrice, Comparator.nullsLast(BigDecimal::compareTo)));
                    break;
                case "price_desc":
                    filtered.sort(Comparator.comparing(CustomerProductCardResponse::getDisplayPrice, Comparator.nullsLast(Comparator.reverseOrder())));
                    break;
                case "rating_desc":
                    filtered.sort(Comparator.comparing(CustomerProductCardResponse::getRating, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(CustomerProductCardResponse::getReviewCount, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(CustomerProductCardResponse::getProductId));
                    break;
                case "bestseller":
                    Map<Integer, Long> soldQuantities = new HashMap<>();
                    for (Object[] row : orderItemRepository.sumSoldQuantityByProduct(OrderStatus.SUCCESS)) {
                        soldQuantities.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
                    }
                    filtered.sort(Comparator.comparingLong((CustomerProductCardResponse card) ->
                            soldQuantities.getOrDefault(card.getProductId(), 0L)).reversed()
                            .thenComparing(CustomerProductCardResponse::getProductId, Comparator.reverseOrder()));
                    break;
                case "newest":
                default:
                    sortNewest(filtered, products);
                    break;
            }
        } else {
            // Default sort: ID desc
            sortNewest(filtered, products);
        }

        // Pagination
        int totalElements = filtered.size();
        int pageSize = request.getSize();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        if (totalPages == 0) totalPages = 1;

        int currentPage = request.getPage();
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<CustomerProductCardResponse> pagedProducts = (fromIndex <= totalElements) ? filtered.subList(fromIndex, toIndex) : Collections.emptyList();

        List<MasterCategory> activeCategories = getAllActiveCategories();

        return CustomerProductPageResponse.builder()
                .products(pagedProducts)
                .currentPage(currentPage)
                .totalPages(totalPages)
                .totalProducts(totalElements)
                .pageSize(pageSize)
                .filter(request)
                .categories(activeCategories)
                .minAvailablePrice(BigDecimal.ZERO)
                .maxAvailablePrice(new BigDecimal("500000"))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerProductCardResponse> getFeaturedProducts(int limit) {
        List<Product> products = productRepository.findActiveCustomerProducts();
        return products.stream()
                .map(this::mapToCardResponse)
                .limit(limit > 0 ? limit : 4)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProductDetailResponse getProductDetail(Integer productId) {
        if (productId == null) {
            return null;
        }

        Optional<Product> optionalProduct = productRepository.findActiveCustomerProductById(productId);
        if (optionalProduct.isEmpty()) {
            return null;
        }

        Product product = optionalProduct.get();
        List<ProductVariant> variants = productVariantRepository.findByProduct_ProductId(productId);
        List<ProductImage> images = productImageRepository.findByProduct_ProductId(productId);

        // Primary image
        String primaryImageUrl = null;
        List<String> imageUrls = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (ProductImage img : images) {
                if (img.getImageUrl() != null && !img.getImageUrl().trim().isEmpty()) {
                    imageUrls.add(img.getImageUrl().trim());
                    if (Boolean.TRUE.equals(img.getIsPrimary()) && primaryImageUrl == null) {
                        primaryImageUrl = img.getImageUrl().trim();
                    }
                }
            }
            if (primaryImageUrl == null && !imageUrls.isEmpty()) {
                primaryImageUrl = imageUrls.get(0);
            }
        }

        // Variants mapping
        List<CustomerVariantResponse> variantResponses = new ArrayList<>();
        BigDecimal lowestPrice = null;
        BigDecimal originalPrice = null;
        int totalStock = 0;
        boolean inStock = false;
        String weightSpecification = "1Kg";

        if (variants != null && !variants.isEmpty()) {
            for (ProductVariant v : variants) {
                BigDecimal effective = (v.getSalePrice() != null && v.getSalePrice().compareTo(BigDecimal.ZERO) > 0)
                        ? v.getSalePrice() : v.getOriginalPrice();

                if (lowestPrice == null || (effective != null && effective.compareTo(lowestPrice) < 0)) {
                    lowestPrice = effective;
                    originalPrice = v.getOriginalPrice();
                    if (v.getVariantName() != null && !v.getVariantName().trim().isEmpty()) {
                        weightSpecification = v.getVariantName().trim();
                    }
                }

                if (v.getStockQuantity() != null) {
                    totalStock += v.getStockQuantity();
                    if (v.getStockQuantity() > 0 && Boolean.TRUE.equals(v.getIsAvailable())) {
                        inStock = true;
                    }
                }

                variantResponses.add(CustomerVariantResponse.builder()
                        .variantId(v.getVariantId())
                        .variantName(v.getVariantName())
                        .originalPrice(v.getOriginalPrice())
                        .salePrice(v.getSalePrice())
                        .displayPrice(effective)
                        .formattedPrice(formatCurrency(effective))
                        .stockQuantity(v.getStockQuantity())
                        .isAvailable(v.getIsAvailable())
                        .build());
            }
        }

        if (lowestPrice == null) {
            lowestPrice = BigDecimal.ZERO;
        }

        // Reviews mapping
        List<Review> reviewEntities = reviewRepository.findByProductId(productId);
        List<CustomerReviewResponse> reviewResponses = new ArrayList<>();
        double avgRating = 0.0;
        long reviewCount = 0;

        if (reviewEntities != null && !reviewEntities.isEmpty()) {
            reviewCount = reviewEntities.size();
            double sum = 0;
            for (Review r : reviewEntities) {
                sum += (r.getRating() != null ? r.getRating() : 0);
                String customerName = (r.getCustomer() != null && r.getCustomer().getFullName() != null)
                        ? r.getCustomer().getFullName() : "Khách hàng BetaFruit";

                reviewResponses.add(CustomerReviewResponse.builder()
                        .reviewId(r.getReviewId())
                        .customerName(customerName)
                        .rating(r.getRating() != null ? r.getRating() : 0)
                        .comment(r.getComment())
                        .shopReply(r.getShopReply())
                        .createdAt(r.getCreatedAt())
                        .repliedAt(r.getRepliedAt())
                        .formattedDate(r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FORMATTER) : "")
                        .build());
            }
            avgRating = Math.round((sum / reviewCount) * 10.0) / 10.0;
        }

        return CustomerProductDetailResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : "Trái cây")
                .shopId(product.getShop() != null ? product.getShop().getShopId() : null)
                .shopName(product.getShop() != null ? product.getShop().getShopName() : "BetaFruit")
                .primaryImageUrl(primaryImageUrl)
                .imageUrls(imageUrls)
                .displayPrice(lowestPrice)
                .originalPrice(originalPrice)
                .formattedPrice(formatCurrency(lowestPrice))
                .formattedOriginalPrice(originalPrice != null && originalPrice.compareTo(lowestPrice) > 0 ? formatCurrency(originalPrice) : null)
                .rating(avgRating)
                .fullStars((int) Math.round(avgRating))
                .reviewCount(reviewCount)
                .inStock(inStock)
                .totalStock(totalStock)
                .weightSpecification(weightSpecification)
                .variants(variantResponses)
                .reviews(reviewResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MasterCategory> getAllActiveCategories() {
        return masterCategoryRepository.findByIsActiveTrueOrderByCategoryNameAsc();
    }

    private CustomerProductCardResponse mapToCardResponse(Product product) {
        List<ProductVariant> variants = productVariantRepository.findByProduct_ProductId(product.getProductId());

        BigDecimal lowestPrice = null;
        BigDecimal originalPrice = null;
        BigDecimal salePrice = null;
        boolean inStock = false;
        int totalStock = 0;
        String unit = "kg";

        if (variants != null && !variants.isEmpty()) {
            for (ProductVariant v : variants) {
                BigDecimal effective = (v.getSalePrice() != null && v.getSalePrice().compareTo(BigDecimal.ZERO) > 0)
                        ? v.getSalePrice() : v.getOriginalPrice();

                if (lowestPrice == null || (effective != null && effective.compareTo(lowestPrice) < 0)) {
                    lowestPrice = effective;
                    originalPrice = v.getOriginalPrice();
                    salePrice = v.getSalePrice();
                    if (v.getVariantName() != null && !v.getVariantName().trim().isEmpty()) {
                        unit = v.getVariantName().trim().toLowerCase();
                        if (unit.contains("1 kg") || unit.contains("1kg")) {
                            unit = "kg";
                        }
                    }
                }

                if (v.getStockQuantity() != null) {
                    totalStock += v.getStockQuantity();
                    if (v.getStockQuantity() > 0 && Boolean.TRUE.equals(v.getIsAvailable())) {
                        inStock = true;
                    }
                }
            }
        }

        if (lowestPrice == null) {
            lowestPrice = BigDecimal.ZERO;
        }

        // Image
        String imageUrl = null;
        List<ProductImage> images = productImageRepository.findByProduct_ProductId(product.getProductId());
        if (images != null && !images.isEmpty()) {
            for (ProductImage img : images) {
                if (img.getImageUrl() != null && !img.getImageUrl().trim().isEmpty()) {
                    if (Boolean.TRUE.equals(img.getIsPrimary())) {
                        imageUrl = img.getImageUrl().trim();
                        break;
                    }
                    if (imageUrl == null) {
                        imageUrl = img.getImageUrl().trim();
                    }
                }
            }
        }

        // Formatted price with unit
        String formattedPrice = formatCurrency(lowestPrice);
        if (unit != null && !unit.isEmpty()) {
            formattedPrice += " / " + unit;
        }

        // Rating
        Double avgRating = reviewRepository.getAverageRatingByProductId(product.getProductId());
        Long reviewCount = reviewRepository.countReviewsByProductId(product.getProductId());
        if (avgRating == null || reviewCount == null || reviewCount == 0) {
            avgRating = 0.0;
            reviewCount = 0L;
        } else {
            avgRating = Math.round(avgRating * 10.0) / 10.0;
        }

        return CustomerProductCardResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : "Trái cây")
                .shopName(product.getShop() != null ? product.getShop().getShopName() : "BetaFruit")
                .imageUrl(imageUrl)
                .originalPrice(originalPrice)
                .salePrice(salePrice)
                .displayPrice(lowestPrice)
                .formattedPrice(formattedPrice)
                .formattedOriginalPrice(originalPrice != null && originalPrice.compareTo(lowestPrice) > 0 ? formatCurrency(originalPrice) : null)
                .rating(avgRating)
                .fullStars((int) Math.round(avgRating))
                .reviewCount(reviewCount)
                .inStock(inStock)
                .stockQuantity(totalStock)
                .unit(unit)
                .build();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0đ";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount) + "đ";
    }

    private void validateFilter(ProductFilterRequest request) {
        if ((request.getMinPrice() != null && request.getMinPrice().signum() < 0)
                || (request.getMaxPrice() != null && request.getMaxPrice().signum() < 0)) {
            throw new IllegalArgumentException("Giá lọc không được âm.");
        }
        if (request.getMinPrice() != null && request.getMaxPrice() != null
                && request.getMinPrice().compareTo(request.getMaxPrice()) > 0) {
            throw new IllegalArgumentException("Giá tối thiểu không được lớn hơn giá tối đa.");
        }
        if (request.getRating() != null && (request.getRating() < 1 || request.getRating() > 5)) {
            throw new IllegalArgumentException("Đánh giá phải từ 1 đến 5 sao.");
        }
    }

    private void sortNewest(List<CustomerProductCardResponse> cards, List<Product> products) {
        Map<Integer, java.time.LocalDateTime> dates = new HashMap<>();
        products.forEach(product -> dates.put(product.getProductId(), product.getCreatedAt()));
        cards.sort(Comparator.comparing((CustomerProductCardResponse card) -> dates.get(card.getProductId()),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CustomerProductCardResponse::getProductId, Comparator.reverseOrder()));
    }
}
