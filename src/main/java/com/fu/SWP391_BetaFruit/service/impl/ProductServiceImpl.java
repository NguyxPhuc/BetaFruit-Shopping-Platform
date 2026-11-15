package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.ProductCreateRequest;
import com.fu.SWP391_BetaFruit.dto.request.ProductUpdateRequest;
import com.fu.SWP391_BetaFruit.dto.request.ShopProductFilterRequest;
import com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import com.fu.SWP391_BetaFruit.service.ProductService;
import com.fu.SWP391_BetaFruit.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final StorageService storageService;

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
    public Page<Product> searchProductsPaginated(String keyword, String tab, int page, int size, String sortBy, String sortDir) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 5;

        String safeSortBy = (sortBy != null && ALLOWED_PRODUCT_SORT_FIELDS.contains(sortBy.trim()))
                ? sortBy.trim() : "productId";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
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
    public Map<String, Object> getAdminProductPageData(String keyword, String tab, int page, int size, String sortBy, String sortDir) {
        String currentTab = (tab != null && !tab.trim().isEmpty()) ? tab.trim().toUpperCase() : "ALL";
        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        String safeSortBy = (sortBy != null && ALLOWED_PRODUCT_SORT_FIELDS.contains(sortBy.trim()))
                ? sortBy.trim() : "productId";
        String safeSortDir = "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";

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

        validateProductTextFields(request.getProductName(), request.getDescription());

        List<VariantItemRequest> variantList = request.getEffectiveVariants();
        if (variantList.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập ít nhất 1 thực thể (biến thể) cho hoa quả!");
        }

        for (int i = 0; i < variantList.size(); i++) {
            validateVariantItem(variantList.get(i), i + 1);
        }

        // Lưu ảnh lên MinIO nếu có MultipartFile tải lên
        String imageUrl;
        String uploadedMinioUrl = null;
        MultipartFile imageFile = request.getImageFile();
        if (imageFile != null && !imageFile.isEmpty()) {
            storageService.validateImage(imageFile);
            uploadedMinioUrl = storageService.storeImage(imageFile, "products");
            imageUrl = uploadedMinioUrl;
        } else if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
            imageUrl = request.getImageUrl().trim();
        } else {
            throw new IllegalArgumentException("Vui lòng tải lên hình ảnh đại diện của hoa quả!");
        }

        Product savedProduct;
        try {
            // 1. Lưu Thực thể Product (Hoa quả)
            Product product = new Product();
            product.setShop(shop);
            product.setCategory(category);
            product.setProductName(request.getProductName().trim());
            product.setDescription(request.getDescription().trim());
            // Mặc định hoa quả mới đăng ký: chờ kiểm duyệt chất lượng (PENDING) và tạm ẩn (HIDDEN)
            product.setApprovalStatus(ProductApprovalStatus.PENDING);
            product.setVisibilityStatus(ProductVisibility.HIDDEN);

            savedProduct = productRepository.save(product);

            // 2. Lưu các Thực thể ProductVariant (Hỗ trợ nhiều quy cách đóng gói & Giá bán)
            for (VariantItemRequest vReq : variantList) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(savedProduct);
                variant.setVariantName(vReq.getVariantName().trim());
                variant.setOriginalPrice(vReq.getOriginalPrice());
                variant.setSalePrice(vReq.getSalePrice());
                variant.setCostPrice(vReq.getCostPrice() != null ? vReq.getCostPrice() : BigDecimal.ZERO);
                variant.setStockQuantity(vReq.getStockQuantity() != null ? vReq.getStockQuantity() : 0);
                variant.setLowStockThreshold(vReq.getLowStockThreshold() != null ? vReq.getLowStockThreshold() : 5);
                variant.setIsAvailable(true);

                productVariantRepository.save(variant);
            }

            // 3. Lưu Thực thể ProductImage (Ảnh đại diện hoa quả)
            ProductImage image = new ProductImage();
            image.setProduct(savedProduct);
            image.setImageUrl(imageUrl);
            image.setIsPrimary(true);

            productImageRepository.save(image);
        } catch (Exception ex) {
            if (uploadedMinioUrl != null) {
                storageService.clearUp(List.of(uploadedMinioUrl));
            }
            throw ex;
        }

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
        ShopProductFilterRequest request = ShopProductFilterRequest.builder()
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();
        Map<String, Object> data = getShopProductPageData(userId, request);
        return (Page<Product>) data.get("productPage");
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getShopProductPageData(Integer userId, ShopProductFilterRequest filterRequest) {
        if (filterRequest == null) {
            filterRequest = new ShopProductFilterRequest();
        }

        Shop shop = getShopByUserId(userId);
        List<Product> allShopProducts = productRepository.findByShop_ShopIdOrderByProductIdDesc(shop.getShopId());

        // Eager-touch collections to avoid any lazy loading issues in Thymeleaf view
        allShopProducts.forEach(p -> {
            if (p.getVariants() != null) p.getVariants().size();
            if (p.getImages() != null) p.getImages().size();
            if (p.getCategory() != null) p.getCategory().getCategoryName();
        });

        // 1. Calculate live tab counters across all products of the shop
        long totalCount = allShopProducts.size();
        long activeCount = allShopProducts.stream()
                .filter(p -> p.getApprovalStatus() == ProductApprovalStatus.APPROVED
                        && p.getVisibilityStatus() == ProductVisibility.ACTIVE)
                .count();
        long pendingCount = allShopProducts.stream()
                .filter(p -> p.getApprovalStatus() == ProductApprovalStatus.PENDING)
                .count();
        long hiddenCount = allShopProducts.stream()
                .filter(p -> p.getVisibilityStatus() == ProductVisibility.HIDDEN)
                .count();
        long rejectedCount = allShopProducts.stream()
                .filter(p -> p.getApprovalStatus() == ProductApprovalStatus.REJECTED)
                .count();
        long lowStockTabCount = allShopProducts.stream()
                .filter(this::isProductLowStockOrOutOfStock)
                .count();

        // 2. Resolve price range boundaries
        BigDecimal effectiveMinPrice = null;
        BigDecimal effectiveMaxPrice = null;
        String priceRange = filterRequest.getPriceRange() != null ? filterRequest.getPriceRange().trim().toUpperCase() : "ALL";

        switch (priceRange) {
            case "UNDER_50K":
                effectiveMaxPrice = new BigDecimal("50000");
                break;
            case "50K_150K":
                effectiveMinPrice = new BigDecimal("50000");
                effectiveMaxPrice = new BigDecimal("150000");
                break;
            case "150K_300K":
                effectiveMinPrice = new BigDecimal("150000");
                effectiveMaxPrice = new BigDecimal("300000");
                break;
            case "OVER_300K":
                effectiveMinPrice = new BigDecimal("300000");
                break;
            case "CUSTOM":
            default:
                if (filterRequest.getMinPrice() != null && filterRequest.getMinPrice().compareTo(BigDecimal.ZERO) > 0) {
                    effectiveMinPrice = filterRequest.getMinPrice();
                }
                if (filterRequest.getMaxPrice() != null && filterRequest.getMaxPrice().compareTo(BigDecimal.ZERO) > 0) {
                    effectiveMaxPrice = filterRequest.getMaxPrice();
                }
                break;
        }

        final BigDecimal finalMinPrice = effectiveMinPrice;
        final BigDecimal finalMaxPrice = effectiveMaxPrice;
        final String tab = filterRequest.getTab() != null ? filterRequest.getTab().trim().toUpperCase() : "ALL";
        final String keyword = filterRequest.getKeyword() != null ? filterRequest.getKeyword().trim().toLowerCase() : "";
        final Integer categoryId = filterRequest.getCategoryId();
        final String stockStatus = filterRequest.getStockStatus() != null ? filterRequest.getStockStatus().trim().toUpperCase() : "ALL";

        // 3. Filter stream
        List<Product> filteredProducts = allShopProducts.stream()
                // Filter by Tab
                .filter(p -> {
                    switch (tab) {
                        case "ACTIVE":
                            return p.getApprovalStatus() == ProductApprovalStatus.APPROVED
                                    && p.getVisibilityStatus() == ProductVisibility.ACTIVE;
                        case "PENDING":
                            return p.getApprovalStatus() == ProductApprovalStatus.PENDING;
                        case "HIDDEN":
                            return p.getVisibilityStatus() == ProductVisibility.HIDDEN;
                        case "REJECTED":
                            return p.getApprovalStatus() == ProductApprovalStatus.REJECTED;
                        case "LOW_STOCK":
                            return isProductLowStockOrOutOfStock(p);
                        case "ALL":
                        default:
                            return true;
                    }
                })
                // Filter by Keyword
                .filter(p -> {
                    if (keyword.isEmpty()) return true;
                    return p.getProductName() != null && p.getProductName().toLowerCase().contains(keyword);
                })
                // Filter by Category
                .filter(p -> {
                    if (categoryId == null || categoryId <= 0) return true;
                    return p.getCategory() != null && categoryId.equals(p.getCategory().getCategoryId());
                })
                // Filter by Stock Status
                .filter(p -> {
                    switch (stockStatus) {
                        case "IN_STOCK":
                            return isProductHealthyStock(p);
                        case "LOW_STOCK":
                            return isProductLowStockOnly(p);
                        case "OUT_OF_STOCK":
                            return isProductOutOfStock(p);
                        case "ALL":
                        default:
                            return true;
                    }
                })
                // Filter by Price Range
                .filter(p -> matchesPrice(p, finalMinPrice, finalMaxPrice))
                .collect(Collectors.toList());

        // 4. Sort stream
        String sortBy = filterRequest.getSortBy() != null ? filterRequest.getSortBy().trim().toLowerCase() : "newest";
        Comparator<Product> comparator;
        switch (sortBy) {
            case "oldest":
                comparator = Comparator.comparing(Product::getProductId, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "price_asc":
                comparator = Comparator.comparing(this::getLowestPrice, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "price_desc":
                comparator = Comparator.comparing(this::getLowestPrice, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
            case "stock_asc":
                comparator = Comparator.comparing(this::getTotalStock, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "stock_desc":
                comparator = Comparator.comparing(this::getTotalStock, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
            case "name_asc":
                comparator = Comparator.comparing(p -> p.getProductName() != null ? p.getProductName().toLowerCase() : "", Comparator.naturalOrder());
                break;
            case "name_desc":
                comparator = Comparator.comparing(p -> p.getProductName() != null ? p.getProductName().toLowerCase() : "", Comparator.reverseOrder());
                break;
            case "newest":
            default:
                comparator = Comparator.comparing(Product::getProductId, Comparator.nullsLast(Comparator.reverseOrder()));
                break;
        }
        filteredProducts.sort(comparator);

        // 5. Pagination
        int pageSize = filterRequest.getSize() > 0 ? filterRequest.getSize() : 5;
        int totalItems = filteredProducts.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) totalPages = 1;

        int currentPage = filterRequest.getPage();
        if (currentPage < 1) currentPage = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        List<Product> pagedList = (fromIndex < totalItems) ? filteredProducts.subList(fromIndex, toIndex) : Collections.emptyList();

        Page<Product> productPage = new PageImpl<>(pagedList, PageRequest.of(currentPage - 1, pageSize), totalItems);

        // 6. Build response data map
        Map<String, Object> data = new HashMap<>();
        data.put("products", pagedList);
        data.put("productPage", productPage);
        data.put("currentPage", currentPage);
        data.put("totalPages", totalPages);
        data.put("totalItems", totalItems);
        data.put("keyword", filterRequest.getKeyword() != null ? filterRequest.getKeyword().trim() : "");
        data.put("tab", tab);
        data.put("categoryId", categoryId);
        data.put("stockStatus", stockStatus);
        data.put("priceRange", priceRange);
        data.put("minPrice", filterRequest.getMinPrice());
        data.put("maxPrice", filterRequest.getMaxPrice());
        data.put("sortBy", sortBy);
        data.put("hasActiveFilters", filterRequest.hasActiveFilters());

        // Tab counts
        data.put("totalCount", totalCount);
        data.put("activeCount", activeCount);
        data.put("pendingCount", pendingCount);
        data.put("hiddenCount", hiddenCount);
        data.put("rejectedCount", rejectedCount);
        data.put("lowStockTabCount", lowStockTabCount);

        return data;
    }

    private boolean isProductLowStockOrOutOfStock(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return false;
        }
        return product.getVariants().stream().anyMatch(v -> {
            int threshold = (v.getLowStockThreshold() != null) ? v.getLowStockThreshold() : 5;
            int stock = (v.getStockQuantity() != null) ? v.getStockQuantity() : 0;
            return stock <= threshold;
        });
    }

    private boolean isProductHealthyStock(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return false;
        }
        return product.getVariants().stream().allMatch(v -> {
            int threshold = (v.getLowStockThreshold() != null) ? v.getLowStockThreshold() : 5;
            int stock = (v.getStockQuantity() != null) ? v.getStockQuantity() : 0;
            return stock > threshold;
        });
    }

    private boolean isProductLowStockOnly(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return false;
        }
        return product.getVariants().stream().anyMatch(v -> {
            int threshold = (v.getLowStockThreshold() != null) ? v.getLowStockThreshold() : 5;
            int stock = (v.getStockQuantity() != null) ? v.getStockQuantity() : 0;
            return stock > 0 && stock <= threshold;
        });
    }

    private boolean isProductOutOfStock(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return false;
        }
        return product.getVariants().stream().anyMatch(v -> {
            int stock = (v.getStockQuantity() != null) ? v.getStockQuantity() : 0;
            return stock <= 0;
        });
    }

    private boolean matchesPrice(Product product, BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return true;
        }
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return false;
        }
        return product.getVariants().stream().anyMatch(v -> {
            BigDecimal price = (v.getSalePrice() != null && v.getSalePrice().compareTo(BigDecimal.ZERO) > 0)
                    ? v.getSalePrice()
                    : v.getOriginalPrice();
            if (price == null) return false;
            boolean matchesMin = (minPrice == null || price.compareTo(minPrice) >= 0);
            boolean matchesMax = (maxPrice == null || price.compareTo(maxPrice) <= 0);
            return matchesMin && matchesMax;
        });
    }

    private BigDecimal getLowestPrice(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return BigDecimal.valueOf(Long.MAX_VALUE);
        }
        return product.getVariants().stream()
                .map(v -> (v.getSalePrice() != null && v.getSalePrice().compareTo(BigDecimal.ZERO) > 0)
                        ? v.getSalePrice()
                        : v.getOriginalPrice())
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.valueOf(Long.MAX_VALUE));
    }

    private int getTotalStock(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return 0;
        }
        return product.getVariants().stream()
                .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                .sum();
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
    public Product updateProductForShop(Integer userId, Integer productId, ProductUpdateRequest request) {
        Shop shop = getShopByUserId(userId);
        Product product = getProductById(productId);
        if (!product.getShop().getShopId().equals(shop.getShopId())) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa sản phẩm này!");
        }

        MasterCategory category = masterCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục hoa quả với mã ID: " + request.getCategoryId()));

        validateProductTextFields(request.getProductName(), request.getDescription());

        // Kiểm tra thực thể trước khi cập nhật entity và upload ảnh lên MinIO
        List<VariantItemRequest> reqVariants = request.getEffectiveVariants();
        if (reqVariants.isEmpty()) {
            throw new IllegalArgumentException("Hoa quả phải có ít nhất 1 thực thể!");
        }

        for (int i = 0; i < reqVariants.size(); i++) {
            validateVariantItem(reqVariants.get(i), i + 1);
        }

        product.setProductName(request.getProductName().trim());
        product.setCategory(category);
        product.setDescription(request.getDescription().trim());

        // Nếu sản phẩm từng bị từ chối phê duyệt, khi chủ shop chỉnh sửa xong thì gửi lại về trạng thái PENDING
        if (product.getApprovalStatus() == ProductApprovalStatus.REJECTED) {
            product.setApprovalStatus(ProductApprovalStatus.PENDING);
        }

        // Lưu ảnh mới lên MinIO nếu người dùng tải tệp ảnh lên
        String newImgUrl = null;
        String uploadedMinioUrl = null;
        MultipartFile imageFile = request.getImageFile();
        if (imageFile != null && !imageFile.isEmpty()) {
            storageService.validateImage(imageFile);
            uploadedMinioUrl = storageService.storeImage(imageFile, "products");
            newImgUrl = uploadedMinioUrl;
        } else if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
            newImgUrl = request.getImageUrl().trim();
        }

        try {
            // Cập nhật ảnh đại diện nếu có
            if (newImgUrl != null) {
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

            List<ProductVariant> existingVariants = productVariantRepository.findByProduct_ProductId(productId);
            Set<Integer> keptVariantIds = new HashSet<>();

            for (VariantItemRequest vReq : reqVariants) {
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
        } catch (Exception ex) {
            if (uploadedMinioUrl != null) {
                storageService.clearUp(List.of(uploadedMinioUrl));
            }
            throw ex;
        }
    }

    private void validateProductTextFields(String productName, String description) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên hoa quả không được để trống.");
        }
        if (description == null || description.trim().length() < 20) {
            throw new IllegalArgumentException("Mô tả chi tiết hoa quả phải có ít nhất 20 ký tự.");
        }
    }

    private void validateVariantItem(VariantItemRequest vReq, int position) {
        String label = "Thực thể #" + position + " ('" + vReq.getVariantName() + "')";
        BigDecimal minPrice = new BigDecimal("1000");
        if (vReq.getOriginalPrice() == null || vReq.getOriginalPrice().compareTo(minPrice) < 0) {
            throw new IllegalArgumentException(label + " phải có giá bán từ 1,000 VNĐ trở lên.");
        }
        if (vReq.getSalePrice() != null) {
            if (vReq.getSalePrice().compareTo(minPrice) < 0 || vReq.getSalePrice().compareTo(vReq.getOriginalPrice()) >= 0) {
                throw new IllegalArgumentException(label
                        + " có giá khuyến mãi phải từ 1,000 VNĐ trở lên và nhỏ hơn giá bán (" + vReq.getOriginalPrice() + " đ)!");
            }
        }
        if (vReq.getStockQuantity() != null && vReq.getStockQuantity() < 0) {
            throw new IllegalArgumentException(label + " có số lượng tồn kho không được là số âm.");
        }
        if (vReq.getLowStockThreshold() != null && vReq.getLowStockThreshold() < 0) {
            throw new IllegalArgumentException(label + " có mức báo sắp hết không được là số âm.");
        }
    }
}
