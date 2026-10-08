package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.MasterCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasterCategoryRepository extends JpaRepository<MasterCategory, Integer> {
    boolean existsByCategoryNameIgnoreCase(String categoryName);
    boolean existsByCategoryNameIgnoreCaseAndCategoryIdNot(String categoryName, Integer categoryId);
    List<MasterCategory> findByCategoryNameContainingIgnoreCase(String keyword);
    Page<MasterCategory> findByCategoryNameContainingIgnoreCase(String keyword, Pageable pageable);
    long countByIsActive(Boolean isActive);

    @Query("SELECT c FROM MasterCategory c WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:isActive IS NULL OR c.isActive = :isActive)")
    Page<MasterCategory> filterCategories(@Param("keyword") String keyword,
                                          @Param("isActive") Boolean isActive,
                                          Pageable pageable);
}
