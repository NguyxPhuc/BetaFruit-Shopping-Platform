package com.fu.SWP391_BetaFruit.repository;

import jakarta.persistence.LockModeType;
import com.fu.SWP391_BetaFruit.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.variantId = :variantId")
    Optional<ProductVariant> findForStockUpdate(@Param("variantId") Integer variantId);

    List<ProductVariant> findByProduct_ProductId(Integer productId);
    Optional<ProductVariant> findFirstByProduct_ProductId(Integer productId);

    @Query("SELECT v FROM ProductVariant v WHERE v.product.shop.shopId = :shopId AND (v.stockQuantity <= COALESCE(v.lowStockThreshold, 5) OR v.stockQuantity <= 0)")
    List<ProductVariant> findLowStockVariantsByShopId(@Param("shopId") Integer shopId);

    @Query("SELECT COUNT(v) FROM ProductVariant v WHERE v.product.shop.shopId = :shopId AND (v.stockQuantity <= COALESCE(v.lowStockThreshold, 5) OR v.stockQuantity <= 0)")
    long countLowStockVariantsByShopId(@Param("shopId") Integer shopId);

    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product p " +
            "WHERE p.shop.owner.userId = :ownerId " +
            "AND p.approvalStatus = 'APPROVED' " +
            "ORDER BY p.productName ASC, v.variantName ASC")
    List<ProductVariant> findAllApprovedVariantsByShopOwner(@Param("ownerId") Integer ownerId);
}
