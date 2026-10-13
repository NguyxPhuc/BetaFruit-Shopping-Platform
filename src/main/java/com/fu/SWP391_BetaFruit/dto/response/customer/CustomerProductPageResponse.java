package com.fu.SWP391_BetaFruit.dto.response.customer;

import com.fu.SWP391_BetaFruit.dto.request.customer.ProductFilterRequest;
import com.fu.SWP391_BetaFruit.entity.MasterCategory;
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
public class CustomerProductPageResponse {
    private List<CustomerProductCardResponse> products;
    private int currentPage;
    private int totalPages;
    private long totalProducts;
    private int pageSize;
    private ProductFilterRequest filter;
    private List<MasterCategory> categories;
    private BigDecimal minAvailablePrice;
    private BigDecimal maxAvailablePrice;
}
