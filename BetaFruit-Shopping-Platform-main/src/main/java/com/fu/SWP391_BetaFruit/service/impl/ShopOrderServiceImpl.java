package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.ShopOrderListItemResponse;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.entity.OrderStatusHistory;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.OrderStatusHistoryRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.ShopOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ShopOrderServiceImpl implements ShopOrderService {
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;

    public ShopOrderServiceImpl(OrderRepository orderRepository,
                                OrderStatusHistoryRepository orderStatusHistoryRepository,
                                UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopOrderListItemResponse> getOrdersForShopOwner(
            Integer ownerId, String keyword, OrderStatus status, String sortBy, String sortDirection, int page, int size) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, buildSort(sortBy, sortDirection));

        return orderRepository.searchOrdersForShopOwner(ownerId, normalizedKeyword, status, pageable)
                .map(this::toListItem);
    }

    private ShopOrderListItemResponse toListItem(Order order) {
        return new ShopOrderListItemResponse(
                order.getOrderId(),
                "#DH" + String.format("%04d", order.getOrderId()),
                order.getCustomer().getFullName(),
                order.getFinalAmount(),
                Boolean.TRUE.equals(order.getIsSettled()) ? order.getFinalAmount() : java.math.BigDecimal.ZERO,
                order.getOrderStatus() == null ? null : order.getOrderStatus().name(),
                statusLabel(order.getOrderStatus()),
                order.getOrderStatus() == OrderStatus.PENDING,
                order.getOrderStatus() == OrderStatus.CONFIRMED,
                order.getOrderStatus() == OrderStatus.PREPARING,
                canShopCancel(order.getOrderStatus()),
                order.getCreatedAt()
        );
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        return switch (sortBy == null ? "" : sortBy) {
            case "orderCode" -> Sort.by(direction, "orderId");
            case "totalAmount" -> Sort.by(direction, "finalAmount");
            // Tiền nhận = FinalAmount khi đơn đã được đối soát, ngược lại là 0đ.
            case "receivedAmount" -> Sort.by(direction, "isSettled").and(Sort.by(direction, "finalAmount"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    @Override
    @Transactional
    public void confirmOrder(Integer orderId, Integer ownerId) {
        changeStatus(orderId, ownerId, OrderStatus.PENDING, OrderStatus.CONFIRMED, "Shop xác nhận đơn hàng");
    }

    @Override
    @Transactional
    public void startPreparingOrder(Integer orderId, Integer ownerId) {
        changeStatus(orderId, ownerId, OrderStatus.CONFIRMED, OrderStatus.PREPARING, "Shop bắt đầu chuẩn bị hàng");
    }

    @Override
    @Transactional
    public void markOrderReady(Integer orderId, Integer ownerId) {
        changeStatus(orderId, ownerId, OrderStatus.PREPARING, OrderStatus.READY, "Shop đã chuẩn bị xong hàng");
    }

    @Override
    @Transactional
    public void cancelOrder(Integer orderId, Integer ownerId) {
        Order order = getOwnedOrder(orderId, ownerId);
        if (!canShopCancel(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể hủy đơn trước khi shipper lấy hàng");
        }
        saveStatusChange(order, ownerId, OrderStatus.CANCELLED, "Shop hủy đơn hàng");
    }

    private void changeStatus(Integer orderId, Integer ownerId, OrderStatus expectedStatus,
                              OrderStatus nextStatus, String note) {
        Order order = getOwnedOrder(orderId, ownerId);
        if (order.getOrderStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trạng thái đơn hàng không hợp lệ cho thao tác này");
        }
        saveStatusChange(order, ownerId, nextStatus, note);
    }

    private Order getOwnedOrder(Integer orderId, Integer ownerId) {
        return orderRepository.findByOrderIdAndShopOwnerUserId(orderId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng của cửa hàng này"));
    }

    private void saveStatusChange(Order order, Integer ownerId, OrderStatus nextStatus, String note) {
        OrderStatus previousStatus = order.getOrderStatus();
        order.setOrderStatus(nextStatus);
        orderStatusHistoryRepository.save(new OrderStatusHistory(
                null, order, userRepository.getReferenceById(ownerId), previousStatus, nextStatus, note, null));
    }

    private boolean canShopCancel(OrderStatus status) {
        return status == OrderStatus.PENDING
                || status == OrderStatus.CONFIRMED
                || status == OrderStatus.PREPARING
                || status == OrderStatus.READY;
    }

    private String statusLabel(OrderStatus status) {
        if (status == null) return "Chưa xác định";
        return switch (status) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case PREPARING -> "Đang chuẩn bị hàng";
            case READY -> "Sẵn sàng giao";
            case PICKED_UP -> "Đã lấy hàng";
            case DELIVERING -> "Đang giao hàng";
            case SUCCESS -> "Giao thành công";
            case FAILED -> "Giao thất bại";
            case CANCELLED -> "Đã hủy";
            case REJECTED -> "Từ chối nhận đơn";
        };
    }
}
