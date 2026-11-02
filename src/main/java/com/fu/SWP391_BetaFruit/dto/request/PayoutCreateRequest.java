package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PayoutCreateRequest {
    @NotNull(message = "Vui lòng nhập số tiền cần rút")
    @DecimalMin(value = "50000.00", message = "Số tiền rút tối thiểu là 50.000 VNĐ")
    private BigDecimal requestedAmount;
    @NotBlank(message = "Vui lòng chọn ngân hàng")
    private String bankName;
    @NotBlank(message = "Vui lòng nhập số tài khoản ngân hàng")
    private String bankAccountNumber;
    @NotBlank(message = "Vui lòng nhập tên chủ tài khoản")
    private String accountName;
}
