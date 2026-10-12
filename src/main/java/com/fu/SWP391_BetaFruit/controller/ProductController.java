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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
}
