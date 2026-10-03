package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.CategoryRequest;
import com.fu.SWP391_BetaFruit.dto.request.RoleRequest;
import com.fu.SWP391_BetaFruit.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ShopService shopService;
    private final ProductService productService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final RoleService roleService;

    // ================= 0. TỔNG QUAN (DASHBOARD) =================
    /**
     * Displays the Admin Dashboard overview page.
     */
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        return "admin/admin-dashboard";
    }

    // ================= 1. QUẢN LÝ CỬA HÀNG (SHOPS) =================
    /**
     * Displays the list of shops with optional filtering by status, keyword, pagination, and sorting.
     */
    @GetMapping("/shops")
    public String listShops(@RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "5") int size,
                            @RequestParam(value = "sortBy", defaultValue = "shopId") String sortBy,
                            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
                            Model model) {
        model.addAllAttributes(shopService.getAdminShopPageData(keyword, status, page, size, sortBy, sortDir));
        return "admin/shops/list";
    }

    /**
     * Approves a shop registration and grants selling permissions to the owner.
     */
    @PostMapping("/shops/{id}/approve")
    public String approveShop(@PathVariable("id") Integer id,
                              @RequestParam(value = "page", defaultValue = "1") int page,
                              @RequestParam(value = "status", required = false) String status,
                              RedirectAttributes redirectAttributes) {
        shopService.approveShop(id);
        redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt cửa hàng thành công! Chủ shop đã được cấp quyền bán hàng.");
        String redirectUrl = "redirect:/admin/shops?page=" + page;
        if (status != null && !status.isEmpty()) {
            redirectUrl += "&status=" + status;
        }
        return redirectUrl;
    }

    /**
     * Rejects a shop registration request.
     */
    @PostMapping("/shops/{id}/reject")
    public String rejectShop(@PathVariable("id") Integer id,
                             @RequestParam(value = "page", defaultValue = "1") int page,
                             @RequestParam(value = "status", required = false) String status,
                             RedirectAttributes redirectAttributes) {
        shopService.rejectShop(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đơn đăng ký mở cửa hàng này!");
        String redirectUrl = "redirect:/admin/shops?page=" + page;
        if (status != null && !status.isEmpty()) {
            redirectUrl += "&status=" + status;
        }
        return redirectUrl;
    }

    /**
     * Configures the platform commission rate for a shop.
     */
    @PostMapping("/shops/{id}/commission")
    public String configureCommissionRate(@PathVariable("id") Integer id,
                                          @RequestParam("commissionRate") BigDecimal commissionRate,
                                          RedirectAttributes redirectAttributes) {
        shopService.updateCommissionRate(id, commissionRate);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tỷ lệ phí sàn chiết khấu cho cửa hàng thành công!");
        return "redirect:/admin/shops";
    }

    // ================= 2. QUẢN LÝ SẢN PHẨM (PRODUCTS) =================
    /**
     * Displays the list of products filtered by tab status, search keyword, pagination, and sorting.
     */
    @GetMapping("/products")
    public String listProducts(@RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "size", defaultValue = "5") int size,
                               @RequestParam(value = "sortBy", defaultValue = "productId") String sortBy,
                               @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
                               Model model) {
        model.addAllAttributes(productService.getAdminProductPageData(keyword, tab, page, size, sortBy, sortDir));
        return "admin/products/list";
    }

    /**
     * Approves a fruit product and activates it for sale on the platform.
     */
    @PostMapping("/products/{id}/approve")
    public String approveProduct(@PathVariable("id") Integer id,
                                 @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                                 @RequestParam(value = "page", defaultValue = "1") int page,
                                 RedirectAttributes redirectAttributes) {
        productService.approveProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt hoa quả thành công! Sản phẩm đã được kích hoạt mở bán trên sàn.");
        return "redirect:/admin/products?tab=" + tab + "&page=" + page;
    }

    /**
     * Rejects a fruit product moderation request.
     */
    @PostMapping("/products/{id}/reject")
    public String rejectProduct(@PathVariable("id") Integer id,
                                @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                                @RequestParam(value = "page", defaultValue = "1") int page,
                                RedirectAttributes redirectAttributes) {
        productService.rejectProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối kiểm duyệt sản phẩm hoa quả này!");
        return "redirect:/admin/products?tab=" + tab + "&page=" + page;
    }

    /**
     * Toggles the display visibility status of a fruit product.
     */
    @PostMapping("/products/{id}/toggle-visibility")
    public String toggleVisibility(@PathVariable("id") Integer id,
                                   @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                                   @RequestParam(value = "page", defaultValue = "1") int page,
                                   RedirectAttributes redirectAttributes) {
        productService.toggleProductVisibility(id);
        redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái hiển thị hoa quả thành công!");
        return "redirect:/admin/products?tab=" + tab + "&page=" + page;
    }

    // ================= 3. QUẢN LÝ NGƯỜI DÙNG (USERS) =================
    /**
     * Displays the list of users with optional keyword searching, role/status filtering, pagination, and sorting.
     */
    @GetMapping("/users")
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "roleId", required = false) Long roleId,
                            @RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "5") int size,
                            @RequestParam(value = "sortBy", defaultValue = "userId") String sortBy,
                            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
                            Model model) {
        model.addAllAttributes(userService.getAdminUserPageData(keyword, roleId, status, page, size, sortBy, sortDir));
        return "admin/users/list";
    }

    /**
     * Toggles the active/blocked status of a user account.
     */
    @PostMapping("/users/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Integer id,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "roleId", required = false) Long roleId,
                               @RequestParam(value = "status", required = false) String status,
                               RedirectAttributes redirectAttributes) {
        userService.toggleUserStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái tài khoản thành công!");
        StringBuilder redirectUrl = new StringBuilder("redirect:/admin/users?page=").append(page);
        if (keyword != null && !keyword.trim().isEmpty()) {
            redirectUrl.append("&keyword=").append(keyword.trim());
        }
        if (roleId != null && roleId > 0) {
            redirectUrl.append("&roleId=").append(roleId);
        }
        if (status != null && !status.trim().isEmpty()) {
            redirectUrl.append("&status=").append(status.trim());
        }
        return redirectUrl.toString();
    }

    /**
     * Assigns specified roles to a user account.
     */
    @PostMapping("/users/assign-roles")
    public String assignRoles(@RequestParam("userId") Integer userId,
                              @RequestParam(value = "roleIds", required = false) List<Long> roleIds,
                              @RequestParam(value = "page", defaultValue = "1") int page,
                              @RequestParam(value = "keyword", required = false) String keyword,
                              @RequestParam(value = "roleId", required = false) Long roleId,
                              @RequestParam(value = "status", required = false) String status,
                              RedirectAttributes redirectAttributes) {
        userService.assignRolesToUser(userId, roleIds);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phân quyền vai trò thành công!");
        StringBuilder redirectUrl = new StringBuilder("redirect:/admin/users?page=").append(page);
        if (keyword != null && !keyword.trim().isEmpty()) {
            redirectUrl.append("&keyword=").append(keyword.trim());
        }
        if (roleId != null && roleId > 0) {
            redirectUrl.append("&roleId=").append(roleId);
        }
        if (status != null && !status.trim().isEmpty()) {
            redirectUrl.append("&status=").append(status.trim());
        }
        return redirectUrl.toString();
    }

    // ================= 4. QUẢN LÝ DANH MỤC (CATEGORIES) =================
    /**
     * Displays the list of fruit categories with optional keyword searching, pagination, and sorting.
     */
    @GetMapping("/categories")
    public String listCategories(@RequestParam(value = "keyword", required = false) String keyword,
                                 @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
                                 @RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "5") int size,
                                 @RequestParam(value = "sortBy", defaultValue = "categoryId") String sortBy,
                                 @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir,
                                 Model model) {
        model.addAllAttributes(categoryService.getAdminCategoryPageData(keyword, status, page, size, sortBy, sortDir));
        return "admin/categories/list";
    }

    /**
     * Handles the creation of a new fruit category.
     */
    @PostMapping("/categories/create")
    public String createCategory(@ModelAttribute("newCategory") CategoryRequest request, RedirectAttributes redirectAttributes) {
        categoryService.createCategory(request);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo danh mục hoa quả mới thành công!");
        return "redirect:/admin/categories";
    }

    /**
     * Updates an existing fruit category by ID.
     */
    @PostMapping("/categories/edit/{id}")
    public String editCategory(@PathVariable("id") Integer id,
                               @ModelAttribute CategoryRequest request,
                               RedirectAttributes redirectAttributes) {
        categoryService.updateCategory(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục hoa quả thành công!");
        return "redirect:/admin/categories";
    }

    /**
     * Toggles the active or display status of a category.
     */
    @PostMapping("/categories/{id}/toggle-status")
    public String toggleCategoryStatus(@PathVariable("id") Integer id,
                                       @RequestParam(value = "page", defaultValue = "1") int page,
                                       @RequestParam(value = "keyword", required = false) String keyword,
                                       @RequestParam(value = "status", required = false) String status,
                                       RedirectAttributes redirectAttributes) {
        categoryService.toggleCategoryStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái hiển thị danh mục thành công!");
        StringBuilder redirectUrl = new StringBuilder("redirect:/admin/categories?page=").append(page);
        if (keyword != null && !keyword.trim().isEmpty()) {
            redirectUrl.append("&keyword=").append(keyword.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            redirectUrl.append("&status=").append(status.trim());
        }
        return redirectUrl.toString();
    }

    // ================= 5. QUẢN LÝ VAI TRÒ (ROLES) =================
    /**
     * Displays the list of roles and permissions in the system.
     */
    @GetMapping("/roles")
    public String listRoles(Model model) {
        model.addAllAttributes(roleService.getAdminRolePageData());
        return "admin/roles/list";
    }

    /**
     * Handles the creation of a new role.
     */
    @PostMapping("/roles/create")
    public String createRole(@ModelAttribute("newRole") RoleRequest roleRequest, RedirectAttributes redirectAttributes) {
        roleService.createRole(roleRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo vai trò mới thành công!");
        return "redirect:/admin/roles";
    }

    /**
     * Updates an existing role and its permissions by ID.
     */
    @PostMapping("/roles/edit/{id}")
    public String editRole(@PathVariable("id") Long id, @ModelAttribute RoleRequest roleRequest, RedirectAttributes redirectAttributes) {
        roleService.updateRole(id, roleRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật vai trò thành công!");
        return "redirect:/admin/roles";
    }
}
