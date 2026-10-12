package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VariantItemRequest {

    @NotBlank(message = "Tên thực thể không được để trống (VD: Hộp 500g, Thùng 2kg, Quả 1.5kg).")
    @Size(max = 100, message = "Tên thực thể tối đa 100 ký tự.")
    private String variantName;

    @NotNull(message = "Giá gốc niêm yết không được để trống.")
    @DecimalMin(value = "1000", message = "Giá gốc phải từ 1,000 VNĐ trở lên.")
    private BigDecimal originalPrice;

    private BigDecimal costPrice;

    @NotNull(message = "Số lượng tồn kho không được để trống.")
    @Min(value = 0, message = "Số lượng tồn kho không được là số âm.")
    private Integer stockQuantity;

    private Integer lowStockThreshold = 5;
}
