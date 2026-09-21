package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.admin.CategoryDto;
import com.fu.SWP391_BetaFruit.entity.MasterCategory;
import com.fu.SWP391_BetaFruit.repository.MasterCategoryRepository;
import com.fu.SWP391_BetaFruit.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final MasterCategoryRepository masterCategoryRepository;

    @Override
    public List<MasterCategory> getAllCategories() {
        return masterCategoryRepository.findAll();
    }

    @Override
    public List<MasterCategory> searchCategories(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return masterCategoryRepository.findAll();
        }
        return masterCategoryRepository.findByCategoryNameContainingIgnoreCase(keyword.trim());
    }

    @Override
    public MasterCategory getCategoryById(Integer id) {
        return masterCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục hoa quả với mã ID: " + id));
    }

    @Override
    @Transactional
    public void createCategory(CategoryDto dto) {
        if (dto.getCategoryName() == null || dto.getCategoryName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục hoa quả không được để trống!");
        }

        String trimmedName = dto.getCategoryName().trim();
        if (masterCategoryRepository.existsByCategoryNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("Danh mục hoa quả mang tên '" + trimmedName + "' đã tồn tại trong hệ thống!");
        }

        MasterCategory category = new MasterCategory();
        category.setCategoryName(trimmedName);
        category.setIsActive(true); // Mặc định mở kích hoạt
        masterCategoryRepository.save(category);
    }

    @Override
    @Transactional
    public void updateCategory(Integer id, CategoryDto dto) {
        if (dto.getCategoryName() == null || dto.getCategoryName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục hoa quả không được để trống!");
        }

        String trimmedName = dto.getCategoryName().trim();
        if (masterCategoryRepository.existsByCategoryNameIgnoreCaseAndCategoryIdNot(trimmedName, id)) {
            throw new IllegalArgumentException("Tên danh mục hoa quả '" + trimmedName + "' đã được sử dụng bởi danh mục khác!");
        }

        MasterCategory category = getCategoryById(id);
        category.setCategoryName(trimmedName);
        masterCategoryRepository.save(category);
    }

    @Override
    @Transactional
    public void toggleCategoryStatus(Integer id) {
        MasterCategory category = getCategoryById(id);
        boolean currentStatus = category.getIsActive() != null && category.getIsActive();
        category.setIsActive(!currentStatus);
        masterCategoryRepository.save(category);
    }
}
