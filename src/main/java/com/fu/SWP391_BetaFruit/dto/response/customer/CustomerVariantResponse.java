package com.fu.SWP391_BetaFruit.dto.response.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVariantResponse {
    private Integer variantId;
    private String variantName;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private BigDecimal displayPrice;
    private String formattedPrice;
    private Integer stockQuantity;
    private Boolean isAvailable;
}
