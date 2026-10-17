package com.fu.SWP391_BetaFruit.dto.request;

import com.fu.SWP391_BetaFruit.enums.UserStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống!")
    @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 đến 50 ký tự!")
    @Pattern(
        regexp = "^(?!.*(.)\\1{2,})[a-zA-Z0-9_]{3,50}$",
        message = "Tên đăng nhập từ 3 đến 50 ký tự, chỉ chứa chữ cái, số và dấu gạch dưới (_), không chứa dấu cách và không lặp ký tự liên tiếp quá 2 lần!"
    )
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống!")
    @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự!")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,64}$",
        message = "Mật khẩu phải từ 8 đến 64 ký tự, bao gồm ít nhất 1 chữ hoa, 1 chữ số và 1 ký tự đặc biệt!"
    )
    private String password;

    @NotBlank(message = "Email không được để trống!")
    @Email(message = "Email không đúng định dạng hợp lệ!")
    @Size(max = 100, message = "Email tối đa 100 ký tự!")
    private String email;

    @NotBlank(message = "Họ và tên không được để trống!")
    @Size(min = 2, max = 100, message = "Họ và tên từ 2 đến 100 ký tự!")
    @Pattern(
        regexp = "^(?!.*(.)\\1{2,})[\\p{L}\\s\\-_']{2,100}$",
        message = "Họ và tên từ 2 đến 100 ký tự, chỉ chứa chữ cái, dấu cách, dấu gạch nối hoặc dấu nháy đơn, và không lặp ký tự liên tiếp quá 2 lần!"
    )
    private String fullName;

    @Pattern(
        regexp = "^$|^(0|\\+84)[\\s\\-]?[35789]([\\s\\-]?[0-9]){8}$",
        message = "Số điện thoại phải là số di động hợp lệ (VD: 0912345678) hoặc để trống!"
    )
    private String phone;

    private UserStatus status = UserStatus.ACTIVE;

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 vai trò cho người dùng!")
    private List<Long> roleIds = new ArrayList<>();
}
