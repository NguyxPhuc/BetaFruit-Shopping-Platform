package com.fu.SWP391_BetaFruit.dto.request;

import com.fu.SWP391_BetaFruit.utils.RegexUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordRequest {
    @NotBlank(message = "Mật khẩu cũ không được để trống")
    private String oldPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, max = 64, message = "Mật khẩu mới phải từ 8 đến 64 ký tự")
    @Pattern(regexp = RegexUtils.PASSWORD_NO_SPACE_REGEX, message = "Mật khẩu mới không được chứa khoảng trắng")
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String newPasswordConfirm;
}
