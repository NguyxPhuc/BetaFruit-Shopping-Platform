package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.InventoryTransaction;
import com.fu.SWP391_BetaFruit.enums.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Integer> {

    @Query("SELECT it FROM InventoryTransaction it " +
           "JOIN it.variant v " +
           "JOIN v.product p " +
           "WHERE p.shop.shopId = :shopId " +
           "ORDER BY it.transactionId DESC")
    Page<InventoryTransaction> findByShopId(@Param("shopId") Integer shopId, Pageable pageable);

    @Query("SELECT it FROM InventoryTransaction it " +
           "JOIN it.variant v " +
           "JOIN v.product p " +
           "WHERE p.shop.shopId = :shopId AND it.transactionType = :type " +
           "ORDER BY it.transactionId DESC")
    Page<InventoryTransaction> findByShopIdAndType(@Param("shopId") Integer shopId,
                                                   @Param("type") InventoryTransactionType type,
                                                   Pageable pageable);

    @Query("SELECT it FROM InventoryTransaction it " +
           "JOIN it.variant v " +
           "JOIN v.product p " +
           "WHERE p.shop.shopId = :shopId AND v.variantId = :variantId " +
           "ORDER BY it.transactionId DESC")
    Page<InventoryTransaction> findByShopIdAndVariantId(@Param("shopId") Integer shopId,
                                                        @Param("variantId") Integer variantId,
                                                        Pageable pageable);
}
