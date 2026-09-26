package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.admin.CategoryRequest;
import com.fu.SWP391_BetaFruit.dto.request.admin.RoleRequest;
import com.fu.SWP391_BetaFruit.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // ================= 1. QUẢN LÝ CỬA HÀNG (SHOPS) =================
    /**
     * Displays the list of shops with optional filtering by status and keyword.
     */
    @GetMapping("/shops")
    public String listShops(@RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            Model model) {
        model.addAllAttributes(shopService.getAdminShopPageData(keyword, status));
        return "admin/shops/list";
    }

    /**
     * Approves a shop registration and grants selling permissions to the owner.
     */
    @PostMapping("/shops/{id}/approve")
    public String approveShop(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        shopService.approveShop(id);
        redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt cửa hàng thành công! Chủ shop đã được cấp quyền bán hàng.");
        return "redirect:/admin/shops";
    }

    /**
     * Rejects a shop registration request.
     */
    @PostMapping("/shops/{id}/reject")
    public String rejectShop(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        shopService.rejectShop(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đơn đăng ký mở cửa hàng này!");
        return "redirect:/admin/shops";
    }

    // ================= 2. QUẢN LÝ SẢN PHẨM (PRODUCTS) =================
    /**
     * Displays the list of products filtered by tab status and search keyword.
     */
    @GetMapping("/products")
    public String listProducts(@RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               Model model) {
        model.addAllAttributes(productService.getAdminProductPageData(keyword, tab));
        return "admin/products/list";
    }

    /**
     * Approves a fruit product and activates it for sale on the platform.
     */
    @PostMapping("/products/{id}/approve")
    public String approveProduct(@PathVariable("id") Integer id,
                                 @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                                 RedirectAttributes redirectAttributes) {
        productService.approveProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt hoa quả thành công! Sản phẩm đã được kích hoạt mở bán trên sàn.");
        return "redirect:/admin/products?tab=" + tab;
    }

    /**
     * Rejects a fruit product moderation request.
     */
    @PostMapping("/products/{id}/reject")
    public String rejectProduct(@PathVariable("id") Integer id,
                                @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                                RedirectAttributes redirectAttributes) {
        productService.rejectProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối kiểm duyệt sản phẩm hoa quả này!");
        return "redirect:/admin/products?tab=" + tab;
    }

    /**
     * Toggles the display visibility status of a fruit product.
     */
    @PostMapping("/products/{id}/toggle-visibility")
    public String toggleVisibility(@PathVariable("id") Integer id,
                                   @RequestParam(value = "tab", required = false, defaultValue = "ALL") String tab,
                                   RedirectAttributes redirectAttributes) {
        productService.toggleProductVisibility(id);
        redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái hiển thị hoa quả thành công!");
        return "redirect:/admin/products?tab=" + tab;
    }

    // ================= 3. QUẢN LÝ NGƯỜI DÙNG (USERS) =================
    /**
     * Displays the list of users with optional keyword searching.
     */
    @GetMapping("/users")
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAllAttributes(userService.getAdminUserPageData(keyword));
        return "admin/users/list";
    }

    /**
     * Toggles the active/blocked status of a user account.
     */
    @PostMapping("/users/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        userService.toggleUserStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái tài khoản thành công!");
        return "redirect:/admin/users";
    }

    /**
     * Assigns specified roles to a user account.
     */
    @PostMapping("/users/assign-roles")
    public String assignRoles(@RequestParam("userId") Integer userId,
                              @RequestParam(value = "roleIds", required = false) List<Long> roleIds,
                              RedirectAttributes redirectAttributes) {
        userService.assignRolesToUser(userId, roleIds);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phân quyền vai trò thành công!");
        return "redirect:/admin/users";
    }

    // ================= 4. QUẢN LÝ DANH MỤC (CATEGORIES) =================
    /**
     * Displays the list of fruit categories with optional keyword searching.
     */
    @GetMapping("/categories")
    public String listCategories(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAllAttributes(categoryService.getAdminCategoryPageData(keyword));
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
    public String toggleCategoryStatus(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        categoryService.toggleCategoryStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái hiển thị danh mục thành công!");
        return "redirect:/admin/categories";
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
