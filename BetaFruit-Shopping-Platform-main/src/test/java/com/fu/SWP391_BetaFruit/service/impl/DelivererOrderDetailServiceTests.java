package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelivererOrderDetailServiceTests {
    final DeliveryAssignmentRepository assignments = mock(DeliveryAssignmentRepository.class);
    final OrderItemRepository items = mock(OrderItemRepository.class);
    final OrderStatusHistoryRepository history = mock(OrderStatusHistoryRepository.class);
    final ShipperOrderQueryService orders = mock(ShipperOrderQueryService.class);
    final DelivererOrderDetailService service = new DelivererOrderDetailService(assignments, items, history, orders);

    @Test void unassignedOrderDoesNotReadItemsOrHistory() {
        assertThrows(ResponseStatusException.class, () -> service.getDetail(12, 3));
        verifyNoInteractions(items, history, orders);
    }

    @Test void snapshotItemsAndActionFlagsFollowCurrentState() {
        Order order = new Order(); order.setOrderStatus(OrderStatus.READY);
        order.setTotalAmount(new BigDecimal("100000"));
        DeliveryAssignment assignment = new DeliveryAssignment(); assignment.setOrder(order);
        when(assignments.findByOrderOrderIdAndShipperUserId(12, 3)).thenReturn(Optional.of(assignment));
        OrderItem item = new OrderItem(); item.setSnapshotProductName("Táo"); item.setSnapshotVariantName("Hộp");
        item.setQuantity(2); item.setPriceSnapshot(new BigDecimal("50000"));
        when(items.findByOrderOrderIdOrderByOrderItemIdAsc(12)).thenReturn(List.of(item));
        var detail = service.getDetail(12, 3);
        assertTrue(detail.canPickUp()); assertFalse(detail.canStartDelivering());
        assertEquals(new BigDecimal("100000"), detail.items().getFirst().lineTotal());
        order.setOrderStatus(OrderStatus.PICKED_UP);
        detail = service.getDetail(12, 3);
        assertFalse(detail.canPickUp()); assertTrue(detail.canStartDelivering());
        order.setIsSettled(true);
        assertFalse(service.getDetail(12, 3).canStartDelivering());
    }
}
