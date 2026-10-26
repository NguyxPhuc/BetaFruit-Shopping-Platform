package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.TierCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TierCouponRepository extends JpaRepository<TierCoupon, Integer> {

    List<TierCoupon> findByTier_TierId(Integer tierId);

    List<TierCoupon> findByCoupon_CouponId(Integer couponId);

    @Modifying
    @Query("DELETE FROM TierCoupon tc WHERE tc.tier.tierId = :tierId")
    void deleteByTier_TierId(@Param("tierId") Integer tierId);

    boolean existsByTier_TierIdAndCoupon_CouponId(Integer tierId, Integer couponId);
}
