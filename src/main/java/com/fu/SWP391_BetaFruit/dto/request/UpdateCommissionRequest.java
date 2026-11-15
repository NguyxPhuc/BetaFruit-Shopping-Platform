package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
    @DecimalMin(value = "0.00", inclusive = true, message = "Tỷ lệ chiết khấu phí sàn phải nằm trong khoảng từ 0.00% đến 30.00%!")
    @DecimalMax(value = "30.00", inclusive = true, message = "Tỷ lệ chiết khấu phí sàn không được vượt quá 30.00% để đảm bảo lợi nhuận cho đối tác bán hàng!")
    @Digits(integer = 2, fraction = 2, message = "Tỷ lệ phí sàn chỉ được tối đa 2 chữ số thập phân (ví dụ: 5.50%)!")
    private BigDecimal commissionRate;
}
