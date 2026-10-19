package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.request.CategoryRequest;
import com.fu.SWP391_BetaFruit.dto.request.RoleRequest;
import com.fu.SWP391_BetaFruit.dto.request.MembershipTierRequest;
import com.fu.SWP391_BetaFruit.dto.request.UpdateCommissionRequest;
import com.fu.SWP391_BetaFruit.dto.request.UserRequest;
import com.fu.SWP391_BetaFruit.entity.Role;
import com.fu.SWP391_BetaFruit.enums.RoleName;
import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;
import com.fu.SWP391_BetaFruit.service.MembershipTierService;
import com.fu.SWP391_BetaFruit.service.CategoryService;
import com.fu.SWP391_BetaFruit.service.RoleService;
import com.fu.SWP391_BetaFruit.service.ShopService;
import com.fu.SWP391_BetaFruit.service.UserService;
import com.fu.SWP391_BetaFruit.utils.RedirectUtils;
import com.fu.SWP391_BetaFruit.utils.StringUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final CategoryService categoryService;
    private final RoleService roleService;
    private final ShopService shopService;
    private final MembershipTierService membershipTierService;

    // ================= 0. TỔNG QUAN (DASHBOARD) =================
    /**
     * Displays the Admin Dashboard overview page.
     */
    @GetMapping({"/admin", "/admin/", "/admin/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("categoryCount", categoryService.countCategories());
        model.addAttribute("approvedShopCount", shopService.countByStatus(ShopApprovalStatus.APPROVED));
        model.addAttribute("membershipTierCount", membershipTierService.getAllTiers().size());
        model.addAttribute("userCount", userService.countUsers());
        return "admin/admin-dashboard";
    }

    // ================= 1. CẤU HÌNH PHÍ SÀN CỬA HÀNG (FE-12.1) =================
    /**
     * Displays the shop commission configuration page (Admin only).
     * Only displays APPROVED shops and supports filtering by owner account status and commission tier.
     */
    @GetMapping("/admin/shops/commission")
    public String shopCommissions(@RequestParam(value = "keyword", required = false) String keyword,
                                  @RequestParam(value = "ownerStatus", defaultValue = "ALL") String ownerStatus,
                                  @RequestParam(value = "commissionTier", defaultValue = "ALL") String commissionTier,
                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                  @RequestParam(value = "size", defaultValue = "5") int size,
                                  @RequestParam(value = "sortBy", defaultValue = "shopId") String sortBy,
                                  @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir,
                                  Model model) {
        model.addAllAttributes(shopService.getAdminCommissionPageData(StringUtils.sanitizeKeyword(keyword), ownerStatus, commissionTier, page, size, sortBy, sortDir));
        return "admin/commission/shops";
    }

    /**
     * Updates the platform commission rate for a specific shop (FE-12.1 - Admin only).
     */
    @PostMapping("/admin/shops/{id}/commission")
    public String updateCommission(@PathVariable("id") Integer id,
                                   @Valid @ModelAttribute UpdateCommissionRequest request,
                                   BindingResult bindingResult,
                                   @RequestParam(value = "page", defaultValue = "1") int page,
                                   @RequestParam(value = "keyword", required = false) String keyword,
                                   @RequestParam(value = "ownerStatus", defaultValue = "ALL") String ownerStatus,
                                   @RequestParam(value = "commissionTier", defaultValue = "ALL") String commissionTier,
                                   RedirectAttributes redirectAttributes) {
        String cleanKeyword = StringUtils.sanitizeKeyword(keyword);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            redirectAttributes.addAttribute("page", page);
            if (cleanKeyword != null) {
                redirectAttributes.addAttribute("keyword", cleanKeyword);
            }
            redirectAttributes.addAttribute("ownerStatus", ownerStatus);
            redirectAttributes.addAttribute("commissionTier", commissionTier);
            return "redirect:/admin/shops/commission";
        }
        shopService.updateCommissionRate(id, request.getCommissionRate());
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tỷ lệ phí sàn thành công!");
        redirectAttributes.addAttribute("page", page);
        if (cleanKeyword != null) {
            redirectAttributes.addAttribute("keyword", cleanKeyword);
        }
        redirectAttributes.addAttribute("ownerStatus", ownerStatus);
        redirectAttributes.addAttribute("commissionTier", commissionTier);
        return "redirect:/admin/shops/commission";
    }

    // ================= 2. QUẢN LÝ NGƯỜI DÙNG & PHÂN QUYỀN (USERS) =================
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
                            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir,
                            Model model) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(size, 1), 100);
        model.addAllAttributes(userService.getAdminUserPageData(StringUtils.sanitizeKeyword(keyword), roleId, status, safePage, safeSize, sortBy, sortDir));
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
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Integer loggedInUserId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (loggedInUserId != null && loggedInUserId.equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không thể tự khóa tài khoản quản trị đang đăng nhập của chính mình!");
            return RedirectUtils.buildUserRedirectUrl(page, keyword, roleId, status);
        }

        try {
            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return RedirectUtils.buildUserRedirectUrl(page, keyword, roleId, status);
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
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Integer loggedInUserId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (loggedInUserId != null && loggedInUserId.equals(userId)) {
            boolean willKeepAdmin = false;
            if (roleIds != null) {
                List<Role> allRoles = roleService.getAllRoles();
                willKeepAdmin = allRoles.stream()
                        .anyMatch(r -> roleIds.contains(r.getRoleId()) && r.getRoleName() == RoleName.ADMIN);
            }
            if (!willKeepAdmin) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không thể tự gỡ vai trò Quản trị viên (ADMIN) của chính mình!");
                return RedirectUtils.buildUserRedirectUrl(page, keyword, roleId, status);
            }
        }

        try {
            userService.assignRolesToUser(userId, roleIds);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phân quyền vai trò thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return RedirectUtils.buildUserRedirectUrl(page, keyword, roleId, status);
    }

    /**
     * Handles creating a new user account (Admin only).
     */
    @PostMapping("/admin/users/create")
    public String createUser(@Valid @ModelAttribute("newUser") UserRequest request,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/users";
        }
        userService.createUser(request);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản người dùng [" + request.getUsername() + "] thành công!");
        return "redirect:/admin/users";
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
                                 @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir,
                                 Model model) {
        model.addAllAttributes(categoryService.getAdminCategoryPageData(StringUtils.sanitizeKeyword(keyword), status, page, size, sortBy, sortDir));
        return "admin/categories/list";
    }

    /**
     * Handles the creation of a new fruit category.
     */
    @PostMapping("/admin/categories/create")
    public String createCategory(@Valid @ModelAttribute("newCategory") CategoryRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/categories";
        }
        categoryService.createCategory(request);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo danh mục hoa quả mới thành công!");
        return "redirect:/admin/categories";
    }

    /**
     * Updates an existing fruit category by ID.
     */
    @PostMapping("/admin/categories/edit/{id}")
    public String editCategory(@PathVariable("id") Integer id,
                               @Valid @ModelAttribute CategoryRequest request,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/categories";
        }
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
    public String createRole(@Valid @ModelAttribute("newRole") RoleRequest roleRequest,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/roles";
        }
        roleService.createRole(roleRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo vai trò mới thành công!");
        return "redirect:/admin/roles";
    }

    /**
     * Updates an existing role and its permissions by ID.
     */
    @PostMapping("/admin/roles/edit/{id}")
    public String editRole(@PathVariable("id") Long id,
                           @Valid @ModelAttribute RoleRequest roleRequest,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/roles";
        }
        roleService.updateRole(id, roleRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật vai trò thành công!");
        return "redirect:/admin/roles";
    }

    /**
     * Toggles the active/hidden status of a role.
     */
    @PostMapping("/admin/roles/{id}/toggle-status")
    public String toggleRoleStatus(@PathVariable("id") Long id,
                                   RedirectAttributes redirectAttributes) {
        try {
            roleService.toggleRoleStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái hiển thị vai trò thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    // ================= 4. QUẢN LÝ HẠNG THÀNH VIÊN (FE-12.5 & BR-LY-02) =================
    /**
     * Hiển thị danh sách các hạng thành viên (Admin).
     */
    @GetMapping("/admin/membership-tiers")
    public String listMembershipTiers(Model model) {
        model.addAllAttributes(membershipTierService.getAdminMembershipPageData());
        return "admin/membership/list";
    }

    /**
     * Tạo mới hạng thành viên (Admin).
     */
    @PostMapping("/admin/membership-tiers/create")
    public String createMembershipTier(@Valid @ModelAttribute("newTier") MembershipTierRequest request,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/membership-tiers";
        }
        try {
            membershipTierService.createTier(request);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo hạng thành viên mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/membership-tiers";
    }

    /**
     * Cập nhật hạng thành viên (Admin).
     */
    @PostMapping("/admin/membership-tiers/edit/{id}")
    public String editMembershipTier(@PathVariable("id") Integer id,
                                     @Valid @ModelAttribute MembershipTierRequest request,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/membership-tiers";
        }
        try {
            membershipTierService.updateTier(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hạng thành viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/membership-tiers";
    }

    /**
     * Xóa hạng thành viên (Admin).
     */
    @PostMapping("/admin/membership-tiers/delete/{id}")
    public String deleteMembershipTier(@PathVariable("id") Integer id,
                                       RedirectAttributes redirectAttributes) {
        try {
            membershipTierService.deleteTier(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa hạng thành viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/membership-tiers";
    }
}
