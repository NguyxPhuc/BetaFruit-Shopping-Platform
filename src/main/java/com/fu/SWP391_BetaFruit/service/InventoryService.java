package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.ProductVariant;

import java.util.List;

public interface InventoryService {
    /**
     * Bật / Tắt trạng thái mở bán biến thể (Manage Variant Availability)
     */
    boolean toggleVariantAvailability(Integer userId, Integer variantId);

    /**
     * Cập nhật số lượng tồn kho trực tiếp theo kết quả kiểm kê (Update Stock Quantity)
     */
    ProductVariant updateStockQuantity(Integer userId, Integer variantId, int newStock);

    /**
     * Lấy danh sách các biến thể có tồn kho chạm ngưỡng cảnh báo (Low Stock Alerts)
     */
    List<ProductVariant> getLowStockVariants(Integer userId);

    /**
     * Đếm số lượng biến thể có tồn kho chạm ngưỡng cảnh báo
     */
    long countLowStockVariants(Integer userId);
}
