package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.InventoryTransaction;
import com.fu.SWP391_BetaFruit.entity.ProductVariant;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.InventoryTransactionType;
import com.fu.SWP391_BetaFruit.repository.InventoryTransactionRepository;
import com.fu.SWP391_BetaFruit.repository.ProductVariantRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    private Shop getShopOrThrow(Integer userId) {
        return shopRepository.findByOwner_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin cửa hàng gắn với tài khoản của bạn!"));
    }

    private User getUserOrThrow(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin người dùng!"));
    }

    private ProductVariant getVariantAndVerifyShop(Integer userId, Integer variantId) {
        Shop shop = getShopOrThrow(userId);
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thực thể hoa quả với ID: " + variantId));

        if (!variant.getProduct().getShop().getShopId().equals(shop.getShopId())) {
            throw new IllegalArgumentException("Bạn không có quyền quản trị biến thể của cửa hàng khác!");
        }
        return variant;
    }

    @Override
    @Transactional
    public boolean toggleVariantAvailability(Integer userId, Integer variantId) {
        ProductVariant variant = getVariantAndVerifyShop(userId, variantId);
        boolean current = Boolean.TRUE.equals(variant.getIsAvailable());
        variant.setIsAvailable(!current);
        productVariantRepository.save(variant);
        return variant.getIsAvailable();
    }

    @Override
    @Transactional
    public ProductVariant updateStockQuantity(Integer userId, Integer variantId, int newStock) {
        if (newStock < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho không được âm!");
        }

        User user = getUserOrThrow(userId);
        ProductVariant variant = getVariantAndVerifyShop(userId, variantId);

        int previous = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
        int diff = newStock - previous;

        variant.setStockQuantity(newStock);
        ProductVariant saved = productVariantRepository.save(variant);

        // Ghi lại lịch sử giao dịch điều chỉnh kiểm kê
        InventoryTransaction tx = new InventoryTransaction();
        tx.setVariant(saved);
        tx.setUser(user);
        tx.setTransactionType(InventoryTransactionType.DAMAGE_ADJUSTMENT);
        tx.setPreviousStock(previous);
        tx.setNewStock(newStock);
        tx.setQuantityChanged(diff);
        inventoryTransactionRepository.save(tx);

        return saved;
    }

    @Override
    @Transactional
    public ProductVariant restockVariant(Integer userId, Integer variantId, int additionalQuantity) {
        if (additionalQuantity <= 0) {
            throw new IllegalArgumentException("Số lượng nhập thêm phải lớn hơn 0!");
        }

        User user = getUserOrThrow(userId);
        ProductVariant variant = getVariantAndVerifyShop(userId, variantId);

        int previous = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
        int newStock = previous + additionalQuantity;

        variant.setStockQuantity(newStock);
        ProductVariant saved = productVariantRepository.save(variant);

        // Ghi lại lịch sử giao dịch nhập thêm hàng
        InventoryTransaction tx = new InventoryTransaction();
        tx.setVariant(saved);
        tx.setUser(user);
        tx.setTransactionType(InventoryTransactionType.RESTOCK);
        tx.setPreviousStock(previous);
        tx.setNewStock(newStock);
        tx.setQuantityChanged(additionalQuantity);
        inventoryTransactionRepository.save(tx);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryTransaction> getShopInventoryHistory(Integer userId, InventoryTransactionType type, int page, int size) {
        Shop shop = getShopOrThrow(userId);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);

        if (type != null) {
            return inventoryTransactionRepository.findByShopIdAndType(shop.getShopId(), type, pageable);
        }
        return inventoryTransactionRepository.findByShopId(shop.getShopId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariant> getLowStockVariants(Integer userId) {
        Shop shop = getShopOrThrow(userId);
        return productVariantRepository.findLowStockVariantsByShopId(shop.getShopId());
    }

    @Override
    @Transactional(readOnly = true)
    public long countLowStockVariants(Integer userId) {
        Shop shop = getShopOrThrow(userId);
        return productVariantRepository.countLowStockVariantsByShopId(shop.getShopId());
    }
}
