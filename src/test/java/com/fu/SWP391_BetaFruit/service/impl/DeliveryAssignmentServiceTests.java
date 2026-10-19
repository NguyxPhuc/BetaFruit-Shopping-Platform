package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeliveryAssignmentServiceTests {
    final OrderRepository orders = mock(OrderRepository.class);
    final DeliveryAssignmentRepository assignments = mock(DeliveryAssignmentRepository.class);
    final ShipperRepository shippers = mock(ShipperRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final DeliveryAssignmentService service = new DeliveryAssignmentService(orders, assignments, shippers, users);

    @Test void onlyReadyOrdersCanBeAssigned() {
        Order order = new Order();
        when(orders.findLockedById(12)).thenReturn(Optional.of(order));
        for (OrderStatus status : OrderStatus.values()) {
            if (status == OrderStatus.READY) continue;
            order.setOrderStatus(status);
            assertThrows(ResponseStatusException.class, () -> service.assign(12, 3, 7));
        }
        verifyNoInteractions(assignments, shippers, users);
        order.setOrderStatus(OrderStatus.READY);
        Shipper shipper = new Shipper(); shipper.setUserId(3);
        User actor = new User(); actor.setUserId(7);
        when(shippers.findById(3)).thenReturn(Optional.of(shipper));
        when(users.findById(7)).thenReturn(Optional.of(actor));
        when(assignments.save(any())).thenAnswer(i -> i.getArgument(0));
        var saved = service.assign(12, 3, 7);
        assertSame(order, saved.getOrder());
        assertSame(shipper, saved.getShipper());
        assertSame(actor, saved.getAssignedBy());
        assertEquals(OrderStatus.READY, order.getOrderStatus());
    }

    @Test void existingAssignmentCannotBeOverwritten() {
        Order order = new Order(); order.setOrderStatus(OrderStatus.READY);
        when(orders.findLockedById(12)).thenReturn(Optional.of(order));
        when(assignments.findByOrderOrderId(12)).thenReturn(Optional.of(new DeliveryAssignment()));
        assertThrows(ResponseStatusException.class, () -> service.assign(12, 3, 7));
        verify(assignments, never()).save(any());
        verifyNoInteractions(shippers, users);
    }
}
