package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {
    List<ProductImage> findByProduct_ProductIdInOrderByImageIdAsc(java.util.Collection<Integer> productIds);
    List<ProductImage> findByProduct_ProductId(Integer productId);
    Optional<ProductImage> findFirstByProduct_ProductIdAndIsPrimaryTrue(Integer productId);
}
