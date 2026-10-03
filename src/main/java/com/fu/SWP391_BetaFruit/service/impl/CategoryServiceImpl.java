package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.CategoryRequest;
import com.fu.SWP391_BetaFruit.entity.MasterCategory;
import com.fu.SWP391_BetaFruit.repository.MasterCategoryRepository;
import com.fu.SWP391_BetaFruit.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public void createCategory(CategoryRequest request) {
        if (request.getCategoryName() == null || request.getCategoryName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục hoa quả không được để trống!");
        }

        String trimmedName = request.getCategoryName().trim();
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
    public void updateCategory(Integer id, CategoryRequest request) {
        if (request.getCategoryName() == null || request.getCategoryName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục hoa quả không được để trống!");
        }

        String trimmedName = request.getCategoryName().trim();
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

    private Boolean parseActiveStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty() || "ALL".equalsIgnoreCase(statusStr.trim())) {
            return null;
        }
        String clean = statusStr.trim();
        if ("ACTIVE".equalsIgnoreCase(clean) || "TRUE".equalsIgnoreCase(clean)) {
            return Boolean.TRUE;
        }
        if ("HIDDEN".equalsIgnoreCase(clean) || "INACTIVE".equalsIgnoreCase(clean) || "FALSE".equalsIgnoreCase(clean)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static final Set<String> ALLOWED_CATEGORY_SORT_FIELDS = Set.of(
            "categoryId", "categoryName", "isActive"
    );

    @Override
    public Page<MasterCategory> getAdminCategoryPage(String keyword, int page, int size) {
        return getAdminCategoryPage(keyword, "ALL", page, size, "categoryId", "desc");
    }

    @Override
    public Page<MasterCategory> getAdminCategoryPage(String keyword, int page, int size, String sortBy, String sortDir) {
        return getAdminCategoryPage(keyword, "ALL", page, size, sortBy, sortDir);
    }

    @Override
    public Page<MasterCategory> getAdminCategoryPage(String keyword, String status, int page, int size, String sortBy, String sortDir) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 5;

        String safeSortBy = (sortBy != null && ALLOWED_CATEGORY_SORT_FIELDS.contains(sortBy.trim()))
                ? sortBy.trim() : "categoryId";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, safeSortBy));

        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        Boolean isActive = parseActiveStatus(status);

        return masterCategoryRepository.filterCategories(cleanKeyword, isActive, pageable);
    }

    @Override
    public Map<String, Object> getAdminCategoryPageData(String keyword) {
        return getAdminCategoryPageData(keyword, "ALL", 1, 5, "categoryId", "desc");
    }

    @Override
    public Map<String, Object> getAdminCategoryPageData(String keyword, int page, int size) {
        return getAdminCategoryPageData(keyword, "ALL", page, size, "categoryId", "desc");
    }

    @Override
    public Map<String, Object> getAdminCategoryPageData(String keyword, int page, int size, String sortBy, String sortDir) {
        return getAdminCategoryPageData(keyword, "ALL", page, size, sortBy, sortDir);
    }

    @Override
    public Map<String, Object> getAdminCategoryPageData(String keyword, String status, int page, int size, String sortBy, String sortDir) {
        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        String safeSortBy = (sortBy != null && ALLOWED_CATEGORY_SORT_FIELDS.contains(sortBy.trim()))
                ? sortBy.trim() : "categoryId";
        String safeSortDir = "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";

        Boolean parsedStatus = parseActiveStatus(status);
        String currentStatus = (parsedStatus == null) ? "ALL" : (parsedStatus ? "ACTIVE" : "HIDDEN");

        Page<MasterCategory> categoryPage = getAdminCategoryPage(cleanKeyword, currentStatus, page, size, safeSortBy, safeSortDir);

        Map<String, Object> data = new HashMap<>();
        data.put("categories", categoryPage.getContent());
        data.put("categoryPage", categoryPage);
        data.put("pageData", categoryPage);
        data.put("newCategory", new CategoryRequest());
        data.put("keyword", cleanKeyword);
        data.put("currentStatus", currentStatus);
        data.put("sortBy", safeSortBy);
        data.put("sortDir", safeSortDir);
        data.put("totalCount", masterCategoryRepository.count());
        data.put("activeCount", masterCategoryRepository.countByIsActive(true));
        data.put("hiddenCount", masterCategoryRepository.countByIsActive(false));

        return data;
    }
}
