package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.InventoryTransaction;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.entity.ProductVariant;
import com.fu.SWP391_BetaFruit.enums.InventoryTransactionType;
import com.fu.SWP391_BetaFruit.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Runs inside the order-status transaction, after the order row has been locked. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class OrderStockService {
    private final OrderItemRepository items;
    private final ProductVariantRepository variants;
    private final InventoryTransactionRepository transactions;
    private final UserRepository users;

    public OrderStockService(OrderItemRepository items, ProductVariantRepository variants,
                             InventoryTransactionRepository transactions, UserRepository users) {
        this.items = items;
        this.variants = variants;
        this.transactions = transactions;
        this.users = users;
    }

    public void deduct(Order order, Integer ownerId) {
        updateStock(order, ownerId, true);
    }

    public void restore(Order order, Integer ownerId) {
        updateStock(order, ownerId, false);
    }

    private void updateStock(Order order, Integer ownerId, boolean deduct) {
        // Aggregate repeated variants and lock in the same order across all orders.
        Map<Integer, Integer> quantities = new TreeMap<>();
        for (var item : items.findByOrderOrderIdOrderByOrderItemIdAsc(order.getOrderId())) {
            if (item.getVariant() == null || item.getVariant().getVariantId() == null
                    || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw conflict("Đơn hàng có sản phẩm hoặc số lượng không hợp lệ");
            }
            try {
                quantities.merge(item.getVariant().getVariantId(), item.getQuantity(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw conflict("Tổng số lượng sản phẩm vượt giới hạn tồn kho");
            }
        }
        if (quantities.isEmpty()) {
            throw conflict("Đơn hàng chưa có sản phẩm để cập nhật tồn kho");
        }

        var changes = new ArrayList<StockChange>();
        for (var entry : quantities.entrySet()) {
            ProductVariant variant = variants.findForStockUpdate(entry.getKey())
                    .orElseThrow(() -> conflict("Không tìm thấy biến thể #" + entry.getKey()));
            if (variant.getProduct() == null || variant.getProduct().getShop() == null
                    || order.getShop() == null || order.getShop().getShopId() == null
                    || !Objects.equals(variant.getProduct().getShop().getShopId(), order.getShop().getShopId())) {
                throw conflict("Sản phẩm không thuộc cửa hàng của đơn hàng");
            }
            Integer previous = variant.getStockQuantity();
            if (previous == null || previous < 0) {
                throw conflict("Tồn kho không hợp lệ cho biến thể #" + entry.getKey());
            }
            int quantity = entry.getValue();
            if (deduct && previous < quantity) {
                throw conflict("Không đủ tồn kho cho biến thể #" + entry.getKey()
                        + ": cần " + quantity + ", còn " + previous);
            }
            int delta = deduct ? -quantity : quantity;
            try {
                changes.add(new StockChange(variant, previous, Math.addExact(previous, delta), delta));
            } catch (ArithmeticException exception) {
                throw conflict("Tồn kho sau khi hoàn hàng vượt giới hạn cho phép");
            }
        }

        // Validate every line before mutating anything; database failures roll back the whole transaction.
        var actor = users.getReferenceById(ownerId);
        for (var change : changes) {
            change.variant().setStockQuantity(change.next());
            transactions.save(new InventoryTransaction(null, change.variant(), actor,
                    deduct ? InventoryTransactionType.ORDER_DEDUCT : InventoryTransactionType.CANCEL_REFUND,
                    change.delta(), change.previous(), change.next(), null));
        }
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private record StockChange(ProductVariant variant, int previous, int next, int delta) {}
}
