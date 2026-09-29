package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.admin.CategoryRequest;
import com.fu.SWP391_BetaFruit.entity.MasterCategory;

import java.util.List;

public interface CategoryService {
    List<MasterCategory> getAllCategories();
    List<MasterCategory> searchCategories(String keyword);
    MasterCategory getCategoryById(Integer id);
    void createCategory(CategoryRequest request);
    void updateCategory(Integer id, CategoryRequest request);
    void toggleCategoryStatus(Integer id);
    java.util.Map<String, Object> getAdminCategoryPageData(String keyword);
}
