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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {

    @NotBlank(message = "Tên hoa quả không được để trống.")
    @Size(max = 255, message = "Tên hoa quả tối đa 255 ký tự.")
    private String productName;

    @NotNull(message = "Vui lòng chọn danh mục hoa quả.")
    private Integer categoryId;

    @NotBlank(message = "Mô tả hoa quả không được để trống.")
    private String description;

    private String imageUrl;

    private MultipartFile imageFile;

    // --- Danh sách nhiều quy cách / biến thể bán hàng ---
    @Valid
    private List<VariantItemRequest> variants = new ArrayList<>();

    // Các trường dự phòng tương thích ngược đơn lẻ
    private String variantName;
    private BigDecimal originalPrice;
    private BigDecimal costPrice;
    private Integer stockQuantity;
    private Integer lowStockThreshold = 5;

    /**
     * Lấy danh sách biến thể hợp lệ (ưu tiên danh sách variants, nếu rỗng thì lấy từ trường đơn lẻ)
     */
    public List<VariantItemRequest> getEffectiveVariants() {
        List<VariantItemRequest> list = new ArrayList<>();
        if (variants != null) {
            for (VariantItemRequest v : variants) {
                if (v != null && v.getVariantName() != null && !v.getVariantName().trim().isEmpty()) {
                    list.add(v);
                }
            }
        }
        if (list.isEmpty() && variantName != null && !variantName.trim().isEmpty()) {
            VariantItemRequest fallback = new VariantItemRequest(variantName, originalPrice, costPrice, stockQuantity, lowStockThreshold);
            list.add(fallback);
        }
        return list;
    }
}
