package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.CustomerOrderResponse;
import com.fu.SWP391_BetaFruit.dto.response.TierUpgradeResult;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.entity.OrderItem;
import com.fu.SWP391_BetaFruit.entity.OrderStatusHistory;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.repository.OrderItemRepository;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.OrderStatusHistoryRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.CustomerOrderService;
import com.fu.SWP391_BetaFruit.service.MembershipTierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final MembershipTierService membershipTierService;

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerOrderResponse> getCustomerOrders(Integer customerId, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = (status == null)
                ? orderRepository.findByCustomer_UserId(customerId, pageable)
                : orderRepository.findByCustomer_UserIdAndOrderStatus(customerId, status, pageable);

        return orderPage.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOrderResponse getOrderDetail(Integer orderId, Integer customerId) {
        Order order = orderRepository.findByOrderIdAndCustomer_UserId(orderId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng #" + orderId));
        return toResponse(order);
    }

    @Override
    @Transactional
    public TierUpgradeResult confirmReceived(Integer orderId, Integer customerId) {
        Order order = orderRepository.findLockedById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng #" + orderId));

        if (!Objects.equals(order.getCustomer().getUserId(), customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xác nhận đơn hàng này");
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng chỉ có thể xác nhận khi ở trạng thái Đã giao hàng");
        }

        order.setOrderStatus(OrderStatus.SUCCESS);
        orderRepository.save(order);

        User customer = userRepository.getReferenceById(customerId);
        orderStatusHistoryRepository.save(new OrderStatusHistory(
                null, order, customer, OrderStatus.DELIVERED, OrderStatus.SUCCESS,
                "Khách hàng đã nhận hàng và xác nhận hài lòng", null
        ));

        // Kích hoạt Loyalty Engine thăng hạng
        TierUpgradeResult upgradeResult = membershipTierService.processOrderCompleted(
                customerId, order.getOrderId(), order.getFinalAmount()
        );

        log.info("Khách hàng #{} đã xác nhận nhận hàng cho đơn #{}. Kết quả thăng hạng: upgraded={}",
                customerId, orderId, upgradeResult.isUpgraded());

        return upgradeResult;
    }

    private CustomerOrderResponse toResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderOrderIdOrderByOrderItemIdAsc(order.getOrderId());
        List<CustomerOrderResponse.OrderItemResponse> itemResponses = items.stream()
                .map(item -> new CustomerOrderResponse.OrderItemResponse(
                        item.getOrderItemId(),
                        item.getSnapshotProductName(),
                        item.getSnapshotVariantName(),
                        item.getQuantity(),
                        item.getPriceSnapshot(),
                        item.getPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();

        boolean canConfirm = (order.getOrderStatus() == OrderStatus.DELIVERED);
        boolean isSuccess = (order.getOrderStatus() == OrderStatus.SUCCESS);

        return CustomerOrderResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(String.format("BF%06d", order.getOrderId()))
                .shopId(order.getShop() != null ? order.getShop().getShopId() : null)
                .shopName(order.getShop() != null ? order.getShop().getShopName() : "Cửa hàng")
                .orderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : "")
                .orderStatusLabel(formatStatusLabel(order.getOrderStatus()))
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "")
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .finalAmount(order.getFinalAmount())
                .snapshotShippingAddress(order.getSnapshotShippingAddress())
                .createdAt(order.getCreatedAt())
                .canConfirmReceived(canConfirm)
                .isSuccess(isSuccess)
                .items(itemResponses)
                .build();
    }

    private String formatStatusLabel(OrderStatus status) {
        if (status == null) return "Chưa xác định";
        return switch (status) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case PREPARING -> "Đang chuẩn bị hàng";
            case READY -> "Sẵn sàng giao";
            case PICKED_UP -> "Shipper đã lấy hàng";
            case DELIVERING -> "Đang giao hàng";
            case DELIVERED -> "Đã giao hàng";
            case SUCCESS -> "Hoàn tất";
            case FAILED -> "Giao thất bại";
            case CANCELLED -> "Đã hủy";
            case REJECTED -> "Từ chối nhận đơn";
        };
    }
}
