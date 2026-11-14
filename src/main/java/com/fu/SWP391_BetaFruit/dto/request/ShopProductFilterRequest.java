package com.fu.SWP391_BetaFruit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO nhận các tiêu chí tìm kiếm, phân loại và bộ lọc nâng cao
 * cho màn hình Quản lý danh sách hoa quả của Shop Owner (/shop/products).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopProductFilterRequest {

    /**
     * Từ khóa tìm kiếm theo tên hoa quả
     */
    private String keyword;

    /**
     * Tab trạng thái kiểm duyệt & mở bán:
     * ALL (Tất cả), ACTIVE (Đang bán), PENDING (Chờ duyệt),
     * HIDDEN (Tạm ẩn), REJECTED (Bị từ chối), LOW_STOCK (Cảnh báo kho / Hết hàng)
     */
    @Builder.Default
    private String tab = "ALL";

    /**
     * Lọc theo Danh mục hoa quả (MasterCategory ID)
     */
    private Integer categoryId;

    /**
     * Lọc theo tình trạng kho hàng:
     * ALL (Tất cả), IN_STOCK (Còn hàng an toàn),
     * LOW_STOCK (Sắp hết hàng <= 5), OUT_OF_STOCK (Đã hết hàng = 0)
     */
    @Builder.Default
    private String stockStatus = "ALL";

    /**
     * Khoảng giá định sẵn (Price Range Presets):
     * ALL (Tất cả mức giá),
     * UNDER_50K (Dưới 50.000 đ),
     * 50K_150K (50.000 đ - 150.000 đ),
     * 150K_300K (150.000 đ - 300.000 đ),
     * OVER_300K (Trên 300.000 đ),
     * CUSTOM (Khoảng giá tùy chỉnh theo minPrice & maxPrice)
     */
    @Builder.Default
    private String priceRange = "ALL";

    /**
     * Giá tối thiểu tùy chỉnh (VND)
     */
    private BigDecimal minPrice;

    /**
     * Giá tối đa tùy chỉnh (VND)
     */
    private BigDecimal maxPrice;

    /**
     * Tiêu chí sắp xếp:
     * newest (Mới nhất), oldest (Cũ nhất),
     * price_asc (Giá: Thấp đến Cao), price_desc (Giá: Cao đến Thấp),
     * stock_asc (Tồn kho: Ít đến Nhiều), stock_desc (Tồn kho: Nhiều đến Ít),
     * name_asc (Tên: A -> Z), name_desc (Tên: Z -> A)
     */
    @Builder.Default
    private String sortBy = "newest";

    /**
     * Trang hiện tại (1-based)
     */
    @Builder.Default
    private int page = 1;

    /**
     * Số phần tử trên mỗi trang
     */
    @Builder.Default
    private int size = 5;

    /**
     * Kiểm tra xem người dùng có đang áp dụng bất kỳ bộ lọc nào khác mặc định không
     */
    public boolean hasActiveFilters() {
        return (keyword != null && !keyword.trim().isEmpty())
                || (tab != null && !"ALL".equalsIgnoreCase(tab))
                || (categoryId != null && categoryId > 0)
                || (stockStatus != null && !"ALL".equalsIgnoreCase(stockStatus))
                || (priceRange != null && !"ALL".equalsIgnoreCase(priceRange))
                || (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) > 0)
                || (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) > 0)
                || (sortBy != null && !"newest".equalsIgnoreCase(sortBy));
    }
}

