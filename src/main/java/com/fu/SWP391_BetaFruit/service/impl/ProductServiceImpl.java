package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.ProductCreateRequest;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import com.fu.SWP391_BetaFruit.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final MasterCategoryRepository masterCategoryRepository;
    private final NotificationService notificationService;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> searchProducts(String keyword, String tab) {
        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        boolean hasKeyword = !cleanKeyword.isEmpty();
        String currentTab = (tab != null && !tab.trim().isEmpty()) ? tab.trim().toUpperCase() : "ALL";

        return switch (currentTab) {
            case "PENDING" -> hasKeyword
                    ? productRepository.searchByApprovalStatus(cleanKeyword, ProductApprovalStatus.PENDING)
                    : productRepository.findByApprovalStatus(ProductApprovalStatus.PENDING);
            case "ACTIVE" -> hasKeyword
                    ? productRepository.searchByApprovalAndVisibility(cleanKeyword, ProductApprovalStatus.APPROVED, ProductVisibility.ACTIVE)
                    : productRepository.findByApprovalStatusAndVisibilityStatus(ProductApprovalStatus.APPROVED, ProductVisibility.ACTIVE);
            case "HIDDEN" -> hasKeyword
                    ? productRepository.searchByApprovalAndVisibility(cleanKeyword, ProductApprovalStatus.APPROVED, ProductVisibility.HIDDEN)
                    : productRepository.findByApprovalStatusAndVisibilityStatus(ProductApprovalStatus.APPROVED, ProductVisibility.HIDDEN);
            case "REJECTED" -> hasKeyword
                    ? productRepository.searchByApprovalStatus(cleanKeyword, ProductApprovalStatus.REJECTED)
                    : productRepository.findByApprovalStatus(ProductApprovalStatus.REJECTED);
            default -> hasKeyword
                    ? productRepository.searchAll(cleanKeyword)
                    : productRepository.findAll();
        };
    }

    @Override
    public Product getProductById(Integer productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm hoa quả với mã ID: " + productId));
    }

    @Override
    @Transactional
    public void approveProduct(Integer productId) {
        Product product = getProductById(productId);
        product.setApprovalStatus(ProductApprovalStatus.APPROVED);
        product.setVisibilityStatus(ProductVisibility.ACTIVE);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void rejectProduct(Integer productId) {
        Product product = getProductById(productId);
        product.setApprovalStatus(ProductApprovalStatus.REJECTED);
        product.setVisibilityStatus(ProductVisibility.HIDDEN);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void toggleProductVisibility(Integer productId) {
        Product product = getProductById(productId);
        if (product.getVisibilityStatus() == ProductVisibility.ACTIVE) {
            product.setVisibilityStatus(ProductVisibility.HIDDEN);
        } else {
            product.setVisibilityStatus(ProductVisibility.ACTIVE);
        }
        productRepository.save(product);
    }

    @Override
    public long countTotal() {
        return productRepository.count();
    }

    @Override
    public long countPending() {
        return productRepository.countByApprovalStatus(ProductApprovalStatus.PENDING);
    }

    @Override
    public long countActive() {
        return productRepository.countByApprovalStatusAndVisibilityStatus(ProductApprovalStatus.APPROVED, ProductVisibility.ACTIVE);
    }

    @Override
    public long countHidden() {
        return productRepository.countByApprovalStatusAndVisibilityStatus(ProductApprovalStatus.APPROVED, ProductVisibility.HIDDEN);
    }

    @Override
    public long countRejected() {
        return productRepository.countByApprovalStatus(ProductApprovalStatus.REJECTED);
    }

    private static final Set<String> ALLOWED_PRODUCT_SORT_FIELDS = Set.of(
            "productId", "productName", "approvalStatus", "visibilityStatus"
    );

    @Override
    public Page<Product> searchProductsPaginated(String keyword, String tab, int page, int size) {
        return searchProductsPaginated(keyword, tab, page, size, "productId", "desc");
    }

    @Override
    public Page<Product> searchProductsPaginated(String keyword, String tab, int page, int size, String sortBy, String sortDir) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 5;

        String safeSortBy = (sortBy != null && ALLOWED_PRODUCT_SORT_FIELDS.contains(sortBy.trim()))
                ? sortBy.trim() : "productId";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, safeSortBy));

        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        boolean hasKeyword = !cleanKeyword.isEmpty();
        String currentTab = (tab != null && !tab.trim().isEmpty()) ? tab.trim().toUpperCase() : "ALL";

        return switch (currentTab) {
            case "PENDING" -> hasKeyword
                    ? productRepository.searchByApprovalStatus(cleanKeyword, ProductApprovalStatus.PENDING, pageable)
                    : productRepository.findByApprovalStatus(ProductApprovalStatus.PENDING, pageable);
            case "ACTIVE" -> hasKeyword
                    ? productRepository.searchByApprovalAndVisibility(cleanKeyword, ProductApprovalStatus.APPROVED, ProductVisibility.ACTIVE, pageable)
                    : productRepository.findByApprovalStatusAndVisibilityStatus(ProductApprovalStatus.APPROVED, ProductVisibility.ACTIVE, pageable);
            case "HIDDEN" -> hasKeyword
                    ? productRepository.searchByApprovalAndVisibility(cleanKeyword, ProductApprovalStatus.APPROVED, ProductVisibility.HIDDEN, pageable)
                    : productRepository.findByApprovalStatusAndVisibilityStatus(ProductApprovalStatus.APPROVED, ProductVisibility.HIDDEN, pageable);
            case "REJECTED" -> hasKeyword
                    ? productRepository.searchByApprovalStatus(cleanKeyword, ProductApprovalStatus.REJECTED, pageable)
                    : productRepository.findByApprovalStatus(ProductApprovalStatus.REJECTED, pageable);
            default -> hasKeyword
                    ? productRepository.searchAll(cleanKeyword, pageable)
                    : productRepository.findAll(pageable);
        };
    }

    @Override
    public Map<String, Object> getAdminProductPageData(String keyword, String tab) {
        return getAdminProductPageData(keyword, tab, 1, 5, "productId", "desc");
    }

    @Override
    public Map<String, Object> getAdminProductPageData(String keyword, String tab, int page, int size) {
        return getAdminProductPageData(keyword, tab, page, size, "productId", "desc");
    }

    @Override
    public Map<String, Object> getAdminProductPageData(String keyword, String tab, int page, int size, String sortBy, String sortDir) {
        String currentTab = (tab != null && !tab.trim().isEmpty()) ? tab.trim().toUpperCase() : "ALL";
        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        String safeSortBy = (sortBy != null && ALLOWED_PRODUCT_SORT_FIELDS.contains(sortBy.trim()))
                ? sortBy.trim() : "productId";
        String safeSortDir = "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";

        Page<Product> productPage = searchProductsPaginated(cleanKeyword, currentTab, page, size, safeSortBy, safeSortDir);

        Map<String, Object> data = new HashMap<>();
        data.put("products", productPage.getContent());
        data.put("productPage", productPage);
        data.put("pageData", productPage);
        data.put("currentTab", currentTab);
        data.put("keyword", cleanKeyword);
        data.put("sortBy", safeSortBy);
        data.put("sortDir", safeSortDir);
        data.put("totalCount", countTotal());
        data.put("pendingCount", countPending());
        data.put("activeCount", countActive());
        data.put("hiddenCount", countHidden());
        data.put("rejectedCount", countRejected());

        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public Shop getShopByUserId(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin tài khoản người dùng!"));
        return shopRepository.findByOwner(user)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản của bạn chưa được liên kết với cửa hàng nào!"));
    }

    @Override
    @Transactional
    public Product createProductForShop(Integer userId, ProductCreateRequest request) {
        Shop shop = getShopByUserId(userId);

        if (shop.getApprovalStatus() == ShopApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("Cửa hàng của bạn đã bị từ chối hoạt động. Không thể đăng bán hoa quả mới!");
        }

        MasterCategory category = masterCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục hoa quả đã chọn!"));

        if (Boolean.FALSE.equals(category.getIsActive())) {
            throw new IllegalArgumentException("Danh mục hoa quả này hiện đang tạm đóng, vui lòng chọn danh mục khác!");
        }

        // 1. Lưu Thực thể Product (Hoa quả)
        Product product = new Product();
        product.setShop(shop);
        product.setCategory(category);
        product.setProductName(request.getProductName().trim());
        product.setDescription(request.getDescription().trim());
        // Mặc định hoa quả mới đăng ký: chờ kiểm duyệt chất lượng (PENDING) và tạm ẩn (HIDDEN)
        product.setApprovalStatus(ProductApprovalStatus.PENDING);
        product.setVisibilityStatus(ProductVisibility.HIDDEN);

        Product savedProduct = productRepository.save(product);

        // 2. Lưu các Thực thể ProductVariant (Hỗ trợ nhiều quy cách đóng gói & Giá bán)
        java.util.List<com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest> variantList = request.getEffectiveVariants();
        if (variantList.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập ít nhất 1 thực thể (biến thể) cho hoa quả!");
        }

        for (com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest vReq : variantList) {
            ProductVariant variant = new ProductVariant();
            variant.setProduct(savedProduct);
            variant.setVariantName(vReq.getVariantName().trim());
            variant.setOriginalPrice(vReq.getOriginalPrice());
            
            // Xử lý giá khuyến mãi (SalePrice): phải > 0 và < OriginalPrice để thỏa mãn CHECK constraint
            if (vReq.getSalePrice() != null) {
                if (vReq.getSalePrice().compareTo(BigDecimal.ZERO) <= 0 || vReq.getSalePrice().compareTo(vReq.getOriginalPrice()) >= 0) {
                    throw new IllegalArgumentException("Giá khuyến mãi của thực thể '" + vReq.getVariantName() + "' phải lớn hơn 0 và nhỏ hơn giá niêm yết (" + vReq.getOriginalPrice() + " đ)!");
                }
                variant.setSalePrice(vReq.getSalePrice());
            } else {
                variant.setSalePrice(null);
            }

            variant.setCostPrice(vReq.getCostPrice() != null ? vReq.getCostPrice() : BigDecimal.ZERO);
            variant.setStockQuantity(vReq.getStockQuantity() != null ? vReq.getStockQuantity() : 0);
            variant.setLowStockThreshold(vReq.getLowStockThreshold() != null ? vReq.getLowStockThreshold() : 5);
            variant.setIsAvailable(true);

            productVariantRepository.save(variant);
        }

        // 3. Lưu Thực thể ProductImage (Ảnh đại diện hoa quả)
        ProductImage image = new ProductImage();
        image.setProduct(savedProduct);
        image.setImageUrl(request.getImageUrl().trim());
        image.setIsPrimary(true);

        productImageRepository.save(image);

        // 4. Tạo thông báo hệ thống gửi đến tài khoản chủ Shop
        try {
            notificationService.createBaseNotification(
                    shop.getOwner(),
                    "Đăng ký hoa quả mới thành công",
                    "Sản phẩm '" + savedProduct.getProductName() + "' đã gửi kiểm duyệt thành công và đang chờ ban quản trị phê duyệt.",
                    NotificationType.SYSTEM_ALERT,
                    "/shop/products"
            );
        } catch (Exception ignored) {
            // Không làm gián đoạn luồng chính nếu có lỗi nhỏ khi tạo thông báo
        }

        return savedProduct;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> getShopProductsPaginated(Integer userId, String keyword, int page, int size) {
        Shop shop = getShopByUserId(userId);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "productId"));
        if (keyword != null && !keyword.trim().isEmpty()) {
            return productRepository.findByShop_ShopIdAndProductNameContainingIgnoreCase(shop.getShopId(), keyword.trim(), pageable);
        }
        return productRepository.findByShop_ShopId(shop.getShopId(), pageable);
    }

    @Override
    @Transactional
    public void toggleProductVisibilityForShop(Integer userId, Integer productId) {
        Shop shop = getShopByUserId(userId);
        Product product = getProductById(productId);
        if (!product.getShop().getShopId().equals(shop.getShopId())) {
            throw new IllegalArgumentException("Bạn không có quyền thao tác trên sản phẩm này!");
        }
        if (product.getApprovalStatus() != ProductApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Hoa quả đang ở trạng thái '" 
                    + (product.getApprovalStatus() == ProductApprovalStatus.PENDING ? "Chờ duyệt" : "Bị từ chối") 
                    + "', chỉ hoa quả 'Đã duyệt' mới có thể thay đổi trạng thái hiển thị!");
        }
        if (product.getVisibilityStatus() == ProductVisibility.ACTIVE) {
            product.setVisibilityStatus(ProductVisibility.HIDDEN);
        } else {
            product.setVisibilityStatus(ProductVisibility.ACTIVE);
        }
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getShopProductForEdit(Integer userId, Integer productId) {
        Shop shop = getShopByUserId(userId);
        Product product = getProductById(productId);
        if (!product.getShop().getShopId().equals(shop.getShopId())) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập sản phẩm này!");
        }
        return product;
    }

    @Override
    @Transactional
    public Product updateProductForShop(Integer userId, Integer productId, com.fu.SWP391_BetaFruit.dto.request.ProductUpdateRequest request) {
        Shop shop = getShopByUserId(userId);
        Product product = getProductById(productId);
        if (!product.getShop().getShopId().equals(shop.getShopId())) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa sản phẩm này!");
        }

        MasterCategory category = masterCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục hoa quả với mã ID: " + request.getCategoryId()));

        product.setProductName(request.getProductName().trim());
        product.setCategory(category);
        product.setDescription(request.getDescription().trim());

        // Nếu sản phẩm từng bị từ chối phê duyệt, khi chủ shop chỉnh sửa xong thì gửi lại về trạng thái PENDING
        if (product.getApprovalStatus() == ProductApprovalStatus.REJECTED) {
            product.setApprovalStatus(ProductApprovalStatus.PENDING);
        }

        // Cập nhật ảnh đại diện nếu có
        if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
            String newImgUrl = request.getImageUrl().trim();
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                ProductImage primaryImg = product.getImages().stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                        .findFirst()
                        .orElse(product.getImages().get(0));
                primaryImg.setImageUrl(newImgUrl);
                productImageRepository.save(primaryImg);
            } else {
                ProductImage newImg = new ProductImage();
                newImg.setProduct(product);
                newImg.setImageUrl(newImgUrl);
                newImg.setIsPrimary(true);
                productImageRepository.save(newImg);
            }
        }

        // Cập nhật thực thể (Variants)
        List<com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest> reqVariants = request.getEffectiveVariants();
        if (reqVariants.isEmpty()) {
            throw new IllegalArgumentException("Hoa quả phải có ít nhất 1 thực thể!");
        }

        List<ProductVariant> existingVariants = productVariantRepository.findByProduct_ProductId(productId);
        java.util.Set<Integer> keptVariantIds = new java.util.HashSet<>();

        for (com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest vReq : reqVariants) {
            if (vReq.getOriginalPrice() == null || vReq.getOriginalPrice().compareTo(new BigDecimal("1000")) < 0) {
                throw new IllegalArgumentException("Thực thể '" + vReq.getVariantName() + "' phải có giá niêm yết từ 1,000 VNĐ trở lên.");
            }
            if (vReq.getSalePrice() != null) {
                if (vReq.getSalePrice().compareTo(BigDecimal.ZERO) <= 0 || vReq.getSalePrice().compareTo(vReq.getOriginalPrice()) >= 0) {
                    throw new IllegalArgumentException("Giá khuyến mãi của thực thể '" + vReq.getVariantName() + "' phải lớn hơn 0 và nhỏ hơn giá niêm yết.");
                }
            }

            if (vReq.getVariantId() != null) {
                // Sửa biến thể có sẵn
                ProductVariant existing = existingVariants.stream()
                        .filter(v -> v.getVariantId().equals(vReq.getVariantId()))
                        .findFirst()
                        .orElse(null);
                if (existing != null) {
                    existing.setVariantName(vReq.getVariantName().trim());
                    existing.setOriginalPrice(vReq.getOriginalPrice());
                    existing.setSalePrice(vReq.getSalePrice());
                    existing.setCostPrice(vReq.getCostPrice() != null ? vReq.getCostPrice() : BigDecimal.ZERO);
                    existing.setStockQuantity(vReq.getStockQuantity() != null ? vReq.getStockQuantity() : 0);
                    existing.setLowStockThreshold(vReq.getLowStockThreshold() != null ? vReq.getLowStockThreshold() : 5);
                    existing.setIsAvailable(vReq.getIsAvailable() != null ? vReq.getIsAvailable() : true);
                    productVariantRepository.save(existing);
                    keptVariantIds.add(existing.getVariantId());
                }
            } else {
                // Tạo biến thể mới thêm vào
                ProductVariant newV = new ProductVariant();
                newV.setProduct(product);
                newV.setVariantName(vReq.getVariantName().trim());
                newV.setOriginalPrice(vReq.getOriginalPrice());
                newV.setSalePrice(vReq.getSalePrice());
                newV.setCostPrice(vReq.getCostPrice() != null ? vReq.getCostPrice() : BigDecimal.ZERO);
                newV.setStockQuantity(vReq.getStockQuantity() != null ? vReq.getStockQuantity() : 0);
                newV.setLowStockThreshold(vReq.getLowStockThreshold() != null ? vReq.getLowStockThreshold() : 5);
                newV.setIsAvailable(true);
                ProductVariant savedV = productVariantRepository.save(newV);
                keptVariantIds.add(savedV.getVariantId());
            }
        }

        // Với các biến thể đã bị người dùng xóa khỏi form: xóa hoặc soft-delete
        for (ProductVariant existing : existingVariants) {
            if (!keptVariantIds.contains(existing.getVariantId())) {
                try {
                    productVariantRepository.delete(existing);
                } catch (Exception ex) {
                    existing.setIsAvailable(false);
                    productVariantRepository.save(existing);
                }
            }
        }

        return productRepository.save(product);
    }
}
