package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderId = :orderId")
    Optional<Order> findLockedById(@Param("orderId") Integer orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByOrderIdAndShopOwnerUserId(Integer orderId, Integer ownerId);

    @EntityGraph(attributePaths = {"customer", "shop", "coupon"})
    @Query("select o from Order o where o.orderId = :orderId and o.shop.owner.userId = :ownerId")
    Optional<Order> findDetailForShopOwner(@Param("orderId") Integer orderId, @Param("ownerId") Integer ownerId);

    @EntityGraph(attributePaths = {"customer", "shop", "coupon"})
    @Query("""
            select o
            from Order o
            join o.shop s
            left join o.coupon c
            where o.shop.owner.userId = :ownerId
              and (:status is null or o.orderStatus = :status)
              and (:keyword is null
                   or lower(o.customer.fullName) like lower(concat('%', :keyword, '%'))
                   or str(o.orderId) like concat('%', :keyword, '%'))
            """)
    Page<Order> searchOrdersForShopOwner(@Param("ownerId") Integer ownerId,
            @Param("keyword") String keyword, @Param("status") OrderStatus status, Pageable pageable);

    // Coupon tracking queries
    Page<Order> findByCoupon_CouponId(Integer couponId, Pageable pageable);

    Page<Order> findByCoupon_CouponIdAndShop_ShopId(Integer couponId, Integer shopId, Pageable pageable);

    long countByCoupon_CouponId(Integer couponId);

    long countByCoupon_CouponIdAndShop_ShopId(Integer couponId, Integer shopId);

    @Query("SELECT COALESCE(SUM(o.discountAmount), 0) FROM Order o WHERE o.coupon.couponId = :couponId")
    BigDecimal sumTotalDiscountByCouponId(@Param("couponId") Integer couponId);

    @Query("SELECT COALESCE(SUM(o.discountAmount), 0) FROM Order o WHERE o.coupon.couponId = :couponId AND o.shop.shopId = :shopId")
    BigDecimal sumTotalDiscountByCouponIdAndShopId(@Param("couponId") Integer couponId, @Param("shopId") Integer shopId);

    Optional<Order> findByOrderId(Integer orderId);

    long countByCustomer_UserIdAndOrderStatus(Integer customerId, OrderStatus orderStatus);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.customer.userId = :customerId AND o.orderStatus = :orderStatus")
    BigDecimal sumTotalSpentByCustomerIdAndStatus(@Param("customerId") Integer customerId, @Param("orderStatus") OrderStatus orderStatus);

    @EntityGraph(attributePaths = {"shop", "coupon"})
    Page<Order> findByCustomer_UserId(Integer customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"shop", "coupon"})
    Page<Order> findByCustomer_UserIdAndOrderStatus(Integer customerId, OrderStatus orderStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"shop", "coupon"})
    Optional<Order> findByOrderIdAndCustomer_UserId(Integer orderId, Integer customerId);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.orderStatus IN (:statuses)")
    BigDecimal sumTotalRevenueByStatuses(@Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(o.platformFee), 0) FROM Order o WHERE o.orderStatus IN (:statuses)")
    BigDecimal sumTotalPlatformFeeByStatuses(@Param("statuses") List<OrderStatus> statuses);

    long countByOrderStatusIn(List<OrderStatus> statuses);

    // =========================================================================
    // VOUCHER ANALYTICS DASHBOARD QUERIES (PLATFORM & SHOP)
    // =========================================================================

    @Query("SELECT SUM(o.discountAmount) FROM Order o WHERE o.coupon IS NOT NULL AND o.coupon.shop IS NULL AND o.orderStatus != 'CANCELLED'")
    BigDecimal sumPlatformSponsoredDiscount();

    @Query("SELECT SUM(o.discountAmount) FROM Order o WHERE o.coupon IS NOT NULL AND o.coupon.shop IS NOT NULL AND o.orderStatus != 'CANCELLED'")
    BigDecimal sumShopSponsoredDiscount();

    @Query("SELECT SUM(o.discountAmount) FROM Order o WHERE o.coupon IS NOT NULL AND o.shop.shopId = :shopId AND o.coupon.shop.shopId = :shopId AND o.orderStatus != 'CANCELLED'")
    BigDecimal sumShopSponsoredDiscountForShop(@Param("shopId") Integer shopId);

    @Query("SELECT SUM(o.discountAmount) FROM Order o WHERE o.coupon IS NOT NULL AND o.shop.shopId = :shopId AND o.coupon.shop IS NULL AND o.orderStatus != 'CANCELLED'")
    BigDecimal sumPlatformSponsoredDiscountForShop(@Param("shopId") Integer shopId);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.coupon IS NOT NULL AND o.orderStatus != 'CANCELLED'")
    BigDecimal sumTotalVoucherRevenue();

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.coupon IS NOT NULL AND o.shop.shopId = :shopId AND o.orderStatus != 'CANCELLED'")
    BigDecimal sumTotalVoucherRevenueForShop(@Param("shopId") Integer shopId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.coupon IS NOT NULL AND o.orderStatus != 'CANCELLED'")
    long countVoucherOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.coupon IS NOT NULL AND o.shop.shopId = :shopId AND o.orderStatus != 'CANCELLED'")
    long countVoucherOrdersForShop(@Param("shopId") Integer shopId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus != 'CANCELLED'")
    long countTotalValidOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.shop.shopId = :shopId AND o.orderStatus != 'CANCELLED'")
    long countTotalValidOrdersForShop(@Param("shopId") Integer shopId);

    @EntityGraph(attributePaths = {"customer", "shop", "coupon"})
    @Query("SELECT o FROM Order o WHERE o.coupon IS NOT NULL AND o.orderStatus != 'CANCELLED' ORDER BY o.orderId DESC")
    List<Order> findAllVoucherOrders();

    @EntityGraph(attributePaths = {"customer", "shop", "coupon"})
    @Query("SELECT o FROM Order o WHERE o.coupon IS NOT NULL AND o.shop.shopId = :shopId AND o.orderStatus != 'CANCELLED' ORDER BY o.orderId DESC")
    List<Order> findAllVoucherOrdersByShopId(@Param("shopId") Integer shopId);
}

