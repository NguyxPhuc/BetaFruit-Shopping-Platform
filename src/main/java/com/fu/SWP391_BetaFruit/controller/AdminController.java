package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.CategoryRequest;
import com.fu.SWP391_BetaFruit.dto.request.RoleRequest;
import com.fu.SWP391_BetaFruit.service.CategoryService;
import com.fu.SWP391_BetaFruit.service.RoleService;
import com.fu.SWP391_BetaFruit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final CategoryService categoryService;
    private final RoleService roleService;

    // ================= 0. TỔNG QUAN (DASHBOARD) =================
    /**
     * Displays the Admin Dashboard overview page.
     */
    @GetMapping({"/admin", "/admin/", "/admin/dashboard"})
    public String dashboard(Model model) {
        return "admin/admin-dashboard";
    }

    // ================= 1. QUẢN LÝ NGƯỜI DÙNG & PHÂN QUYỀN (USERS) =================
    /**
     * Displays the list of users with optional keyword searching, role/status filtering, pagination, and sorting.
     */
    @GetMapping("/admin/users")
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
    @PostMapping("/admin/users/{id}/toggle-status")
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
    @PostMapping("/admin/users/assign-roles")
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

    // ================= 2. QUẢN LÝ DANH MỤC GỐC (MASTER CATEGORIES) =================
    /**
     * Displays the list of fruit categories with optional keyword searching, pagination, and sorting.
     */
    @GetMapping("/admin/categories")
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
    @PostMapping("/admin/categories/create")
    public String createCategory(@ModelAttribute("newCategory") CategoryRequest request, RedirectAttributes redirectAttributes) {
        categoryService.createCategory(request);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo danh mục hoa quả mới thành công!");
        return "redirect:/admin/categories";
    }

    /**
     * Updates an existing fruit category by ID.
     */
    @PostMapping("/admin/categories/edit/{id}")
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
    @PostMapping("/admin/categories/{id}/toggle-status")
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

    // ================= 3. QUẢN LÝ VAI TRÒ (ROLES) =================
    /**
     * Displays the list of roles and permissions in the system.
     */
    @GetMapping("/admin/roles")
    public String listRoles(Model model) {
        model.addAllAttributes(roleService.getAdminRolePageData());
        return "admin/roles/list";
    }

    /**
     * Handles the creation of a new role.
     */
    @PostMapping("/admin/roles/create")
    public String createRole(@ModelAttribute("newRole") RoleRequest roleRequest, RedirectAttributes redirectAttributes) {
        roleService.createRole(roleRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo vai trò mới thành công!");
        return "redirect:/admin/roles";
    }

    /**
     * Updates an existing role and its permissions by ID.
     */
    @PostMapping("/admin/roles/edit/{id}")
    public String editRole(@PathVariable("id") Long id, @ModelAttribute RoleRequest roleRequest, RedirectAttributes redirectAttributes) {
        roleService.updateRole(id, roleRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật vai trò thành công!");
        return "redirect:/admin/roles";
    }
}
