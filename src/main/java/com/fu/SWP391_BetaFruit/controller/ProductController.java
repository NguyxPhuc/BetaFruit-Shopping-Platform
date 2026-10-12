package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.ProductCreateRequest;
import com.fu.SWP391_BetaFruit.entity.Product;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.service.CategoryService;
import com.fu.SWP391_BetaFruit.service.ProductService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.fu.SWP391_BetaFruit.dto.request.ProductUpdateRequest;
import com.fu.SWP391_BetaFruit.entity.ProductVariant;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    // =========================================================================
    // 1. DANH SÁCH HOA QUẢ CỦA CỬA HÀNG (SHOP OWNER)
    // URL chuẩn 3 cấp: /shop/products
    // =========================================================================
    @GetMapping("/shop/products")
    public String listShopProducts(@RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "8") int size,
            HttpSession session,
            Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            Shop shop = productService.getShopByUserId(userId);
            Page<Product> productPage = productService.getShopProductsPaginated(userId, keyword, page, size);

            model.addAttribute("shop", shop);
            model.addAttribute("products", productPage.getContent());
            model.addAttribute("productPage", productPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", productPage.getTotalPages());
            model.addAttribute("totalItems", productPage.getTotalElements());
            model.addAttribute("keyword", keyword != null ? keyword.trim() : "");

            return "shop/products/list";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "shop/products/list";
        }
    }

    // =========================================================================
    // 2. GIAO DIỆN THÊM HOA QUẢ MỚI (SHOP OWNER)
    // URL chuẩn 3 cấp: /shop/products/create
    // Chỉ hiển thị và gắn cho duy nhất 1 cửa hàng mà Shop Owner đang sở hữu
    // =========================================================================
    @GetMapping("/shop/products/create")
    public String showCreateProductForm(HttpSession session, Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            Shop shop = productService.getShopByUserId(userId);

            if (!model.containsAttribute("product")) {
                ProductCreateRequest defaultRequest = new ProductCreateRequest();
                com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest firstVariant = new com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest();
                firstVariant.setLowStockThreshold(5);
                defaultRequest.getVariants().add(firstVariant);
                model.addAttribute("product", defaultRequest);
            }

            model.addAttribute("shop", shop);
            model.addAttribute("categories", categoryService.getActiveCategories());

            return "shop/products/create";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "redirect:/shop/profile";
        }
    }

    // =========================================================================
    // 3. XỬ LÝ LƯU HOA QUẢ MỚI (SHOP OWNER)
    // URL chuẩn 3 cấp: /shop/products/create
    // =========================================================================
    @PostMapping("/shop/products/create")
    public String processCreateProduct(@Valid @ModelAttribute("product") ProductCreateRequest request,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        Shop shop;
        try {
            shop = productService.getShopByUserId(userId);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/shop/profile";
        }

        // Kiểm tra hình ảnh (chỉ chấp nhận tải tệp ảnh từ máy tính)
        boolean hasFile = request.getImageFile() != null && !request.getImageFile().isEmpty();
        if (!hasFile) {
            bindingResult.rejectValue("imageUrl", "error.product", "Vui lòng tải lên hình ảnh đại diện của hoa quả từ máy tính.");
        }

        // Kiểm tra hợp lệ danh sách thực thể (biến thể)
        java.util.List<com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest> effectiveVariants = request.getEffectiveVariants();
        if (effectiveVariants.isEmpty()) {
            model.addAttribute("errorMessage", "Vui lòng nhập ít nhất 1 thực thể cho hoa quả.");
        } else {
            for (int i = 0; i < effectiveVariants.size(); i++) {
                com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest v = effectiveVariants.get(i);
                if (v.getOriginalPrice() == null || v.getOriginalPrice().compareTo(new java.math.BigDecimal("1000")) < 0) {
                    model.addAttribute("errorMessage", "Thực thể #" + (i + 1) + " ('" + v.getVariantName() + "') phải có giá niêm yết từ 1,000 VNĐ trở lên.");
                    break;
                }
                if (v.getSalePrice() != null) {
                    if (v.getSalePrice().compareTo(java.math.BigDecimal.ZERO) <= 0 || v.getSalePrice().compareTo(v.getOriginalPrice()) >= 0) {
                        model.addAttribute("errorMessage", "Thực thể #" + (i + 1) + " ('" + v.getVariantName() + "') có giá khuyến mãi phải lớn hơn 0 và nhỏ hơn giá niêm yết.");
                        break;
                    }
                }
                if (v.getStockQuantity() == null || v.getStockQuantity() < 0) {
                    model.addAttribute("errorMessage", "Thực thể #" + (i + 1) + " ('" + v.getVariantName() + "') có số lượng tồn kho không được âm.");
                    break;
                }
            }
        }

        // Kiểm tra hợp lệ dữ liệu cơ bản
        if (bindingResult.hasErrors() || model.containsAttribute("errorMessage")) {
            if (request.getVariants() == null || request.getVariants().isEmpty()) {
                request.setVariants(effectiveVariants);
            }
            if (request.getVariants().isEmpty()) {
                request.getVariants().add(new com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest());
            }
            model.addAttribute("shop", shop);
            model.addAttribute("categories", categoryService.getActiveCategories());
            return "shop/products/create";
        }

        // Nếu người dùng tải ảnh từ máy tính, lưu vào thư mục uploads/products/
        if (hasFile) {
            try {
                org.springframework.web.multipart.MultipartFile file = request.getImageFile();
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String filename = java.util.UUID.randomUUID() + extension;
                java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads", "products");
                if (!java.nio.file.Files.exists(uploadDir)) {
                    java.nio.file.Files.createDirectories(uploadDir);
                }
                java.nio.file.Path targetLocation = uploadDir.resolve(filename);
                java.nio.file.Files.copy(file.getInputStream(), targetLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                request.setImageUrl("/uploads/products/" + filename);
            } catch (java.io.IOException e) {
                model.addAttribute("errorMessage", "Lỗi khi lưu tệp hình ảnh tải lên: " + e.getMessage());
                model.addAttribute("shop", shop);
                model.addAttribute("categories", categoryService.getActiveCategories());
                return "shop/products/create";
            }
        }

        try {
            Product savedProduct = productService.createProductForShop(userId, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đăng ký hoa quả '" + savedProduct.getProductName()
                            + "' thành công! Sản phẩm đang trong trạng thái chờ ban quản trị phê duyệt chất lượng.");
            return "redirect:/shop/products";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("shop", shop);
            model.addAttribute("categories", categoryService.getActiveCategories());
            return "shop/products/create";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Đã xảy ra sự cố trong quá trình lưu hoa quả: " + ex.getMessage());
            model.addAttribute("shop", shop);
            model.addAttribute("categories", categoryService.getActiveCategories());
            return "shop/products/create";
        }
    }

    // =========================================================================
    // 4. CHUYỂN ĐỔI TRẠNG THÁI ẨN / HIỆN HOA QUẢ (SHOP OWNER)
    // URL: /shop/products/{id}/toggle-visibility
    // =========================================================================
    @PostMapping("/shop/products/{id}/toggle-visibility")
    public String toggleProductVisibility(@PathVariable("id") Integer id,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            productService.toggleProductVisibilityForShop(userId, id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái hiển thị hoa quả thành công!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái hiển thị: " + ex.getMessage());
        }
        return "redirect:/shop/products";
    }

    // =========================================================================
    // 5. GIAO DIỆN CHỈNH SỬA HOA QUẢ (SHOP OWNER)
    // URL: /shop/products/{id}/edit
    // =========================================================================
    @GetMapping("/shop/products/{id}/edit")
    public String showEditProductForm(@PathVariable("id") Integer id,
                                      HttpSession session,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            Shop shop = productService.getShopByUserId(userId);
            Product product = productService.getShopProductForEdit(userId, id);

            if (!model.containsAttribute("product")) {
                ProductUpdateRequest request = new ProductUpdateRequest();
                request.setProductId(product.getProductId());
                request.setProductName(product.getProductName());
                request.setCategoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null);
                request.setDescription(product.getDescription());
                if (product.getImages() != null && !product.getImages().isEmpty()) {
                    request.setImageUrl(product.getImages().get(0).getImageUrl());
                }

                if (product.getVariants() != null) {
                    for (ProductVariant v : product.getVariants()) {
                        com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest vReq = new com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest();
                        vReq.setVariantId(v.getVariantId());
                        vReq.setVariantName(v.getVariantName());
                        vReq.setOriginalPrice(v.getOriginalPrice());
                        vReq.setSalePrice(v.getSalePrice());
                        vReq.setCostPrice(v.getCostPrice());
                        vReq.setStockQuantity(v.getStockQuantity());
                        vReq.setLowStockThreshold(v.getLowStockThreshold());
                        vReq.setIsAvailable(v.getIsAvailable());
                        request.getVariants().add(vReq);
                    }
                }
                if (request.getVariants().isEmpty()) {
                    request.getVariants().add(new com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest());
                }
                model.addAttribute("product", request);
            }

            model.addAttribute("shop", shop);
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("originalProduct", product);

            return "shop/products/edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/shop/products";
        }
    }

    // =========================================================================
    // 6. XỬ LÝ LƯU CẬP NHẬT HOA QUẢ (SHOP OWNER)
    // URL: /shop/products/{id}/edit
    // =========================================================================
    @PostMapping("/shop/products/{id}/edit")
    public String processUpdateProduct(@PathVariable("id") Integer id,
                                       @Valid @ModelAttribute("product") ProductUpdateRequest request,
                                       BindingResult bindingResult,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        Shop shop;
        Product originalProduct;
        try {
            shop = productService.getShopByUserId(userId);
            originalProduct = productService.getShopProductForEdit(userId, id);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/shop/products";
        }

        // Kiểm tra hợp lệ danh sách thực thể (biến thể)
        java.util.List<com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest> effectiveVariants = request.getEffectiveVariants();
        if (effectiveVariants.isEmpty()) {
            model.addAttribute("errorMessage", "Vui lòng nhập ít nhất 1 thực thể cho hoa quả.");
        } else {
            for (int i = 0; i < effectiveVariants.size(); i++) {
                com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest v = effectiveVariants.get(i);
                if (v.getOriginalPrice() == null || v.getOriginalPrice().compareTo(new java.math.BigDecimal("1000")) < 0) {
                    model.addAttribute("errorMessage", "Thực thể #" + (i + 1) + " ('" + v.getVariantName() + "') phải có giá niêm yết từ 1,000 VNĐ trở lên.");
                    break;
                }
                if (v.getSalePrice() != null) {
                    if (v.getSalePrice().compareTo(java.math.BigDecimal.ZERO) <= 0 || v.getSalePrice().compareTo(v.getOriginalPrice()) >= 0) {
                        model.addAttribute("errorMessage", "Thực thể #" + (i + 1) + " ('" + v.getVariantName() + "') có giá khuyến mãi phải lớn hơn 0 và nhỏ hơn giá niêm yết.");
                        break;
                    }
                }
                if (v.getStockQuantity() == null || v.getStockQuantity() < 0) {
                    model.addAttribute("errorMessage", "Thực thể #" + (i + 1) + " ('" + v.getVariantName() + "') có số lượng tồn kho không được âm.");
                    break;
                }
            }
        }

        // Kiểm tra lỗi validation
        if (bindingResult.hasErrors() || model.containsAttribute("errorMessage")) {
            if (request.getVariants() == null || request.getVariants().isEmpty()) {
                request.setVariants(effectiveVariants);
            }
            if (request.getVariants().isEmpty()) {
                request.getVariants().add(new com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest());
            }
            model.addAttribute("shop", shop);
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("originalProduct", originalProduct);
            return "shop/products/edit";
        }

        // Nếu người dùng chọn ảnh mới tải lên từ máy tính
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            try {
                org.springframework.web.multipart.MultipartFile file = request.getImageFile();
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String filename = java.util.UUID.randomUUID() + extension;
                java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads", "products");
                if (!java.nio.file.Files.exists(uploadDir)) {
                    java.nio.file.Files.createDirectories(uploadDir);
                }
                java.nio.file.Path targetLocation = uploadDir.resolve(filename);
                java.nio.file.Files.copy(file.getInputStream(), targetLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                request.setImageUrl("/uploads/products/" + filename);
            } catch (java.io.IOException e) {
                model.addAttribute("errorMessage", "Lỗi khi lưu tệp hình ảnh tải lên: " + e.getMessage());
                model.addAttribute("shop", shop);
                model.addAttribute("categories", categoryService.getActiveCategories());
                model.addAttribute("originalProduct", originalProduct);
                return "shop/products/edit";
            }
        }

        try {
            Product updatedProduct = productService.updateProductForShop(userId, id, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật hoa quả '" + updatedProduct.getProductName() + "' thành công!");
            return "redirect:/shop/products";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("shop", shop);
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("originalProduct", originalProduct);
            return "shop/products/edit";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Đã xảy ra sự cố trong quá trình cập nhật hoa quả: " + ex.getMessage());
            model.addAttribute("shop", shop);
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("originalProduct", originalProduct);
            return "shop/products/edit";
        }
    }
}
