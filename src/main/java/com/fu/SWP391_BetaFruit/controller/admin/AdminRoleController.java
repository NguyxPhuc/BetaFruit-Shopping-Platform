package com.fu.SWP391_BetaFruit.controller.admin;

import com.fu.SWP391_BetaFruit.dto.admin.RoleDto;
import com.fu.SWP391_BetaFruit.entity.Role;
import com.fu.SWP391_BetaFruit.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleService roleService;

    @GetMapping
    public String listRoles(Model model) {
        List<Role> roles = roleService.getAllRoles();
        model.addAttribute("roles", roles);
        model.addAttribute("newRole", new RoleDto());
        return "admin/roles/list";
    }

    @PostMapping("/create")
    public String createRole(@ModelAttribute("newRole") RoleDto roleDto, RedirectAttributes redirectAttributes) {
        try {
            roleService.createRole(roleDto);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo vai trò mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @PostMapping("/edit/{id}")
    public String editRole(@PathVariable("id") Long id, @ModelAttribute RoleDto roleDto, RedirectAttributes redirectAttributes) {
        try {
            roleService.updateRole(id, roleDto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật vai trò thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/roles";
    }
}
