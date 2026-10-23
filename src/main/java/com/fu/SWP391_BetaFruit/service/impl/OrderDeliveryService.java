package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Delivery confirmation must come from the shipper assigned to this order. */
@Service
public class OrderDeliveryService {
    private final OrderRepository orders;
    private final DeliveryAssignmentRepository assignments;
    private final OrderStatusHistoryRepository history;
    private final UserRepository users;

    public OrderDeliveryService(OrderRepository orders, DeliveryAssignmentRepository assignments,
                                OrderStatusHistoryRepository history, UserRepository users) {
        this.orders = orders;
        this.assignments = assignments;
        this.history = history;
        this.users = users;
    }

    @Transactional
    public void markPickedUp(Integer orderId, Integer shipperId) {
        transition(orderId, shipperId, OrderStatus.READY, OrderStatus.PICKED_UP, "Shipper đã lấy hàng");
    }

    @Transactional
    public void startDelivering(Integer orderId, Integer shipperId) {
        transition(orderId, shipperId, OrderStatus.PICKED_UP, OrderStatus.DELIVERING, "Shipper bắt đầu giao hàng");
    }

    private void transition(Integer orderId, Integer shipperId, OrderStatus expected, OrderStatus next, String note) {
        Order order = assignedOrder(orderId, shipperId);
        if (order.getOrderStatus() != expected || Boolean.TRUE.equals(order.getIsSettled())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trạng thái đơn hàng không hợp lệ cho thao tác này");
        }
        order.setOrderStatus(next);
        history.save(new OrderStatusHistory(null, order, users.getReferenceById(shipperId), expected, next, note, null));
    }

    @Transactional
    public void markDelivered(Integer orderId, Integer shipperId, boolean codCollected) {
        Order order = assignedOrder(orderId, shipperId);
        if (order.getOrderStatus() == OrderStatus.DELIVERED) return;
        if (order.getOrderStatus() != OrderStatus.DELIVERING || Boolean.TRUE.equals(order.getIsSettled())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng không ở trạng thái đang giao");
        }
        OrderPaymentRules.codAmount(order);
        if (order.getPaymentMethod() == com.fu.SWP391_BetaFruit.enums.PaymentMethod.COD && !codCollected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vui lòng xác nhận đã thu đủ tiền COD từ khách");
        }
        if (order.getPaymentMethod() == com.fu.SWP391_BetaFruit.enums.PaymentMethod.COD
                && Boolean.TRUE.equals(order.getIsCodRemitted())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn COD chưa giao nhưng đã được ghi nhận nộp tiền");
        }
        // Freeze the amount used by settlement; never recalculate it three days later.
        OrderPricing.captureShopAmounts(order);
        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setIsSettled(false);
        if (order.getPaymentMethod() == com.fu.SWP391_BetaFruit.enums.PaymentMethod.COD) {
            // Confirming a COD delivery also confirms that cash was collected from the customer.
            // The system has not received this cash from the shipper yet.
            order.setIsCodRemitted(false);
        }
        history.save(new OrderStatusHistory(null, order, users.getReferenceById(shipperId),
                OrderStatus.DELIVERING, OrderStatus.DELIVERED, "Shipper xác nhận giao thành công", null));
        // AvailableBalance and SettledAt are deliberately untouched during the holding period.
    }

    private Order assignedOrder(Integer orderId, Integer shipperId) {
        Order order = orders.findLockedById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        assignments.findByOrderOrderId(orderId)
                .filter(a -> java.util.Objects.equals(a.getShipper().getUserId(), shipperId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Chỉ shipper được phân công mới được cập nhật đơn hàng"));
        return order;
    }
}
