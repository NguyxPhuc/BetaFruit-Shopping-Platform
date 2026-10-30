package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO điểm dữ liệu xu hướng theo ngày biểu diễn trên Line Chart (Doanh thu & Chi phí giảm giá).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponDailyTrendResponse {
    private LocalDate orderDate;                // Ngày phát sinh đơn hàng
    private String formattedDate;               // Chuỗi ngày định dạng dd/MM để vẽ biểu đồ
    private BigDecimal revenueTotal;            // Doanh số bán trong ngày
    private BigDecimal discountTotal;           // Tiền giảm giá trong ngày
    private long orderCount;                    // Số lượng đơn hàng áp mã trong ngày
}
