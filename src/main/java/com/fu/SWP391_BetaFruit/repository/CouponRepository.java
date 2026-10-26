package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Integer> {

    // Shop Owner queries
    Page<Coupon> findByShop_ShopId(Integer shopId, Pageable pageable);

    Page<Coupon> findByShop_ShopIdAndCouponCodeContainingIgnoreCase(Integer shopId, String keyword, Pageable pageable);

    Optional<Coupon> findByCouponIdAndShop_ShopId(Integer couponId, Integer shopId);

    long countByShop_ShopId(Integer shopId);

    long countByShop_ShopIdAndIsActiveTrue(Integer shopId);

    // Admin queries (platform-wide coupons where shop is null)
    Page<Coupon> findByShopIsNull(Pageable pageable);

    List<Coupon> findByShopIsNullAndIsActiveTrue();

    Page<Coupon> findByShopIsNullAndCouponCodeContainingIgnoreCase(String keyword, Pageable pageable);

    long countByShopIsNull();

    long countByShopIsNullAndIsActiveTrue();

    // All coupons (with keyword)
    Page<Coupon> findByCouponCodeContainingIgnoreCase(String keyword, Pageable pageable);

    // Uniqueness validation
    boolean existsByCouponCodeIgnoreCase(String couponCode);

    boolean existsByCouponCodeIgnoreCaseAndCouponIdNot(String couponCode, Integer couponId);

    Optional<Coupon> findByCouponCodeIgnoreCase(String couponCode);
}
