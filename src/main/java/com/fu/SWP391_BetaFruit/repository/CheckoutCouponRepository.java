package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Checkout-only access. Does not change the team's coupon management repository. */
public interface CheckoutCouponRepository extends Repository<Coupon, Integer> {
    Optional<Coupon> findByCouponCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.couponId = :id")
    Optional<Coupon> findLockedById(@Param("id") Integer id);
}
