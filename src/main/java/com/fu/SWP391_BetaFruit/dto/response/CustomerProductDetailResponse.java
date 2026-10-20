package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProductDetailResponse {
    private Integer productId;
    private String productName;
    private String description;
    private Integer categoryId;
    private String categoryName;
    private Integer shopId;
    private String shopName;
    private String primaryImageUrl;
    private List<String> imageUrls;
    private BigDecimal displayPrice;
    private BigDecimal originalPrice;
    private String formattedPrice;
    private String formattedOriginalPrice;
    private Double rating;
    private Integer fullStars;
    private Long reviewCount;
    private Boolean inStock;
    private Integer totalStock;
    private String weightSpecification;
    private List<CustomerVariantResponse> variants;
    private List<CustomerReviewResponse> reviews;
}
