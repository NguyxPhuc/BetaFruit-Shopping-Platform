package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(min = 2, max = 100, message = "Tên hạng thành viên phải từ 2 đến 100 ký tự!")
    @Pattern(
        regexp = "^(?!.*([^\\s])\\1{2,})(?!.*[^\\s]{51,})[\\p{L}\\p{M}0-9\\s\\-_]+$",
        message = "Tên hạng không hợp lệ (không được chứa ký tự lặp lại quá 2 lần hoặc chứa từ dài bất thường)!"
    )
    private String tierName;

    @NotNull(message = "Ngưỡng chi tiêu yêu cầu không được để trống!")
    @DecimalMin(value = "0.0", inclusive = true, message = "Ngưỡng chi tiêu yêu cầu phải >= 0!")
    @DecimalMax(value = "10000000000.0", inclusive = true, message = "Ngưỡng chi tiêu không được vượt quá 10 tỷ VNĐ!")
    @Digits(integer = 11, fraction = 2, message = "Ngưỡng chi tiêu không hợp lệ!")
    private BigDecimal minSpentRequired;

    @NotNull(message = "Số đơn hàng yêu cầu không được để trống!")
    @Min(value = 0, message = "Số đơn hàng yêu cầu phải >= 0!")
    @Max(value = 10000, message = "Số đơn hàng yêu cầu không được vượt quá 10.000 đơn!")
    private Integer minOrdersRequired;
}
