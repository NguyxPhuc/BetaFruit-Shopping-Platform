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
public class CustomerProfileRequest {

    @NotBlank(message = "Vui lòng nhập username")
    @Size(max = 50, message = "Username không được vượt quá 50 ký tự")
    private String username;

    @NotBlank(message = "Vui lòng nhập họ và tên")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập địa chỉ Email")
    @Email(message = "Định dạng Email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ (Phải là số Việt Nam gồm 10 chữ số)")
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;
}
