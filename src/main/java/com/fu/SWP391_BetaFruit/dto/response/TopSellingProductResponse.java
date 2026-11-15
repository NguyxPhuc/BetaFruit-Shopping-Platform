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
public class TopSellingProductResponse {
    private Integer rank; // 1, 2, 3, 4, 5
    private Integer productId;
    private Integer shopId; // Mã ID của shop
    private String productName;
    private String categoryName;
    private String shopName;
    private String imageUrl;
    private Long totalQuantitySold; // Số lượng đã bán
    private BigDecimal totalRevenue; // Doanh số tạo ra (VNĐ)
    private Integer totalStock; // Tồn kho hiện tại
}
