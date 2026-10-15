package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Coupon tracking queries
    Page<Order> findByCoupon_CouponId(Integer couponId, Pageable pageable);

    Page<Order> findByCoupon_CouponIdAndShop_ShopId(Integer couponId, Integer shopId, Pageable pageable);

    long countByCoupon_CouponId(Integer couponId);

    long countByCoupon_CouponIdAndShop_ShopId(Integer couponId, Integer shopId);

    @Query("SELECT COALESCE(SUM(o.discountAmount), 0) FROM Order o WHERE o.coupon.couponId = :couponId")
    BigDecimal sumTotalDiscountByCouponId(@Param("couponId") Integer couponId);

    @Query("SELECT COALESCE(SUM(o.discountAmount), 0) FROM Order o WHERE o.coupon.couponId = :couponId AND o.shop.shopId = :shopId")
    BigDecimal sumTotalDiscountByCouponIdAndShopId(@Param("couponId") Integer couponId, @Param("shopId") Integer shopId);
}
