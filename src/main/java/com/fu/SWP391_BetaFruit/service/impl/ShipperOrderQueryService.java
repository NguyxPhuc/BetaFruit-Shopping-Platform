package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.*;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.repository.DeliveryAssignmentRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ShipperOrderQueryService {
    private final DeliveryAssignmentRepository assignments;
    public ShipperOrderQueryService(DeliveryAssignmentRepository assignments) {
        this.assignments = assignments;
    }

    /**
     * Tác dụng: Tìm đơn được phân công cho shipper theo từ khóa, trạng thái, phương thức thanh toán; sắp
     * xếp, phân trang và chuyển sang DTO.
     * Màn hình / luồng: Màn shipper/orders và shipper/order-detail; ShipperOrderController và
     * DelivererOrderDetailService gọi.
     */
    public Page<ShipperOrderResponse> searchOrders(Integer shipperId, String keyword, OrderStatus status,
            PaymentMethod paymentMethod, String sortBy, String sortDirection, int page, int size) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property = switch (sortBy == null ? "" : sortBy) {
            case "orderCode" -> "order.orderId";
            case "totalAmount" -> "order.finalAmount";
            default -> "assignedAt";
        };
        String normalizedKeyword = org.springframework.util.StringUtils.hasText(keyword) ? keyword.trim() : null;
        return loadPage(page, size, index -> assignments.searchAssignedOrders(shipperId, normalizedKeyword, status, paymentMethod,
                PageRequest.of(index, Math.min(100, Math.max(1, size)),
                        Sort.by(direction, property).and(Sort.by(Sort.Direction.DESC, "assignmentId")))))
                .map(a -> response(a.getOrder()));
    }

    /**
     * Tác dụng: Lấy thông tin tóm tắt một đơn đúng shipper được phân công;
     * DelivererOrderDetailService.getDetail dùng khi mở chi tiết.
     * Màn hình / luồng: Màn shipper/orders và shipper/order-detail; ShipperOrderController và
     * DelivererOrderDetailService gọi.
     */
    public ShipperOrderResponse getOrder(Integer orderId, Integer shipperId) {
        return assignments.findByOrderOrderIdAndShipperUserId(orderId, shipperId)
                .map(a -> response(a.getOrder())).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn được phân công"));
    }

    /**
     * Tác dụng: Chuyển đơn thành DTO gồm tổng tiền, COD phải thu/đang giữ, trạng thái nộp tiền và quyền xác
     * nhận giao; dùng chung cho danh sách và chi tiết.
     * Màn hình / luồng: Màn shipper/orders và shipper/order-detail; ShipperOrderController và
     * DelivererOrderDetailService gọi.
     */
    private ShipperOrderResponse response(Order order) {
        return new ShipperOrderResponse(order.getOrderId(), order.getOrderStatus().name(),
                order.getCustomer().getFullName(), order.getCustomer().getPhone(),
                order.getSnapshotShippingAddress(), order.getShop().getShopName(),
                order.getPaymentMethod() == null ? null : order.getPaymentMethod().name(), OrderPricing.customerAmount(order),
                OrderPaymentRules.codAmount(order), OrderPaymentRules.amountToCollect(order),
                OrderPaymentRules.codHeldAmount(order), OrderPaymentRules.remittanceStatus(order),
                order.getPaymentMethod() == PaymentMethod.COD ? order.getIsCodRemitted() : null,
                order.getIsSettled(), order.getSettledAt(), order.getOrderStatus() == OrderStatus.DELIVERING
                        && !Boolean.TRUE.equals(order.getIsSettled()));
    }

    /**
     * Tác dụng: Tải trang danh sách đơn được phân công và tự sửa chỉ số trang vượt giới hạn; searchOrders gọi.
     * Màn hình / luồng: Màn shipper/orders và shipper/order-detail; ShipperOrderController và
     * DelivererOrderDetailService gọi.
     *
     * Ghi chú hiện có:
     * Query a bounded page; recover when a URL or concurrent deletion leaves it outside the result.
     */
    private <T> Page<T> loadPage(int page, int size, java.util.function.IntFunction<Page<T>> query) {
        int index = Math.max(0, page);
        // Keep the SQL offset within the range accepted by JPA providers.
        Page<T> result = query.apply(index > Integer.MAX_VALUE / Math.min(100, Math.max(1, size)) ? 0 : index);
        int last = Math.max(0, result.getTotalPages() - 1);
        int target = Math.min(index, last);
        if (result.getNumber() != target) result = query.apply(target);
        if (result.isEmpty() && result.getNumber() > 0) result = query.apply(0);
        return result;
    }
}
