package com.fu.SWP391_BetaFruit.controller.admin;

import com.fu.SWP391_BetaFruit.entity.Role;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.service.RoleService;
import com.fu.SWP391_BetaFruit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final RoleService roleService;

    @GetMapping
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<User> users;
        if (keyword != null && !keyword.trim().isEmpty()) {
            users = userService.searchUsers(keyword.trim());
        } else {
            users = userService.getAllUsers();
        }

        List<Role> allRoles = roleService.getAllRoles();

        model.addAttribute("users", users);
        model.addAttribute("allRoles", allRoles);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        return "admin/users/list";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/assign-roles")
    public String assignRoles(@RequestParam("userId") Integer userId,
                              @RequestParam(value = "roleIds", required = false) List<Long> roleIds,
                              RedirectAttributes redirectAttributes) {
        try {
            userService.assignRolesToUser(userId, roleIds);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phân quyền vai trò thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
