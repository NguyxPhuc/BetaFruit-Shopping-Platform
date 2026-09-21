package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.ShopOrderListItemResponse;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.service.ShopOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ShopOrderServiceImpl implements ShopOrderService {
    private final OrderRepository orderRepository;

    public ShopOrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopOrderListItemResponse> getOrdersForShopOwner(
            Integer ownerId, String keyword, OrderStatus status, int page, int size) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        return orderRepository.searchOrdersForShopOwner(ownerId, normalizedKeyword, status, pageable)
                .map(this::toListItem);
    }

    private ShopOrderListItemResponse toListItem(Order order) {
        return new ShopOrderListItemResponse(
                order.getOrderId(),
                "#DH" + String.format("%04d", order.getOrderId()),
                order.getCustomer().getFullName(),
                order.getCustomer().getAvatarUrl(),
                order.getFinalAmount(),
                order.getOrderStatus() == null ? null : order.getOrderStatus().name(),
                statusLabel(order.getOrderStatus()),
                order.getCreatedAt()
        );
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
