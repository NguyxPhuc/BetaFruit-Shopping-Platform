package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.InventoryTransaction;
import com.fu.SWP391_BetaFruit.entity.ProductVariant;
import com.fu.SWP391_BetaFruit.enums.InventoryTransactionType;
import org.springframework.data.domain.Page;

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
     * Nhập thêm hàng cộng dồn vào kho (Restock Products)
     */
    ProductVariant restockVariant(Integer userId, Integer variantId, int additionalQuantity);

    /**
     * Lấy danh sách lịch sử giao dịch kho phân trang (Inventory History)
     */
    Page<InventoryTransaction> getShopInventoryHistory(Integer userId, InventoryTransactionType type, int page, int size);

    /**
     * Lấy danh sách các biến thể có tồn kho chạm ngưỡng cảnh báo (Low Stock Alerts)
     */
    List<ProductVariant> getLowStockVariants(Integer userId);

    /**
     * Đếm số lượng biến thể có tồn kho chạm ngưỡng cảnh báo
     */
    long countLowStockVariants(Integer userId);
}
