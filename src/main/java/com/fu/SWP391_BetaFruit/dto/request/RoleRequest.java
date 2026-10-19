package com.fu.SWP391_BetaFruit.dto.request;

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
public class RoleRequest {
    private Long roleId;

    @NotBlank(message = "Tên vai trò không được để trống!")
    @Size(min = 2, max = 50, message = "Tên vai trò từ 2 đến 50 ký tự!")
    @Pattern(
        regexp = "^(?!.*(.)\\1{2,})[A-Z][A-Z0-9_]{1,49}$",
        message = "Tên vai trò phải viết hoa, không được chứa ký tự lặp lại liên tiếp (VD: AUDITOR, SUPPORT_LEAD)!"
    )
    private String roleName;
}
