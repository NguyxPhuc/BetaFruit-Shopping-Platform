package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.admin.CategoryDto;
import com.fu.SWP391_BetaFruit.entity.MasterCategory;

import java.util.List;

public interface CategoryService {
    List<MasterCategory> getAllCategories();
    List<MasterCategory> searchCategories(String keyword);
    MasterCategory getCategoryById(Integer id);
    void createCategory(CategoryDto dto);
    void updateCategory(Integer id, CategoryDto dto);
    void toggleCategoryStatus(Integer id);
}
