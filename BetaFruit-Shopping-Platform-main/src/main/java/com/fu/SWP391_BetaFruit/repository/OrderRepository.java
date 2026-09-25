package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    @EntityGraph(attributePaths = "customer")
    @Query("""
            select o
            from CustomerOrder o
            where o.shop.owner.userId = :ownerId
              and (:status is null or o.orderStatus = :status)
              and (:keyword is null
                   or lower(o.customer.fullName) like lower(concat('%', :keyword, '%'))
                   or str(o.orderId) like concat('%', :keyword, '%'))
            """)
    Page<Order> searchOrdersForShopOwner(
            @Param("ownerId") Integer ownerId,
            @Param("keyword") String keyword,
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "customer")
    java.util.Optional<Order> findByOrderIdAndShopOwnerUserId(Integer orderId, Integer ownerId);

    @EntityGraph(attributePaths = {"customer", "shop", "coupon"})
    @Query("select o from CustomerOrder o where o.orderId = :orderId and o.shop.owner.userId = :ownerId")
    java.util.Optional<Order> findDetailForShopOwner(
            @Param("orderId") Integer orderId, @Param("ownerId") Integer ownerId);
}
