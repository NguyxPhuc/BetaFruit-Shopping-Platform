package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.MasterCategory;
import com.fu.SWP391_BetaFruit.repository.MasterCategoryRepository;
import com.fu.SWP391_BetaFruit.repository.ProductRepository;
import com.fu.SWP391_BetaFruit.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử Nghiệp vụ Quản lý Danh mục Gốc (Master Category) & Ràng buộc toàn vẹn")
class CategoryServiceTest {

    @Mock
    private MasterCategoryRepository masterCategoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private MasterCategory activeCategory;
    private MasterCategory hiddenCategory;

    @BeforeEach
    void setUp() {
        activeCategory = new MasterCategory();
        activeCategory.setCategoryId(1);
        activeCategory.setCategoryName("Trái Cây Nhập Khẩu");
        activeCategory.setIsActive(true);

        hiddenCategory = new MasterCategory();
        hiddenCategory.setCategoryId(2);
        hiddenCategory.setCategoryName("Trái Cây Sấy Khô");
        hiddenCategory.setIsActive(false);
    }

    @Test
    @DisplayName("Chặn ẩn danh mục khi đang có sản phẩm (ném IllegalStateException)")
    void toggleCategoryStatus_WhenActiveCategoryHasProducts_ShouldThrowException() {
        when(masterCategoryRepository.findById(1)).thenReturn(Optional.of(activeCategory));
        when(productRepository.countByCategory_CategoryId(1)).thenReturn(5L);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            categoryService.toggleCategoryStatus(1);
        });

        assertTrue(exception.getMessage().contains("Không thể ẩn danh mục"));
        assertTrue(exception.getMessage().contains("5 sản phẩm"));
        verify(masterCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cho phép ẩn danh mục khi số lượng sản phẩm bằng 0")
    void toggleCategoryStatus_WhenActiveCategoryHasNoProducts_ShouldToggleToHidden() {
        when(masterCategoryRepository.findById(1)).thenReturn(Optional.of(activeCategory));
        when(productRepository.countByCategory_CategoryId(1)).thenReturn(0L);

        categoryService.toggleCategoryStatus(1);

        assertFalse(activeCategory.getIsActive(), "Danh mục phải được chuyển sang trạng thái ẩn (false)");
        verify(masterCategoryRepository, times(1)).save(activeCategory);
    }

    @Test
    @DisplayName("Luôn cho phép kích hoạt lại danh mục đang ẩn (Hiện danh mục)")
    void toggleCategoryStatus_WhenCategoryIsHidden_ShouldToggleToActive() {
        when(masterCategoryRepository.findById(2)).thenReturn(Optional.of(hiddenCategory));

        categoryService.toggleCategoryStatus(2);

        assertTrue(hiddenCategory.getIsActive(), "Danh mục phải được chuyển sang trạng thái kích hoạt (true)");
        verify(productRepository, never()).countByCategory_CategoryId(any());
        verify(masterCategoryRepository, times(1)).save(hiddenCategory);
    }

    @Test
    @DisplayName("Nạp dữ liệu trang Admin kèm theo bản đồ số lượng sản phẩm (productCountMap)")
    void getAdminCategoryPageData_ShouldIncludeProductCountMap() {
        when(masterCategoryRepository.filterCategories(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activeCategory, hiddenCategory)));
        when(masterCategoryRepository.count()).thenReturn(2L);
        when(masterCategoryRepository.countByIsActive(true)).thenReturn(1L);
        when(masterCategoryRepository.countByIsActive(false)).thenReturn(1L);

        List<Object[]> mockCounts = List.of(
                new Object[]{1, 15L},
                new Object[]{2, 0L}
        );
        when(productRepository.countProductsGroupedByCategory()).thenReturn(mockCounts);

        Map<String, Object> data = categoryService.getAdminCategoryPageData("", "ALL", 1, 5, "categoryId", "asc");

        assertNotNull(data);
        assertTrue(data.containsKey("productCountMap"));
        @SuppressWarnings("unchecked")
        Map<Integer, Long> countMap = (Map<Integer, Long>) data.get("productCountMap");
        assertEquals(15L, countMap.get(1));
        assertEquals(0L, countMap.get(2));
    }
}
