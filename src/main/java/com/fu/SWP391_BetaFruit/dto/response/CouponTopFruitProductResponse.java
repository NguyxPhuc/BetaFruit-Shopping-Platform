package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO chứa thông tin Top loại hoa quả bán chạy nhất nhờ có áp dụng Voucher.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponTopFruitProductResponse {
    private String productName;                 // Tên sản phẩm hoa quả
    private long totalQuantitySold;             // Tổng số lượng/kg đã bán
    private BigDecimal totalRevenue;            // Doanh số đóng góp
    private long orderCount;                    // Số đơn hàng có mua sản phẩm này
}
