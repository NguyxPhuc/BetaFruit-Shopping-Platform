package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.OrderItem;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrderOrderIdOrderByOrderItemIdAsc(Integer orderId);

    @Query("SELECT i.variant.product.productId, SUM(i.quantity) FROM OrderItem i " +
            "WHERE i.order.orderStatus = :status GROUP BY i.variant.product.productId")
    List<Object[]> sumSoldQuantityByProduct(@Param("status") OrderStatus status);
}
