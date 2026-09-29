package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {
    Optional<Shop> findByOwner(User owner);

    List<Shop> findByApprovalStatus(ShopApprovalStatus status);

    long countByApprovalStatus(ShopApprovalStatus status);

    @Query("SELECT s FROM Shop s JOIN s.owner u WHERE " +
           "(LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Shop> searchShops(@Param("keyword") String keyword);

    @Query("SELECT s FROM Shop s JOIN s.owner u WHERE " +
           "s.approvalStatus = :status AND " +
           "(LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Shop> searchShopsByStatus(@Param("keyword") String keyword, @Param("status") ShopApprovalStatus status);
}
