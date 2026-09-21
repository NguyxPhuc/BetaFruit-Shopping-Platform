package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống.")
    @Size(max = 50, message = "Tên đăng nhập tối đa 50 ký tự.")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống.")
    @Size(min = 6, max = 20, message = "Mật khẩu phải từ 6 -20 kí tự.")
    private String password;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 100, message = "Email tối đa 100 ký tự.")
    private String email;

    @NotBlank(message = "Họ và tên không được để trống.")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự.")
    private String fullName;

    @Pattern(regexp = "^(0|\\+84)[35789][0-9]{8}$", message = "Số điện thoại phải là số di động hợp lệ (VD: 0912345678 hoặc +84912345678).")
    private String phone;
}
