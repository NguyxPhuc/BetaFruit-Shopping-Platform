package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.OrderItem;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrderOrderIdOrderByOrderItemIdAsc(Integer orderId);

    @Query("SELECT i.variant.product.productId, SUM(i.quantity) FROM OrderItem i " +
            "WHERE i.order.orderStatus = :status GROUP BY i.variant.product.productId")
    List<Object[]> sumSoldQuantityByProduct(@Param("status") OrderStatus status);

    @Query("SELECT SUM(i.costPrice * i.quantity) FROM OrderItem i WHERE i.order.coupon IS NOT NULL AND i.order.orderStatus != 'CANCELLED'")
    BigDecimal sumTotalCostPriceForVoucherOrders();

    @Query("SELECT SUM(i.costPrice * i.quantity) FROM OrderItem i WHERE i.order.coupon IS NOT NULL AND i.order.shop.shopId = :shopId AND i.order.orderStatus != 'CANCELLED'")
    BigDecimal sumTotalCostPriceForShopVoucherOrders(@Param("shopId") Integer shopId);

    @Query("SELECT i.productName, SUM(i.quantity), SUM(i.price * i.quantity), COUNT(DISTINCT i.order.orderId) " +
            "FROM OrderItem i WHERE i.order.coupon IS NOT NULL AND i.order.orderStatus != 'CANCELLED' " +
            "GROUP BY i.productName ORDER BY SUM(i.quantity) DESC")
    List<Object[]> findTopFruitProductsForVoucherOrders();

    @Query("SELECT i.productName, SUM(i.quantity), SUM(i.price * i.quantity), COUNT(DISTINCT i.order.orderId) " +
            "FROM OrderItem i WHERE i.order.coupon IS NOT NULL AND i.order.shop.shopId = :shopId AND i.order.orderStatus != 'CANCELLED' " +
            "GROUP BY i.productName ORDER BY SUM(i.quantity) DESC")
    List<Object[]> findTopFruitProductsForShopVoucherOrders(@Param("shopId") Integer shopId);
}
