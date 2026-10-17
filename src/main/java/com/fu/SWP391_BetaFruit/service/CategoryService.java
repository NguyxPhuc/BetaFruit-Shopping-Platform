package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.CategoryRequest;
import com.fu.SWP391_BetaFruit.entity.MasterCategory;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface CategoryService {
    List<MasterCategory> getActiveCategories();
    MasterCategory getCategoryById(Integer id);
    void createCategory(CategoryRequest request);
    void updateCategory(Integer id, CategoryRequest request);
    void toggleCategoryStatus(Integer id);
    Page<MasterCategory> getAdminCategoryPage(String keyword, String status, int page, int size, String sortBy, String sortDir);
    Map<String, Object> getAdminCategoryPageData(String keyword, String status, int page, int size, String sortBy, String sortDir);
}
