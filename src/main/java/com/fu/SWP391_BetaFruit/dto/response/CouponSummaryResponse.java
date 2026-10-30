package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO chứa các chỉ số tổng quan đầu trang của Voucher Dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponSummaryResponse {
    // 1. Nhóm chi phí giảm giá (Tách bạch cả hai bên tài trợ)
    private BigDecimal totalDiscountAmount;      // Tổng tiền giảm giá toàn hệ thống
    private BigDecimal platformDiscountAmount;   // Phần tiền do Sàn BetaFruit bỏ tiền tài trợ
    private BigDecimal shopDiscountAmount;       // Phần tiền do các Cửa Hàng tự bỏ tiền tài trợ

    // 2. Doanh thu từ voucher
    private BigDecimal totalVoucherRevenue;      // Tổng doanh thu từ các đơn có dùng voucher

    // 3. Các chỉ số đánh giá hiệu quả kinh doanh
    private BigDecimal returnOnInvestment;       // Tỷ số hoàn vốn đầu tư (ROI = Revenue / Discount)
    private BigDecimal estimatedGrossProfit;     // Lợi nhuận gộp ước tính (Doanh thu - Giá vốn costPrice - Tiền giảm giá)
    private long voucherOrderCount;              // Số lượng đơn hàng có áp mã
    private long totalOrderCount;                // Tổng số đơn hàng toàn hệ thống
    private BigDecimal voucherOrderRate;         // Tỷ lệ đơn hàng sử dụng mã (%)
    private BigDecimal averageOrderValue;        // Giá trị đơn hàng trung bình (AOV)
    private BigDecimal averageCustomerSavings;   // Mức tiết kiệm trung bình của khách hàng trên mỗi đơn
}
