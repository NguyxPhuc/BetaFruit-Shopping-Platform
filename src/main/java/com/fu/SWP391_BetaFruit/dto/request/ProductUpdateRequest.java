package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {

    private Integer productId;

    @NotBlank(message = "Tên hoa quả không được để trống.")
    @Size(max = 255, message = "Tên hoa quả tối đa 255 ký tự.")
    private String productName;

    @NotNull(message = "Vui lòng chọn danh mục hoa quả.")
    private Integer categoryId;

    @NotBlank(message = "Mô tả hoa quả không được để trống.")
    private String description;

    private String imageUrl;

    private MultipartFile imageFile;

    @Valid
    private List<VariantItemRequest> variants = new ArrayList<>();

    public List<VariantItemRequest> getEffectiveVariants() {
        List<VariantItemRequest> list = new ArrayList<>();
        if (variants != null) {
            for (VariantItemRequest v : variants) {
                if (v != null && v.getVariantName() != null && !v.getVariantName().trim().isEmpty()) {
                    list.add(v);
                }
            }
        }
        return list;
    }
}
