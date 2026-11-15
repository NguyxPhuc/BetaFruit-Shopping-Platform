package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO phân bổ chi phí và đơn hàng theo Danh mục mã (Freeship vs Giảm giá Quả) cho biểu đồ tròn.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponCategoryDistributionResponse {
    private BigDecimal shippingDiscountTotal;   // Tiền giảm phí vận chuyển (Freeship)
    private BigDecimal productDiscountTotal;    // Tiền giảm giá sản phẩm hoa quả
    private long shippingOrderCount;            // Số đơn dùng mã Freeship
    private long productOrderCount;             // Số đơn dùng mã giảm giá Sản phẩm
}
