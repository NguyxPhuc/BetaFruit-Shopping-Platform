package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    List<ProductVariant> findByProduct_ProductId(Integer productId);
    Optional<ProductVariant> findFirstByProduct_ProductId(Integer productId);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM ProductVariant v WHERE v.product.shop.shopId = :shopId AND (v.stockQuantity <= COALESCE(v.lowStockThreshold, 5) OR v.stockQuantity <= 0)")
    List<ProductVariant> findLowStockVariantsByShopId(@org.springframework.data.repository.query.Param("shopId") Integer shopId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(v) FROM ProductVariant v WHERE v.product.shop.shopId = :shopId AND (v.stockQuantity <= COALESCE(v.lowStockThreshold, 5) OR v.stockQuantity <= 0)")
    long countLowStockVariantsByShopId(@org.springframework.data.repository.query.Param("shopId") Integer shopId);
}
