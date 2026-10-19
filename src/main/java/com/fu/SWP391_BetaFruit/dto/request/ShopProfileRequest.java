package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopProfileRequest {
    @NotBlank(message = "Vui lòng nhập tên cửa hàng")
    @Size(max = 255, message = "Tên cửa hàng không được vượt quá 255 ký tự")
    private String shopName;

    @Size(max = 2000, message = "Mô tả cửa hàng không được vượt quá 2000 ký tự")
    private String shopDescription;

    @NotBlank(message = "Vui lòng nhập địa chỉ Email")
    @Email(message = "Định dạng Email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ (Phải là số Việt Nam gồm 10 chữ số)")
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;

    @NotBlank(message = "Vui lòng nhập tên ngân hàng")
    @Size(max = 100, message = "Tên ngân hàng không được vượt quá 100 ký tự")
    private String bankName;

    @NotBlank(message = "Vui lòng nhập số tài khoản ngân hàng")
    @Pattern(regexp = "^[0-9]+$", message = "Số tài khoản ngân hàng chỉ được chứa chữ số")
    @Size(max = 50, message = "Số tài khoản không được vượt quá 50 ký tự")
    private String bankAccountNumber;

    @NotBlank(message = "Vui lòng nhập tên chủ tài khoản")
    @Size(max = 100, message = "Tên chủ tài khoản không được vượt quá 100 ký tự")
    private String accountName;
}
