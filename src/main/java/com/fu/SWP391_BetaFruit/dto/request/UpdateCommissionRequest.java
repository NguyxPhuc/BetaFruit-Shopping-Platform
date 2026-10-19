package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
public class UpdateCommissionRequest {

    @NotNull(message = "Tỷ lệ phí sàn không được để trống!")
    @DecimalMin(value = "0.00", inclusive = true, message = "Tỷ lệ chiết khấu phí sàn phải nằm trong khoảng từ 0% đến 100%!")
    @DecimalMax(value = "100.00", inclusive = true, message = "Tỷ lệ chiết khấu phí sàn phải nằm trong khoảng từ 0% đến 100%!")
    private BigDecimal commissionRate;
}
