package com.fu.SWP391_BetaFruit.dto.request;

import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO nhận các tham số lọc đa chiều cho màn hình Voucher Analytics Dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDashboardFilterRequest {

    /**
     * Mốc thời gian chọn nhanh: "7DAYS", "14DAYS", "30DAYS", "THIS_MONTH", "CUSTOM"
     */
    @Builder.Default
    private String rangePreset = "14DAYS";

    /**
     * Ngày bắt đầu khoảng lọc tùy chọn (YYYY-MM-DD)
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    /**
     * Ngày kết thúc khoảng lọc tùy chọn (YYYY-MM-DD)
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /**
     * Lọc theo danh mục ưu đãi: PRODUCT (Giảm đơn), SHIPPING (Freeship), hoặc null (Tất cả)
     */
    private CouponCategory category;

    /**
     * Tự động chuẩn hóa và tính toán ngày bắt đầu / ngày kết thúc thực tế.
     */
    public void resolveEffectiveDates() {
        LocalDate today = LocalDate.now();

        if (rangePreset == null || rangePreset.trim().isEmpty()) {
            rangePreset = "14DAYS";
        }

        switch (rangePreset.toUpperCase()) {
            case "7DAYS":
                this.startDate = today.minusDays(6);
                this.endDate = today;
                break;
            case "30DAYS":
                this.startDate = today.minusDays(29);
                this.endDate = today;
                break;
            case "THIS_MONTH":
                this.startDate = today.withDayOfMonth(1);
                this.endDate = today;
                break;
            case "CUSTOM":
                if (this.startDate == null) {
                    this.startDate = today.minusDays(13);
                }
                if (this.endDate == null) {
                    this.endDate = today;
                }
                if (this.startDate.isAfter(this.endDate)) {
                    LocalDate temp = this.startDate;
                    this.startDate = this.endDate;
                    this.endDate = temp;
                }
                break;
            case "14DAYS":
            default:
                this.startDate = today.minusDays(13);
                this.endDate = today;
                this.rangePreset = "14DAYS";
                break;
        }
    }
}
