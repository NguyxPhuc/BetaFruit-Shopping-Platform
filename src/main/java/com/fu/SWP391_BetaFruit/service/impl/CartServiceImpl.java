package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.*;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository itemRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final ProductImageRepository imageRepository;

    @Override
    @Transactional(readOnly = true)
    public long getItemCount(Integer customerId) {
        return customerId == null ? 0 : itemRepository.sumQuantityByCustomerId(customerId);
    }

    @Override
    public void addItem(Integer customerId, Integer variantId, Integer quantity) {
        checkQuantity(quantity);
        User customer = requireCustomer(customerId, true);
        if (variantId == null || variantId <= 0) throw new IllegalArgumentException("Vui lòng chọn phân loại hợp lệ.");
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Phân loại không tồn tại."));
        Cart cart = cartRepository.findByCustomer_UserId(customerId).orElse(null);
        CartItem item = cart == null ? null : itemRepository
                .findByCart_CartIdAndVariant_VariantId(cart.getCartId(), variantId).orElse(null);
        long combined = (item == null ? 0L : item.getQuantity().longValue()) + quantity;
        if (combined > Integer.MAX_VALUE) throw new IllegalArgumentException("Số lượng vượt giới hạn.");
        validate(variant, (int) combined);
        if (cart == null) {
            cart = new Cart();
            cart.setCustomer(customer);
            cart = cartRepository.save(cart);
        }
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setVariant(variant);
        }
        item.setQuantity((int) combined);
        itemRepository.save(item);
    }

    @Override
    public void updateQuantity(Integer customerId, Integer itemId, Integer quantity) {
        checkQuantity(quantity);
        requireCustomer(customerId, true);
        CartItem item = ownedItem(customerId, itemId);
        validate(item.getVariant(), quantity);
        item.setQuantity(quantity);
        itemRepository.save(item);
    }

    @Override
    public void removeItem(Integer customerId, Integer itemId) {
        requireCustomer(customerId, true);
        itemRepository.delete(ownedItem(customerId, itemId));
    }

    @Override
    public void clearCart(Integer customerId) {
        requireCustomer(customerId, true);
        cartRepository.findByCustomer_UserId(customerId)
                .ifPresent(cart -> itemRepository.deleteByCart_CartId(cart.getCartId()));
    }

    @Override
    public void clearShopCart(Integer customerId, Integer shopId) {
        requireCustomer(customerId, true);
        if (shopId == null || shopId <= 0) throw new IllegalArgumentException("Cửa hàng không hợp lệ.");
        cartRepository.findByCustomer_UserId(customerId).ifPresent(cart ->
                itemRepository.deleteByCart_CartIdAndVariant_Product_Shop_ShopId(cart.getCartId(), shopId));
    }

    @Override
    @Transactional(readOnly = true)
    public CartSummaryResponse getSummary(Integer customerId) {
        requireCustomer(customerId, false);
        Cart cart = cartRepository.findByCustomer_UserId(customerId).orElse(null);
        if (cart == null) return new CartSummaryResponse(List.of(), 0, BigDecimal.ZERO, true);
        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        long quantity = 0;
        boolean valid = true;
        var cartItems = itemRepository.findSummaryItems(cart.getCartId());
        var productIds = cartItems.stream().map(item -> item.getVariant().getProduct().getProductId()).distinct().toList();
        var imageUrls = new java.util.HashMap<Integer, String>();
        var primaryIds = new java.util.HashSet<Integer>();
        if (!productIds.isEmpty()) {
            for (var image : imageRepository.findByProduct_ProductIdInOrderByImageIdAsc(productIds)) {
                if (image.getImageUrl() == null || image.getImageUrl().isBlank()) continue;
                Integer productId = image.getProduct().getProductId();
                if (!imageUrls.containsKey(productId) || (Boolean.TRUE.equals(image.getIsPrimary()) && primaryIds.add(productId))) {
                    imageUrls.put(productId, image.getImageUrl().trim());
                    if (Boolean.TRUE.equals(image.getIsPrimary())) primaryIds.add(productId);
                }
            }
        }
        for (CartItem item : cartItems) {
            ProductVariant variant = item.getVariant();
            String warning = null;
            BigDecimal price = BigDecimal.ZERO;
            try {
                checkQuantity(item.getQuantity());
                price = validate(variant, item.getQuantity());
            } catch (IllegalArgumentException ex) {
                warning = ex.getMessage();
                valid = false;
            }
            BigDecimal subtotal = warning == null ? OrderPricing.lineTotal(price, item.getQuantity()) : BigDecimal.ZERO;
            total = total.add(subtotal);
            quantity += item.getQuantity();
            Product product = variant.getProduct();
            items.add(new CartItemResponse(item.getCartItemId(), product.getProductId(), product.getProductName(),
                    variant.getVariantName(), product.getShop().getShopId(), product.getShop().getShopName(), item.getQuantity(),
                    variant.getStockQuantity(), price, subtotal, warning == null, warning, imageUrls.get(product.getProductId())));
        }
        return new CartSummaryResponse(List.copyOf(items), quantity, total, valid);
    }

    private User requireCustomer(Integer id, boolean lock) {
        if (id == null) throw new IllegalArgumentException("Vui lòng đăng nhập.");
        User user = (lock ? userRepository.findCartCustomerForUpdate(id) : userRepository.findById(id))
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại."));
        if (user.getStatus() != UserStatus.ACTIVE || user.getRoles().stream()
                .noneMatch(role -> role.getRoleName() == RoleName.CUSTOMER)) {
            throw new IllegalArgumentException("Tài khoản không có quyền sử dụng giỏ hàng.");
        }
        return user;
    }

    private CartItem ownedItem(Integer customerId, Integer itemId) {
        if (itemId == null) throw new IllegalArgumentException("Sản phẩm không có trong giỏ hàng.");
        return itemRepository.findByCartItemIdAndCart_Customer_UserId(itemId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không có trong giỏ hàng của bạn."));
    }

    private void checkQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) throw new IllegalArgumentException("Số lượng phải là số nguyên lớn hơn 0.");
    }

    private BigDecimal validate(ProductVariant variant, int quantity) {
        if (variant == null || !Boolean.TRUE.equals(variant.getIsAvailable()))
            throw new IllegalArgumentException("Phân loại hiện không khả dụng.");
        Product product = variant.getProduct();
        if (product == null || product.getApprovalStatus() != ProductApprovalStatus.APPROVED
                || product.getVisibilityStatus() != ProductVisibility.ACTIVE)
            throw new IllegalArgumentException("Sản phẩm hiện không được bán.");
        Shop shop = product.getShop();
        if (shop == null || shop.getApprovalStatus() != ShopApprovalStatus.APPROVED
                || shop.getOwner() == null || shop.getOwner().getStatus() != UserStatus.ACTIVE)
            throw new IllegalArgumentException("Cửa hàng hiện không hoạt động.");
        if (variant.getStockQuantity() == null || variant.getStockQuantity() < quantity)
            throw new IllegalArgumentException("Số lượng vượt quá tồn kho hiện tại.");
        BigDecimal original = variant.getOriginalPrice();
        BigDecimal sale = variant.getSalePrice();
        if (original == null || original.signum() <= 0 || (sale != null
                && (sale.signum() <= 0 || sale.compareTo(original) >= 0)))
            throw new IllegalArgumentException("Giá sản phẩm không hợp lệ. Vui lòng thử lại sau.");
        return sale != null && sale.signum() > 0 ? sale : original;
    }
}
