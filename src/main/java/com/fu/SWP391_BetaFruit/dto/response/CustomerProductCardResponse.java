package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProductCardResponse {
    private Integer productId;
    private String productName;
    private Integer categoryId;
    private String categoryName;
    private String shopName;
    private String imageUrl;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private BigDecimal displayPrice;
    private String formattedPrice;
    private String formattedOriginalPrice;
    private Double rating;
    private Integer fullStars;
    private Long reviewCount;
    private Boolean inStock;
    private Integer stockQuantity;
    private String unit;
}
