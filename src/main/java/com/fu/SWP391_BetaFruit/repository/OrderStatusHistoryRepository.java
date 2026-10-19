package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Integer> {
    @EntityGraph(attributePaths = "updatedBy")
    List<OrderStatusHistory> findByOrderOrderIdOrderByCreatedAtAscHistoryIdAsc(Integer orderId);
}
