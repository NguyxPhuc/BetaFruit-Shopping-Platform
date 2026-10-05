package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.variantId = :variantId")
    Optional<ProductVariant> findForStockUpdate(@Param("variantId") Integer variantId);
}
