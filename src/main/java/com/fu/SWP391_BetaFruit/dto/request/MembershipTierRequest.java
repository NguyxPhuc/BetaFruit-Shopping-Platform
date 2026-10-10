package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MembershipTierRequest {
    private Integer tierId;

    @NotBlank(message = "Tên hạng thành viên không được để trống!")
    private String tierName;

    @NotNull(message = "Ngưỡng chi tiêu yêu cầu không được để trống!")
    @DecimalMin(value = "0.0", inclusive = true, message = "Ngưỡng chi tiêu yêu cầu phải >= 0!")
    private BigDecimal minSpentRequired;

    @NotNull(message = "Tỷ lệ giảm giá không được để trống!")
    @DecimalMin(value = "0.0", inclusive = true, message = "Tỷ lệ giảm giá phải >= 0%!")
    @DecimalMax(value = "100.0", inclusive = true, message = "Tỷ lệ giảm giá không được vượt quá 100%!")
    private BigDecimal discountPercent;
}
