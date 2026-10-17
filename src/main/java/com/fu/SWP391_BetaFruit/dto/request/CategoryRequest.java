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
public class CategoryRequest {
    private Integer categoryId;

    @NotBlank(message = "Tên danh mục hoa quả không được để trống!")
    @Size(min = 2, max = 100, message = "Tên danh mục phải từ 2 đến 100 ký tự!")
    @Pattern(
        regexp = "^(?!.*(.)\\1{2,})(?!.*[^\\s]{21,})[\\p{L}0-9\\s,./&()_\\-]+$",
        message = "Tên danh mục không hợp lệ (không được chứa ký tự lặp lại quá 2 lần hoặc chứa từ dài bất thường)!"
    )
    private String categoryName;

    private Boolean isActive;
}
