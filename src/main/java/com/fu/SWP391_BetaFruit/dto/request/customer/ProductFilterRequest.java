package com.fu.SWP391_BetaFruit.dto.request.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterRequest {
    private String keyword;
    private Integer categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer rating;          // e.g. 5 for 5 stars, 4 for 4+ stars
    private String availability;     // "all", "in_stock", "out_of_stock"
    private String sortBy;           // "newest", "price_asc", "price_desc", "rating_desc", "bestseller"
    
    @Builder.Default
    private int page = 1;
    
    @Builder.Default
    private int size = 6;
}
