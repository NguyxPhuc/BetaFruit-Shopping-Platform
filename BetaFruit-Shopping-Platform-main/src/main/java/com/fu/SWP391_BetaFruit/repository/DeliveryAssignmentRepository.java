package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.DeliveryAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Integer> {
    @EntityGraph(attributePaths = "shipper.user")
    Optional<DeliveryAssignment> findByOrderOrderId(Integer orderId);
}
