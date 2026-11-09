package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.OrderStatusHistory;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Integer> {
    Optional<OrderStatusHistory> findFirstByOrderOrderIdAndStatusToOrderByCreatedAtDescHistoryIdDesc(
            Integer orderId, OrderStatus statusTo);
    @EntityGraph(attributePaths = "updatedBy")
    List<OrderStatusHistory> findByOrderOrderIdOrderByCreatedAtAscHistoryIdAsc(Integer orderId);
}
