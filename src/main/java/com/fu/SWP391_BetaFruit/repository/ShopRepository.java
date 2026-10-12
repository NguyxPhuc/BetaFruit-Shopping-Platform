package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {
    Optional<Shop> findByOwner(User owner);

    long countByApprovalStatus(ShopApprovalStatus status);

    @Query("SELECT s FROM Shop s JOIN s.owner u WHERE " +
           "(:status IS NULL OR s.approvalStatus = :status) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Shop> searchShopsForAdmin(@Param("keyword") String keyword,
                                   @Param("status") ShopApprovalStatus status,
                                   Pageable pageable);

    @Query("SELECT s FROM Shop s JOIN s.owner u WHERE " +
           "s.approvalStatus = 'APPROVED' AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.bankAccountNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:ownerStatus IS NULL OR :ownerStatus = 'ALL' OR " +
           " (:ownerStatus = 'ACTIVE' AND u.status = com.fu.SWP391_BetaFruit.enums.UserStatus.ACTIVE) OR " +
           " (:ownerStatus = 'DEACTIVATED' AND u.status = com.fu.SWP391_BetaFruit.enums.UserStatus.DEACTIVATED)) AND " +
           "(:commissionTier IS NULL OR :commissionTier = 'ALL' OR " +
           " (:commissionTier = 'DEFAULT' AND s.commissionRate = 5.00) OR " +
           " (:commissionTier = 'DISCOUNTED' AND s.commissionRate < 5.00) OR " +
           " (:commissionTier = 'HIGH' AND s.commissionRate > 5.00))")
    Page<Shop> searchApprovedShopsForCommission(@Param("keyword") String keyword,
                                                @Param("ownerStatus") String ownerStatus,
                                                @Param("commissionTier") String commissionTier,
                                                Pageable pageable);

    @Query("SELECT COUNT(s) FROM Shop s JOIN s.owner u WHERE s.approvalStatus = 'APPROVED' AND u.status = com.fu.SWP391_BetaFruit.enums.UserStatus.ACTIVE")
    long countApprovedActiveOwnerShops();

    @Query("SELECT COUNT(s) FROM Shop s JOIN s.owner u WHERE s.approvalStatus = 'APPROVED' AND u.status = com.fu.SWP391_BetaFruit.enums.UserStatus.DEACTIVATED")
    long countApprovedDeactivatedOwnerShops();
}
