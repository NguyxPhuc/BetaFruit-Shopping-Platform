package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {

    List<Shop> findByApprovalStatus(ShopApprovalStatus status);

    Page<Shop> findByApprovalStatus(ShopApprovalStatus status, Pageable pageable);

    long countByApprovalStatus(ShopApprovalStatus status);

    @Query("SELECT s FROM Shop s JOIN s.owner u WHERE " +
           "(LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Shop> searchShops(@Param("keyword") String keyword);

    @Query(value = "SELECT s FROM Shop s JOIN s.owner u WHERE " +
           "(LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(s) FROM Shop s JOIN s.owner u WHERE " +
           "(LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Shop> searchShops(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Shop s JOIN s.owner u WHERE " +
           "s.approvalStatus = :status AND " +
           "(LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Shop> searchShopsByStatus(@Param("keyword") String keyword, @Param("status") ShopApprovalStatus status);

    @Query(value = "SELECT s FROM Shop s JOIN s.owner u WHERE " +
           "s.approvalStatus = :status AND " +
           "(LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(s) FROM Shop s JOIN s.owner u WHERE " +
           "s.approvalStatus = :status AND " +
           "(LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Shop> searchShopsByStatus(@Param("keyword") String keyword, @Param("status") ShopApprovalStatus status, Pageable pageable);
}
