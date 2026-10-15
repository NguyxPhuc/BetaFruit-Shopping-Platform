package com.fu.SWP391_BetaFruit.dto.request;

import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {

    private Integer couponId;

    @NotBlank(message = "Mã giảm giá không được để trống.")
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,20}$", message = "Mã chỉ chứa 3-20 ký tự chữ, số, dấu gạch ngang (-) hoặc gạch dưới (_), không chứa dấu cách.")
    private String couponCode;

    @NotNull(message = "Vui lòng chọn loại danh mục áp dụng.")
    private CouponCategory couponCategory;

    @NotNull(message = "Vui lòng chọn hình thức giảm giá.")
    private DiscountType discountType;

    @NotNull(message = "Vui lòng nhập mức giảm giá.")
    @DecimalMin(value = "0.01", message = "Mức giảm giá phải lớn hơn 0.")
    private BigDecimal discountValue;

    private BigDecimal minOrderValue;

    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Vui lòng nhập tổng số lượt sử dụng tối đa.")
    @Min(value = 1, message = "Lượt sử dụng tối thiểu là 1.")
    private Integer usageLimit;

    @NotNull(message = "Vui lòng chọn thời gian bắt đầu.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;

    @NotNull(message = "Vui lòng chọn thời gian kết thúc.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;

    private Boolean isActive = true;
}
