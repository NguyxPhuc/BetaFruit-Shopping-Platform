package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.ShopOrderListItemResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopOrderDetailResponse;
import com.fu.SWP391_BetaFruit.repository.OrderItemRepository;
import com.fu.SWP391_BetaFruit.repository.DeliveryAssignmentRepository;
import com.fu.SWP391_BetaFruit.repository.PaymentTransactionRepository;
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
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ShopOrderServiceImpl implements ShopOrderService {
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderStockService orderStockService;

    public ShopOrderServiceImpl(OrderRepository orderRepository,
                                OrderStatusHistoryRepository orderStatusHistoryRepository,
                                UserRepository userRepository,
                                OrderItemRepository orderItemRepository,
                                DeliveryAssignmentRepository deliveryAssignmentRepository,
                                PaymentTransactionRepository paymentTransactionRepository,
                                OrderStockService orderStockService) {
        this.orderRepository = orderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.orderStockService = orderStockService;
    }

    @Override
    @Transactional(readOnly = true)
    public ShopOrderDetailResponse getOrderDetailForShopOwner(Integer orderId, Integer ownerId) {
        Order order = orderRepository.findDetailForShopOwner(orderId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng của cửa hàng này"));

        var items = orderItemRepository.findByOrderOrderIdOrderByOrderItemIdAsc(orderId).stream()
                .map(item -> new ShopOrderDetailResponse.Item(
                        item.getOrderItemId(), item.getSnapshotProductName(), item.getSnapshotVariantName(),
                        item.getQuantity(), item.getPriceSnapshot(),
                        item.getPriceSnapshot().multiply(java.math.BigDecimal.valueOf(item.getQuantity())),
                        item.getCostSnapshot()))
                .toList();
        var history = orderStatusHistoryRepository.findByOrderOrderIdOrderByCreatedAtAscHistoryIdAsc(orderId).stream()
                .map(entry -> new ShopOrderDetailResponse.StatusHistory(
                        entry.getHistoryId(), enumName(entry.getStatusFrom()), statusLabel(entry.getStatusFrom()),
                        enumName(entry.getStatusTo()), statusLabel(entry.getStatusTo()),
                        entry.getNote(), entry.getUpdatedBy().getFullName(), entry.getCreatedAt()))
                .toList();
        var delivery = deliveryAssignmentRepository.findByOrderOrderId(orderId)
                .map(assignment -> new ShopOrderDetailResponse.Delivery(
                        assignment.getAssignmentId(), assignment.getShipper().getUserId(),
                        assignment.getShipper().getUser().getFullName(),
                        assignment.getShipper().getUser().getPhone(), assignment.getAssignedAt()))
                .orElse(null);
        var payments = paymentTransactionRepository.findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(orderId).stream()
                .map(payment -> new ShopOrderDetailResponse.Payment(
                        payment.getTransactionId(), payment.getGateway(), payment.getTransactionRef(),
                        payment.getAmount(), enumName(payment.getStatus()), payment.getCreatedAt()))
                .toList();

        // Historical cost belongs to the order item, not the current variant.
        // Missing lines/costs must not turn into a misleading zero-cost profit.
        BigDecimal totalCost = items.isEmpty() || items.stream().anyMatch(item -> item.costSnapshot() == null)
                ? null
                : items.stream()
                        .map(item -> item.costSnapshot().multiply(BigDecimal.valueOf(item.quantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal estimatedProfit = totalCost == null || order.getShopNetReceived() == null
                ? null : order.getShopNetReceived().subtract(totalCost);

        return new ShopOrderDetailResponse(
                toListItem(order), order.getShop().getShopId(), order.getShop().getShopName(),
                order.getCustomer().getUserId(), order.getCustomer().getPhone(),
                order.getSnapshotShippingAddress(), order.getTotalAmount(), order.getDiscountAmount(),
                order.getShippingFee(), enumName(order.getPaymentMethod()), order.getIsSettled(),
                order.getSettledAt(), order.getCoupon() == null ? null : order.getCoupon().getCouponCode(),
                items, history, delivery, payments, order.getPlatformFee(), order.getIsCODRemitted(),
                totalCost, estimatedProfit);
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
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
                String.valueOf(order.getOrderId()),
                order.getCustomer().getFullName(),
                order.getFinalAmount(),
                order.getShopNetReceived(),
                order.getShopNetReceived(),
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
            case "receivedAmount", "shopNetReceived" -> Sort.by(direction, "shopNetReceived");
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
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            orderStockService.restore(order, ownerId);
        }
        saveStatusChange(order, ownerId, OrderStatus.CANCELLED, "Shop hủy đơn hàng");
    }

    private void changeStatus(Integer orderId, Integer ownerId, OrderStatus expectedStatus,
                              OrderStatus nextStatus, String note) {
        Order order = getOwnedOrder(orderId, ownerId);
        if (order.getOrderStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trạng thái đơn hàng không hợp lệ cho thao tác này");
        }
        if (expectedStatus == OrderStatus.PENDING && nextStatus == OrderStatus.CONFIRMED) {
            orderStockService.deduct(order, ownerId);
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
