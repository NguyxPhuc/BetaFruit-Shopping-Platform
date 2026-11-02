package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.DeliveryAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;

import java.util.Collection;
import java.util.Optional;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Integer> {
    @EntityGraph(attributePaths = "shipper.user")
    Optional<DeliveryAssignment> findByOrderOrderId(Integer orderId);

    @EntityGraph(attributePaths = {"order.customer", "order.shop"})
    @Query("""
            select a from DeliveryAssignment a
            where a.shipper.userId = :shipperId
              and (:keyword is null or str(a.order.orderId) like concat('%', :keyword, '%')
                   or lower(a.order.customer.fullName) like lower(concat('%', :keyword, '%')))
              and (:status is null or a.order.orderStatus = :status)
              and (:paymentMethod is null or a.order.paymentMethod = :paymentMethod)
            """)
    Page<DeliveryAssignment> searchAssignedOrders(@Param("shipperId") Integer shipperId,
            @Param("keyword") String keyword, @Param("status") OrderStatus status,
            @Param("paymentMethod") PaymentMethod paymentMethod, Pageable pageable);

    @EntityGraph(attributePaths = {"order.customer", "order.shop"})
    Optional<DeliveryAssignment> findByOrderOrderIdAndShipperUserId(Integer orderId, Integer shipperId);

    @Query("""
            select count(a) from DeliveryAssignment a
            where a.shipper.userId = :shipperId
              and a.order.orderStatus in :ongoingStatuses
            """)
    long countOngoingOrdersByShipper(@Param("shipperId") Integer shipperId,
            @Param("ongoingStatuses") Collection<OrderStatus> ongoingStatuses);
}
