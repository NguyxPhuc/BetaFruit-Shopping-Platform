package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;

/** Cash collection, COD remittance and shop settlement are separate facts. */
public final class OrderPaymentRules {
    private OrderPaymentRules() {}

    /**
     * Tác dụng: Trả tổng tiền khách phải trả nếu là COD, trả 0 nếu thanh toán online; báo lỗi nếu thiếu
     * phương thức thanh toán.
     * Màn hình / luồng: Dữ liệu COD của danh sách/chi tiết đơn shipper, luồng giao thành công, thu nộp COD
     * và đối soát đơn.
     */
    public static BigDecimal codAmount(Order order) {
        if (order.getPaymentMethod() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng thiếu phương thức thanh toán");
        }
        if (order.getPaymentMethod() != PaymentMethod.COD) return BigDecimal.ZERO;
        return OrderPricing.customerAmount(order);
    }

    /**
     * Tác dụng: Nhận biết đơn đã giao qua các trạng thái DELIVERED/RECEIVED/SUCCESS; remittanceStatus và
     * codHeldAmount dùng để xác định tiền đã thu.
     * Màn hình / luồng: Dữ liệu COD của danh sách/chi tiết đơn shipper, luồng giao thành công, thu nộp COD
     * và đối soát đơn.
     */
    public static boolean delivered(Order order) {
        return order.getOrderStatus() == OrderStatus.DELIVERED
                || order.getOrderStatus() == OrderStatus.RECEIVED
                || order.getOrderStatus() == OrderStatus.SUCCESS;
    }

    /**
     * Tác dụng: Nhận biết đơn còn ở READY/PICKED_UP/DELIVERING; amountToCollect dùng để xác định COD còn phải thu.
     * Màn hình / luồng: Dữ liệu COD của danh sách/chi tiết đơn shipper, luồng giao thành công, thu nộp COD
     * và đối soát đơn.
     */
    public static boolean awaitingDelivery(Order order) {
        return order.getOrderStatus() == OrderStatus.READY
                || order.getOrderStatus() == OrderStatus.PICKED_UP
                || order.getOrderStatus() == OrderStatus.DELIVERING;
    }

    /**
     * Tác dụng: Trả mã trạng thái nộp COD: không áp dụng, đã nộp, chờ nộp hoặc chưa thu;
     * ShipperOrderQueryService dùng cho UI.
     * Màn hình / luồng: Dữ liệu COD của danh sách/chi tiết đơn shipper, luồng giao thành công, thu nộp COD
     * và đối soát đơn.
     */
    public static String remittanceStatus(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.COD) return "NOT_APPLICABLE";
        if (Boolean.TRUE.equals(order.getIsCodRemitted())) return "REMITTED";
        if (delivered(order)) return "PENDING_REMITTANCE";
        return "NOT_COLLECTED";
    }

    /**
     * Tác dụng: Tính COD còn phải thu cho đơn đang chờ/đang giao và chưa nộp tiền; ShipperOrderQueryService
     * dùng khi dựng DTO.
     * Màn hình / luồng: Dữ liệu COD của danh sách/chi tiết đơn shipper, luồng giao thành công, thu nộp COD
     * và đối soát đơn.
     */
    public static BigDecimal amountToCollect(Order order) {
        BigDecimal amount = codAmount(order);
        return awaitingDelivery(order) && !Boolean.TRUE.equals(order.getIsCodRemitted())
                ? amount : BigDecimal.ZERO;
    }

    /**
     * Tác dụng: Tính COD shipper đang giữ khi đơn đã giao nhưng chưa nộp; ShipperOrderQueryService dùng khi
     * dựng DTO.
     * Màn hình / luồng: Dữ liệu COD của danh sách/chi tiết đơn shipper, luồng giao thành công, thu nộp COD
     * và đối soát đơn.
     */
    public static BigDecimal codHeldAmount(Order order) {
        BigDecimal amount = codAmount(order);
        return delivered(order) && !Boolean.TRUE.equals(order.getIsCodRemitted())
                ? amount : BigDecimal.ZERO;
    }
}
