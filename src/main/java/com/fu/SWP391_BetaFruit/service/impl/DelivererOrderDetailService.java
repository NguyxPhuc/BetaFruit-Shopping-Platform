package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.*;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DelivererOrderDetailService {
    private final DeliveryAssignmentRepository assignments;
    private final OrderItemRepository items;
    private final OrderStatusHistoryRepository history;
    private final ShipperOrderQueryService orders;

    public DelivererOrderDetailService(DeliveryAssignmentRepository assignments, OrderItemRepository items,
            OrderStatusHistoryRepository history, ShipperOrderQueryService orders) {
        this.assignments = assignments; this.items = items; this.history = history; this.orders = orders;
    }

    @Transactional(readOnly = true)
    public DelivererOrderDetailResponse getDetail(Integer orderId, Integer shipperId) {
        var assignment = assignments.findByOrderOrderIdAndShipperUserId(orderId, shipperId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn được phân công"));
        var order = assignment.getOrder();
        var summary = orders.getOrder(orderId, shipperId);
        var lines = items.findByOrderOrderIdOrderByOrderItemIdAsc(orderId).stream()
                .map(i -> new DelivererOrderDetailResponse.Item(i.getSnapshotProductName(), i.getSnapshotVariantName(),
                        i.getQuantity(), i.getPriceSnapshot(), OrderPricing.lineTotal(i.getPriceSnapshot(), i.getQuantity())))
                .toList();
        var events = history.findByOrderOrderIdOrderByCreatedAtAscHistoryIdAsc(orderId).stream()
                .map(h -> new DelivererOrderDetailResponse.StatusHistory(ShipperOrderResponse.statusLabel(
                        h.getStatusTo() == null ? null : h.getStatusTo().name()),
                        h.getUpdatedBy() == null ? "Hệ thống" : h.getUpdatedBy().getFullName(), h.getNote(), h.getCreatedAt()))
                .toList();
        boolean editable = !Boolean.TRUE.equals(order.getIsSettled());
        return new DelivererOrderDetailResponse(summary, order.getCreatedAt(), assignment.getAssignedAt(),
                order.getTotalAmount(), order.getDiscountAmount(), order.getShippingFee(), lines, events,
                editable && order.getOrderStatus() == OrderStatus.READY,
                editable && order.getOrderStatus() == OrderStatus.PICKED_UP);
    }

}
