package com.fu.SWP391_BetaFruit.enums;

public enum InventoryTransactionType {
    RESTOCK,            // Shop nhập thêm hàng
    ORDER_DEDUCT,       // Trừ kho khi khách mua
    CANCEL_REFUND,      // Hoàn kho khi đơn bị hủy
    DAMAGE_ADJUSTMENT   // Chỉnh sửa khi hàng hỏng/mất
}
