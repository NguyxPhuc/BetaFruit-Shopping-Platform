package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO phân bổ phương thức thanh toán của các đơn hàng có sử dụng Voucher (COD vs VNPAY).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponPaymentMethodResponse {
    private long codOrderCount;                 // Số đơn thanh toán khi nhận hàng
    private long onlineOrderCount;              // Số đơn thanh toán VNPAY trực tuyến
    private BigDecimal codRevenueTotal;         // Doanh thu đơn COD
    private BigDecimal onlineRevenueTotal;      // Doanh thu đơn VNPAY
}
