package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Product;
import com.fu.SWP391_BetaFruit.enums.ProductApprovalStatus;
import com.fu.SWP391_BetaFruit.enums.ProductVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByApprovalStatus(ProductApprovalStatus status);

    List<Product> findByApprovalStatusAndVisibilityStatus(ProductApprovalStatus approvalStatus, ProductVisibility visibilityStatus);

    long countByApprovalStatus(ProductApprovalStatus status);

    long countByApprovalStatusAndVisibilityStatus(ProductApprovalStatus approvalStatus, ProductVisibility visibilityStatus);

    @Query("SELECT p FROM Product p JOIN p.shop s JOIN p.category c WHERE " +
           "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchAll(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p JOIN p.shop s JOIN p.category c WHERE " +
           "p.approvalStatus = :status AND " +
           "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchByApprovalStatus(@Param("keyword") String keyword, @Param("status") ProductApprovalStatus status);

    @Query("SELECT p FROM Product p JOIN p.shop s JOIN p.category c WHERE " +
           "p.approvalStatus = :approvalStatus AND p.visibilityStatus = :visibilityStatus AND " +
           "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchByApprovalAndVisibility(@Param("keyword") String keyword,
                                                @Param("approvalStatus") ProductApprovalStatus approvalStatus,
                                                @Param("visibilityStatus") ProductVisibility visibilityStatus);
}
