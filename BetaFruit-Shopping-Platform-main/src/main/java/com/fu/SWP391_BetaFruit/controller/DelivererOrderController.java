package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.service.SessionUserService;
import com.fu.SWP391_BetaFruit.service.impl.ShipperOrderQueryService;
import com.fu.SWP391_BetaFruit.service.impl.DelivererOrderDetailService;
import com.fu.SWP391_BetaFruit.service.impl.OrderDeliveryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/deliverer/orders")
public class DelivererOrderController {
    private static final Logger log = LoggerFactory.getLogger(DelivererOrderController.class);
    // Temporary preview identity while login is unavailable. Remove when login is connected.
    private static final Integer TEST_DELIVERER_ID = 3;
    private final ShipperOrderQueryService orders;
    private final SessionUserService sessions;
    private final DelivererOrderDetailService details;
    private final OrderDeliveryService delivery;

    public DelivererOrderController(ShipperOrderQueryService orders, SessionUserService sessions,
            DelivererOrderDetailService details, OrderDeliveryService delivery) {
        this.orders = orders;
        this.sessions = sessions;
        this.details = details;
        this.delivery = delivery;
    }

    @GetMapping
    public String list(HttpSession session, Model model,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(defaultValue = "assignedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var user = currentUser(session);
        String safeSort = switch (sortBy) {
            case "orderCode", "totalAmount" -> sortBy;
            default -> "assignedAt";
        };
        String safeDirection = "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";
        try {
            model.addAttribute("orders", orders.searchOrders(user.getUserId(), keyword, status,
                    paymentMethod, safeSort, safeDirection, page, size));
        } catch (DataAccessException | CannotCreateTransactionException exception) {
            log.error("Cannot load assigned orders for shipper {}", user.getUserId(), exception);
            model.addAttribute("orders", Page.empty());
            model.addAttribute("orderLoadError", "Không thể tải đơn giao hàng. Vui lòng thử lại sau.");
        }
        model.addAttribute("delivererName", user.getFullName());
        model.addAttribute("delivererAvatar", user.getAvatarUrl());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPaymentMethod", paymentMethod);
        model.addAttribute("sortBy", safeSort);
        model.addAttribute("sortDirection", safeDirection);
        return "deliverer/orders";
    }

    @GetMapping("/{orderId}")
    public String detail(@PathVariable Integer orderId, HttpSession session, Model model) {
        var user = currentUser(session);
        model.addAttribute("detail", details.getDetail(orderId, user.getUserId()));
        model.addAttribute("delivererName", user.getFullName());
        model.addAttribute("delivererAvatar", user.getAvatarUrl());
        return "deliverer/order-detail";
    }

    @PostMapping("/{orderId}/picked-up")
    public String pickUp(@PathVariable Integer orderId, HttpSession session, RedirectAttributes redirect) {
        Integer shipperId = currentUser(session).getUserId();
        return change(orderId, () -> delivery.markPickedUp(orderId, shipperId), redirect);
    }

    @PostMapping("/{orderId}/delivering")
    public String delivering(@PathVariable Integer orderId, HttpSession session, RedirectAttributes redirect) {
        Integer shipperId = currentUser(session).getUserId();
        return change(orderId, () -> delivery.startDelivering(orderId, shipperId), redirect);
    }

    @PostMapping("/{orderId}/delivered")
    public String delivered(@PathVariable Integer orderId, HttpSession session, RedirectAttributes redirect,
            @RequestParam(defaultValue = "false") boolean codCollected) {
        Integer shipperId = currentUser(session).getUserId();
        return change(orderId, () -> delivery.markDelivered(orderId, shipperId, codCollected), redirect);
    }

    private String change(Integer orderId, Runnable action, RedirectAttributes redirect) {
        try {
            action.run();
            redirect.addFlashAttribute("statusMessage", "Đã cập nhật trạng thái đơn hàng.");
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() != HttpStatus.CONFLICT) throw exception;
            redirect.addFlashAttribute("statusError", exception.getReason());
        }
        return "redirect:/deliverer/orders/" + orderId;
    }

    private User currentUser(HttpSession session) {
        return sessions.findCurrentUser(session).orElseGet(() -> {
            User testUser = new User();
            testUser.setUserId(TEST_DELIVERER_ID);
            testUser.setFullName("Nhân viên giao hàng #" + TEST_DELIVERER_ID);
            return testUser;
        });
    }
}
