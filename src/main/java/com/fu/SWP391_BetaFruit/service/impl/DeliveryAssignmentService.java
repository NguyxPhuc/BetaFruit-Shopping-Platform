package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.DeliveryAssignment;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Entry point for a future authorized dispatcher UI, not a shipper self-assignment API. */
@Service
public class DeliveryAssignmentService {
    private final OrderRepository orders;
    private final DeliveryAssignmentRepository assignments;
    private final ShipperRepository shippers;
    private final UserRepository users;

    public DeliveryAssignmentService(OrderRepository orders, DeliveryAssignmentRepository assignments,
            ShipperRepository shippers, UserRepository users) {
        this.orders = orders; this.assignments = assignments; this.shippers = shippers; this.users = users;
    }

    @Transactional
    public DeliveryAssignment assign(Integer orderId, Integer shipperId, Integer assignedById) {
        var order = orders.findLockedById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        if (order.getOrderStatus() != OrderStatus.READY || Boolean.TRUE.equals(order.getIsSettled())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ được phân công đơn sẵn sàng giao");
        }
        if (assignments.findByOrderOrderId(orderId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng đã được phân công");
        }
        var shipper = shippers.findById(shipperId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên giao hàng"));
        var actor = users.findById(assignedById).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người phân công"));
        return assignments.save(new DeliveryAssignment(null, order, shipper, actor, null));
    }
}
