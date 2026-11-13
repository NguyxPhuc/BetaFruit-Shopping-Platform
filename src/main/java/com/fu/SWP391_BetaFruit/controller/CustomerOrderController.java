package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.CustomerOrderResponse;
import com.fu.SWP391_BetaFruit.dto.response.TierUpgradeResult;
import com.fu.SWP391_BetaFruit.entity.Order;
import com.fu.SWP391_BetaFruit.entity.OrderStatusHistory;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.enums.PaymentStatus;
import com.fu.SWP391_BetaFruit.repository.OrderRepository;
import com.fu.SWP391_BetaFruit.repository.OrderStatusHistoryRepository;
import com.fu.SWP391_BetaFruit.repository.PaymentTransactionRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.service.CustomerOrderService;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutPaymentService;
import com.fu.SWP391_BetaFruit.service.impl.OrderPaymentRules;
import com.fu.SWP391_BetaFruit.exception.GlobalExceptionHandler.InvalidPagination;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;
    private final CheckoutPaymentService checkoutPayments;

    @GetMapping
    public String listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam MultiValueMap<String, String> paginationParams,
            HttpSession session,
            Model model
    ) {
        Integer customerId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (customerId == null) {
            return "redirect:/auth/login";
        }

        int page = paginationNumber(paginationParams, "page", 0, 0, Integer.MAX_VALUE);
        int size = paginationNumber(paginationParams, "size", 10, 1, 100);
        model.addAttribute("pageSize", size);
        Page<CustomerOrderResponse> ordersPage = customerOrderService.getCustomerOrders(customerId, status, page, size);
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("paymentStates", checkoutPayments.states(customerId,
                ordersPage.getContent().stream().map(CustomerOrderResponse::orderId).toList()));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", ordersPage.getNumber());
        model.addAttribute("totalPages", ordersPage.getTotalPages());

        return "customer/orders";
    }

    @GetMapping("/{orderId}")
    public String orderDetail(
            @PathVariable Integer orderId,
            HttpSession session,
            Model model
    ) {
        Integer customerId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (customerId == null) {
            return "redirect:/auth/login";
        }

        CustomerOrderResponse order = customerOrderService.getOrderDetail(orderId, customerId);
        model.addAttribute("order", order);
        model.addAttribute("paymentState", checkoutPayments.states(customerId, java.util.List.of(orderId)).get(orderId));

        return "customer/order-detail";
    }

    @PostMapping("/{orderId}/pay")
    public String retryVnpay(@PathVariable Integer orderId, HttpSession session,
                             HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Integer customerId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (customerId == null) return "redirect:/auth/login";

        try {
            String baseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                    .fromRequestUri(request).replacePath(request.getContextPath()).replaceQuery(null)
                    .build().toUriString();
            String ipAddress = request.getRemoteAddr();
            if ("::1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress)) ipAddress = "127.0.0.1";
            return "redirect:" + checkoutPayments.start(customerId, orderId, ipAddress, baseUrl);
        } catch (Exception exception) {
            log.warn("Cannot retry VNPay for customerId={}, orderId={}", customerId, orderId, exception);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Chưa thể mở thanh toán VNPay. Đơn hàng vẫn được giữ lại, vui lòng thử lại.");
            return "redirect:/orders/" + orderId;
        }
    }

    @PostMapping("/{orderId}/confirm-received")
    public String confirmReceived(
            @PathVariable Integer orderId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Integer customerId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (customerId == null) {
            return "redirect:/auth/login";
        }

        try {
            TierUpgradeResult upgradeResult = customerOrderService.confirmReceived(orderId, customerId);
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận đã nhận hàng thành công.");

            if (upgradeResult != null && upgradeResult.isUpgraded()) {
                redirectAttributes.addFlashAttribute("upgradeCelebration", true);
                redirectAttributes.addFlashAttribute("newTierName", upgradeResult.getNewTierName());
                redirectAttributes.addFlashAttribute("welcomeCoupon", upgradeResult.getWelcomeCouponCode());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/orders/" + orderId;
    }

    @Slf4j
    @Configuration
    @EnableScheduling
    public static class OrderCompletionScheduler {
        private final OrderRepository orders;
        private final OrderCompletionService completion;
        private final Duration delay;

        // Thời gian chờ tính từ lúc khách xác nhận đã nhận hàng. Đổi 72h thành 10s để demo.
        public OrderCompletionScheduler(OrderRepository orders, OrderCompletionService completion,
                @Value("${orders.completion.delay:72h}") String delay) {
            this.orders = orders;
            this.completion = completion;
            this.delay = DurationStyle.detectAndParse(delay);
            if (this.delay.isNegative() || this.delay.isZero())
                throw new IllegalArgumentException("orders.completion.delay phải lớn hơn 0");
        }

        // Mặc định quét lại sau 1 giây kể từ khi lần quét trước kết thúc.
        @Scheduled(fixedDelayString = "${orders.completion.poll-interval-ms:1000}")
        // Muốn quét đúng đầu mỗi giờ, thay dòng @Scheduled bên trên bằng dòng sau.
        // @Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
        // Thứ tự cron của Spring là giây, phút, giờ, ngày trong tháng, tháng, ngày trong tuần.
        // "0 15 7 * * *"    — 07:15 mỗi ngày
        // "0 45 23 * * *"   — 23:45 mỗi ngày
        // "0 30 8,17 * * *" — 08:30 và 17:30 mỗi ngày
        // "0 30 * * * *"    — Phút 30 của mỗi giờ
        public void completeReceivedOrders() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cutoff = now.minus(delay);
            for (Integer orderId : orders.findCompletionAndSettlementCandidates(cutoff)) {
                try {
                    completion.completeAndSettle(orderId, cutoff, now);
                } catch (Exception ex) {
                    log.error("Không thể hoàn tất/đối soát đơn #{}. Sẽ thử lại ở lần quét tiếp theo.", orderId, ex);
                }
            }
        }
    }

    @Service
    @RequiredArgsConstructor
    public static class OrderCompletionService {
        private final OrderRepository orders;
        private final OrderStatusHistoryRepository histories;
        private final ShopRepository shops;
        private final PaymentTransactionRepository payments;
        private final NotificationService notifications;

        // Spring calls this through a separate bean so each order has its own transaction.
        @Transactional
        public void completeAndSettle(Integer orderId, LocalDateTime cutoff, LocalDateTime now) {
            Order order = orders.findLockedById(orderId).orElse(null);
            if (order == null) return;

            if (order.getOrderStatus() == OrderStatus.RECEIVED) {
                var received = histories.findFirstByOrderOrderIdAndStatusToOrderByCreatedAtDescHistoryIdDesc(
                        orderId, OrderStatus.RECEIVED).orElse(null);
                // Never guess the receipt date for legacy orders without history.
                if (received == null || received.getCreatedAt() == null
                        || received.getCreatedAt().isAfter(cutoff)) return;

                order.setOrderStatus(OrderStatus.SUCCESS);
                histories.save(new OrderStatusHistory(null, order, order.getCustomer(),
                        OrderStatus.RECEIVED, OrderStatus.SUCCESS,
                        "Hệ thống tự động hoàn tất sau thời gian chờ kể từ khi khách xác nhận nhận hàng", null));
                notifications.createBaseNotification(order.getCustomer(),
                        "Đơn hàng #" + orderId + " đã hoàn tất",
                        "Đơn hàng đã kết thúc thời gian chờ sau khi xác nhận nhận hàng.",
                        NotificationType.ORDER_STATUS, "/orders/" + orderId);
            }

            if (order.getOrderStatus() != OrderStatus.SUCCESS
                    || Boolean.TRUE.equals(order.getIsSettled()) || !paymentReceived(order)) return;

            // Use the amount frozen on delivery, never today's commission/coupon settings.
            BigDecimal net = order.getShopNetReceived();
            if (net == null || net.signum() < 0)
                throw new IllegalStateException("Tiền shop thực nhận không hợp lệ cho đơn #" + orderId);
            var shop = shops.findByIdForUpdate(order.getShop().getShopId())
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy cửa hàng của đơn #" + orderId));
            BigDecimal balance = shop.getAvailableBalance() == null ? BigDecimal.ZERO : shop.getAvailableBalance();
            shop.setAvailableBalance(balance.add(net));
            order.setIsSettled(true);
            order.setSettledAt(now);
            shops.save(shop);
            orders.save(order);
            notifications.createBaseNotification(shop.getOwner(),
                    "Đã cộng tiền đơn hàng #" + orderId,
                    "Số tiền " + net.toPlainString() + " đ đã được cộng vào số dư khả dụng của cửa hàng.",
                    NotificationType.PAYMENT_UPDATE, "/shop/balance");
            // Loyalty is already processed by confirmReceived(); do not credit it again here.
        }

        private boolean paymentReceived(Order order) {
            if (order.getPaymentMethod() == PaymentMethod.COD)
                return Boolean.TRUE.equals(order.getIsCodRemitted()) || OrderPaymentRules.codAmount(order).signum() == 0;
            if (order.getPaymentMethod() == PaymentMethod.VNPAY)
                return payments.findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc(order.getOrderId()).stream()
                        .anyMatch(p -> p.getStatus() == PaymentStatus.PAID && "VNPAY".equalsIgnoreCase(p.getGateway()));
            return false;
        }
    }

    private static int paginationNumber(MultiValueMap<String, String> params, String key, int fallback, int min, int max) {
        var values = params.get(key);
        if (values == null) return fallback;
        if (values.size() != 1) throw new InvalidPagination("Tham số " + key + " chỉ được xuất hiện một lần.");
        String raw = values.getFirst();
        if (raw == null || !raw.matches("[0-9]+")) throw invalid(key, min, max);
        try {
            int value = Integer.parseInt(raw);
            if (value < min || value > max) throw invalid(key, min, max);
            return value;
        } catch (NumberFormatException ex) {
            throw invalid(key, min, max);
        }
    }

    private static InvalidPagination invalid(String key, int min, int max) {
        return new InvalidPagination("Tham số " + key + " phải là số nguyên từ " + min + " đến " + max + ". Hãy sửa giá trị đã nhập.");
    }
}
