package com.fu.SWP391_BetaFruit.dto.request;

import com.fu.SWP391_BetaFruit.utils.RegexUtils;
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
    @NotBlank(message = "Tên người dùng không được để trống.")
    @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 đến 50 ký tự.")
    @Pattern(regexp = RegexUtils.USERNAME_REGEX, message = "Tên đăng nhập từ 3 đến 50 ký tự, chỉ chứa chữ cái, số và dấu gạch dưới, không chứa khoảng trắng.")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống.")
    @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự.")
    @Pattern(regexp = RegexUtils.PASSWORD_NO_SPACE_REGEX, message = "Mật khẩu không được chứa khoảng trắng.")
    private String password;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 100, message = "Email tối đa 100 ký tự.")
    private String email;

    @NotBlank(message = "Họ và tên không được để trống.")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự.")
    @Pattern(regexp = RegexUtils.NAME_REGEX, message = "Họ và tên chỉ được chứa chữ cái và khoảng trắng.")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống.")
    @Pattern(regexp = RegexUtils.PHONE_REGEX, message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng các đầu số 03, 05, 07, 08 hoặc 09 (VD: 0912345678 hoặc +84912345678).")
    private String phone;
}
