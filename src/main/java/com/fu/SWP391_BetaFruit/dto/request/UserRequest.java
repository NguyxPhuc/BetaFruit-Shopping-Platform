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
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới (_), không có khoảng trắng!")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống!")
    @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6 đến 50 ký tự!")
    private String password;

    @NotBlank(message = "Email không được để trống!")
    @Email(message = "Email không đúng định dạng hợp lệ!")
    @Size(max = 100, message = "Email tối đa 100 ký tự!")
    private String email;

    @NotBlank(message = "Họ và tên không được để trống!")
    @Size(min = 2, max = 100, message = "Họ và tên từ 2 đến 100 ký tự!")
    private String fullName;

    @Pattern(regexp = "^$|^(0|\\+84)[35789][0-9]{8}$", message = "Số điện thoại phải là số di động hợp lệ (VD: 0912345678) hoặc để trống!")
    private String phone;

    private UserStatus status = UserStatus.ACTIVE;

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 vai trò cho người dùng!")
    private List<Long> roleIds = new ArrayList<>();
}
