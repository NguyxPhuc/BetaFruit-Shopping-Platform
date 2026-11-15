package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.ProductCreateRequest;
import com.fu.SWP391_BetaFruit.dto.request.ProductUpdateRequest;
import com.fu.SWP391_BetaFruit.dto.request.ShopProductFilterRequest;
import com.fu.SWP391_BetaFruit.dto.request.VariantItemRequest;
import com.fu.SWP391_BetaFruit.entity.Product;
import com.fu.SWP391_BetaFruit.entity.ProductVariant;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.exception.InvalidUploadImageException;
import com.fu.SWP391_BetaFruit.service.CategoryService;
import com.fu.SWP391_BetaFruit.service.InventoryService;
import com.fu.SWP391_BetaFruit.service.ProductService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final InventoryService inventoryService;

    // 1. DANH SÁCH HOA QUẢ CỦA CỬA HÀNG (SHOP OWNER)
    // URL chuẩn 3 cấp: /shop/products (Hỗ trợ đa bộ lọc: Trạng thái, Danh mục, Kho hàng, Mức giá, Sắp xếp)
    @GetMapping("/shop/products")
    public String listShopProducts(@ModelAttribute ShopProductFilterRequest filterRequest,
            HttpSession session,
            Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            Shop shop = productService.getShopByUserId(userId);
            Map<String, Object> pageData = productService.getShopProductPageData(userId, filterRequest);
            long lowStockCount = inventoryService.countLowStockVariants(userId);

            model.addAttribute("shop", shop);
            model.addAllAttributes(pageData);
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("lowStockCount", lowStockCount);
            model.addAttribute("lowStockVariants", inventoryService.getLowStockVariants(userId));

            return "shop/products/list";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "shop/products/list";
        }
    }

    // 2. GIAO DIỆN THÊM HOA QUẢ MỚI (SHOP OWNER)
    // URL chuẩn 3 cấp: /shop/products/create
    // Chỉ hiển thị và gắn cho duy nhất 1 cửa hàng mà Shop Owner đang sở hữu
    @GetMapping("/shop/products/create")
    public String showCreateProductForm(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            Shop shop = productService.getShopByUserId(userId);

            if (!model.containsAttribute("product")) {
                ProductCreateRequest defaultRequest = new ProductCreateRequest();
                VariantItemRequest firstVariant = new VariantItemRequest();
                firstVariant.setLowStockThreshold(5);
                firstVariant.setStockQuantity(0);
                defaultRequest.getVariants().add(firstVariant);
                model.addAttribute("product", defaultRequest);
            }

            model.addAttribute("shop", shop);
            model.addAttribute("categories", categoryService.getActiveCategories());

            return "shop/products/create";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/shop/profile";
        }
    }

    // 3. XỬ LÝ LƯU HOA QUẢ MỚI (SHOP OWNER)

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
        if (request.getImageFile() == null || request.getImageFile().isEmpty()) {
            bindingResult.rejectValue("imageUrl", "error.product",
                    "Vui lòng tải lên hình ảnh đại diện của hoa quả từ máy tính.");
        }

        if (bindingResult.hasErrors()) {
            populateErrorFromBindingResult(bindingResult, model);
            return prepareCreateFormView(shop, request, model);
        }

        // Nghiệp vụ kiểm tra chi tiết, upload MinIO và lưu dữ liệu nằm tại ProductServiceImpl
        try {
            Product savedProduct = productService.createProductForShop(userId, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đăng ký hoa quả '" + savedProduct.getProductName()
                            + "' thành công! Sản phẩm đang trong trạng thái chờ ban quản trị phê duyệt chất lượng.");
            return "redirect:/shop/products";
        } catch (IllegalArgumentException | InvalidUploadImageException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return prepareCreateFormView(shop, request, model);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Đã xảy ra sự cố trong quá trình lưu hoa quả: " + ex.getMessage());
            return prepareCreateFormView(shop, request, model);
        }
    }

    // 4. CHUYỂN ĐỔI TRẠNG THÁI ẨN / HIỆN HOA QUẢ (SHOP OWNER)
    // URL: /shop/products/{id}/toggle-visibility

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
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Lỗi khi cập nhật trạng thái hiển thị: " + ex.getMessage());
        }
        return "redirect:/shop/products";
    }

    // 5. GIAO DIỆN CHỈNH SỬA HOA QUẢ (SHOP OWNER)
    // URL: /shop/products/{id}/edit
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
                        VariantItemRequest vReq = new VariantItemRequest();
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
                    request.getVariants().add(new VariantItemRequest());
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

    // 6. XỬ LÝ LƯU CẬP NHẬT HOA QUẢ (SHOP OWNER)
    // URL: /shop/products/{id}/edit

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

        if (bindingResult.hasErrors()) {
            populateErrorFromBindingResult(bindingResult, model);
            return prepareEditFormView(shop, originalProduct, request, model);
        }

        // Nghiệp vụ kiểm tra chi tiết, upload MinIO và cập nhật dữ liệu nằm tại ProductServiceImpl
        try {
            Product updatedProduct = productService.updateProductForShop(userId, id, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật hoa quả '" + updatedProduct.getProductName() + "' thành công!");
            return "redirect:/shop/products";
        } catch (IllegalArgumentException | InvalidUploadImageException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return prepareEditFormView(shop, originalProduct, request, model);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Đã xảy ra sự cố trong quá trình cập nhật hoa quả: " + ex.getMessage());
            return prepareEditFormView(shop, originalProduct, request, model);
        }
    }

    private void populateErrorFromBindingResult(BindingResult bindingResult, Model model) {
        if (!bindingResult.hasErrors() || model.containsAttribute("errorMessage")) {
            return;
        }
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            if (fieldError.getField() != null && fieldError.getField().startsWith("variants")) {
                String msg = fieldError.isBindingFailure()
                        ? "Dữ liệu số ở phần thực thể không hợp lệ, vui lòng kiểm tra lại."
                        : fieldError.getDefaultMessage();
                model.addAttribute("errorMessage", msg);
                return;
            }
        }
        FieldError firstError = bindingResult.getFieldError();
        if (firstError != null) {
            model.addAttribute("errorMessage", firstError.isBindingFailure()
                    ? "Dữ liệu nhập vào chưa hợp lệ, vui lòng kiểm tra lại."
                    : firstError.getDefaultMessage());
        }
    }

    private String prepareCreateFormView(Shop shop, ProductCreateRequest request, Model model) {
        request.setVariants(normalizeVariantsForView(request.getVariants(), request.getEffectiveVariants()));
        return renderProductFormView("shop/products/create", shop, null, model);
    }

    private String prepareEditFormView(Shop shop, Product originalProduct, ProductUpdateRequest request, Model model) {
        request.setVariants(normalizeVariantsForView(request.getVariants(), request.getEffectiveVariants()));
        return renderProductFormView("shop/products/edit", shop, originalProduct, model);
    }

    private List<VariantItemRequest> normalizeVariantsForView(List<VariantItemRequest> current,
            List<VariantItemRequest> fallback) {
        List<VariantItemRequest> resolved = (current == null || current.isEmpty()) ? fallback : current;
        if (resolved == null || resolved.isEmpty()) {
            resolved = new java.util.ArrayList<>();
            resolved.add(new VariantItemRequest());
        }
        return resolved;
    }

    private String renderProductFormView(String viewName, Shop shop, Product originalProduct, Model model) {
        model.addAttribute("shop", shop);
        model.addAttribute("categories", categoryService.getActiveCategories());
        if (originalProduct != null) {
            model.addAttribute("originalProduct", originalProduct);
        }
        return viewName;
    }
}
