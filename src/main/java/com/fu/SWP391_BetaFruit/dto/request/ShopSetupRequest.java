package com.fu.SWP391_BetaFruit.dto.request;

import com.fu.SWP391_BetaFruit.utils.RegexUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopSetupRequest {
    @NotBlank(message = "Vui lòng nhập tên cửa hàng")
    @Size(max = 255, message = "Tên cửa hàng không được vượt quá 255 ký tự")
    private String shopName;

    @Size(max = 2000, message = "Mô tả cửa hàng không được vượt quá 2000 ký tự")
    private String shopDescription;

    @NotBlank(message = "Vui lòng nhập tên ngân hàng")
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

    private MultipartFile logoFile;

    private MultipartFile bannerFile;
}
