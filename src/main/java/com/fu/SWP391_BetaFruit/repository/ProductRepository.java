package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Product;
import com.fu.SWP391_BetaFruit.enums.ProductApprovalStatus;
import com.fu.SWP391_BetaFruit.enums.ProductVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    Page<Product> findByApprovalStatus(ProductApprovalStatus status, Pageable pageable);

    Page<Product> findByApprovalStatusAndVisibilityStatus(ProductApprovalStatus approvalStatus, ProductVisibility visibilityStatus, Pageable pageable);

    long countByApprovalStatus(ProductApprovalStatus status);

    long countByApprovalStatusAndVisibilityStatus(ProductApprovalStatus approvalStatus, ProductVisibility visibilityStatus);

    @Query(value = "SELECT p FROM Product p JOIN p.shop s JOIN p.category c WHERE " +
           "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(p) FROM Product p JOIN p.shop s JOIN p.category c WHERE " +
           "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchAll(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT p FROM Product p JOIN p.shop s JOIN p.category c WHERE " +
           "p.approvalStatus = :status AND " +
           "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(p) FROM Product p JOIN p.shop s JOIN p.category c WHERE " +
           "p.approvalStatus = :status AND " +
           "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByApprovalStatus(@Param("keyword") String keyword, @Param("status") ProductApprovalStatus status, Pageable pageable);

    @Query(value = "SELECT p FROM Product p JOIN p.shop s JOIN p.category c WHERE " +
           "p.approvalStatus = :approvalStatus AND p.visibilityStatus = :visibilityStatus AND " +
           "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(p) FROM Product p JOIN p.shop s JOIN p.category c WHERE " +
           "p.approvalStatus = :approvalStatus AND p.visibilityStatus = :visibilityStatus AND " +
           "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByApprovalAndVisibility(@Param("keyword") String keyword,
                                                @Param("approvalStatus") ProductApprovalStatus approvalStatus,
                                                @Param("visibilityStatus") ProductVisibility visibilityStatus,
                                                Pageable pageable);

    Page<Product> findByShop_ShopId(Integer shopId, Pageable pageable);

    Page<Product> findByShop_ShopIdAndProductNameContainingIgnoreCase(Integer shopId, String keyword, Pageable pageable);

    long countByShop_ShopId(Integer shopId);
}
