package com.fu.SWP391_BetaFruit.controller.admin;

import com.fu.SWP391_BetaFruit.dto.admin.CategoryDto;
import com.fu.SWP391_BetaFruit.entity.MasterCategory;
import com.fu.SWP391_BetaFruit.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String listCategories(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<MasterCategory> categories;
        if (keyword != null && !keyword.trim().isEmpty()) {
            categories = categoryService.searchCategories(keyword.trim());
        } else {
            categories = categoryService.getAllCategories();
        }

        model.addAttribute("categories", categories);
        model.addAttribute("newCategory", new CategoryDto());
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        return "admin/categories/list";
    }

    @PostMapping("/create")
    public String createCategory(@ModelAttribute("newCategory") CategoryDto dto, RedirectAttributes redirectAttributes) {
        try {
            categoryService.createCategory(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo danh mục hoa quả mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/edit/{id}")
    public String editCategory(@PathVariable("id") Integer id,
                               @ModelAttribute CategoryDto dto,
                               RedirectAttributes redirectAttributes) {
        try {
            categoryService.updateCategory(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục hoa quả thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.toggleCategoryStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái hiển thị danh mục thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
