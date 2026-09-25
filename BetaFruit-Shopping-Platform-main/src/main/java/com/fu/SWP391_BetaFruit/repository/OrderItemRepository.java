package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrderOrderIdOrderByOrderItemIdAsc(Integer orderId);
}
