package com.fu.SWP391_BetaFruit.dto.request;

import com.fu.SWP391_BetaFruit.utils.RegexUtils;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Pattern(regexp = RegexUtils.BANK_NAME_REGEX, message = "Tên ngân hàng không được chứa ký tự đặc biệt")
    @Size(max = 100, message = "Tên ngân hàng không được vượt quá 100 ký tự")
    private String bankName;

    @NotBlank(message = "Vui lòng nhập số tài khoản ngân hàng")
    @Pattern(regexp = RegexUtils.BANK_ACCOUNT_REGEX, message = "Số tài khoản ngân hàng chỉ được chứa chữ số")
    @Size(max = 50, message = "Số tài khoản không được vượt quá 50 ký tự")
    private String bankAccountNumber;

    @NotBlank(message = "Vui lòng nhập tên chủ tài khoản")
    @Pattern(regexp = RegexUtils.ACCOUNT_NAME_REGEX, message = "Tên chủ tài khoản chỉ được chứa chữ cái không dấu và khoảng trắng")
    @Size(max = 100, message = "Tên chủ tài khoản không được vượt quá 100 ký tự")
    private String accountName;
}
