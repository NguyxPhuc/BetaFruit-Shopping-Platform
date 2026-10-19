package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Email không được để trống.")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống.")
    private String password;
}
