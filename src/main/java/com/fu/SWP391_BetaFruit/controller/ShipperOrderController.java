package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.entity.Shipper;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.repository.ShipperRepository;
import com.fu.SWP391_BetaFruit.service.CODCollectionService;
import com.fu.SWP391_BetaFruit.service.SessionUserService;
import com.fu.SWP391_BetaFruit.service.impl.DelivererOrderDetailService;
import com.fu.SWP391_BetaFruit.service.impl.OrderDeliveryService;
import com.fu.SWP391_BetaFruit.service.impl.ShipperOrderQueryService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/shipper/orders")
public class ShipperOrderController {
    private static final Logger log = LoggerFactory.getLogger(ShipperOrderController.class);
    private final ShipperOrderQueryService orders;
    private final SessionUserService sessions;
    private final DelivererOrderDetailService details;
    private final OrderDeliveryService delivery;
    private final ShipperRepository shipperRepository;
    private final CODCollectionService codCollectionService;

    public ShipperOrderController(ShipperOrderQueryService orders, SessionUserService sessions,
            DelivererOrderDetailService details, OrderDeliveryService delivery,
            ShipperRepository shipperRepository, CODCollectionService codCollectionService) {
        this.orders = orders;
        this.sessions = sessions;
        this.details = details;
        this.delivery = delivery;
        this.shipperRepository = shipperRepository;
        this.codCollectionService = codCollectionService;
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
        if (user == null) return "redirect:/auth/login";
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

        BigDecimal codDebt = shipperRepository.findById(user.getUserId())
                .map(Shipper::getCodDebt)
                .orElse(BigDecimal.ZERO);
        if (codDebt == null) codDebt = BigDecimal.ZERO;
        BigDecimal codCeiling = new BigDecimal("3000000");
        boolean isCodLimitReached = codDebt.compareTo(codCeiling) >= 0;

        model.addAttribute("codDebt", codDebt);
        model.addAttribute("codCeiling", codCeiling);
        model.addAttribute("isCodLimitReached", isCodLimitReached);

        return "shipper/orders";
    }

    @GetMapping("/cod-history")
    public String codHistory(HttpSession session, Model model) {
        var user = currentUser(session);
        if (user == null) return "redirect:/auth/login";

        BigDecimal codDebt = shipperRepository.findById(user.getUserId())
                .map(Shipper::getCodDebt)
                .orElse(BigDecimal.ZERO);
        if (codDebt == null) codDebt = BigDecimal.ZERO;
        BigDecimal codCeiling = new BigDecimal("3000000");
        boolean isCodLimitReached = codDebt.compareTo(codCeiling) >= 0;
        BigDecimal remainingQuota = codCeiling.subtract(codDebt).max(BigDecimal.ZERO);

        var pendingAssignments = codCollectionService.getUnremittedAssignmentsByShipper(user.getUserId());
        var pastCollections = codCollectionService.getCollectionsByShipper(user.getUserId());

        model.addAttribute("delivererName", user.getFullName());
        model.addAttribute("delivererAvatar", user.getAvatarUrl());
        model.addAttribute("codDebt", codDebt);
        model.addAttribute("codCeiling", codCeiling);
        model.addAttribute("remainingQuota", remainingQuota);
        model.addAttribute("isCodLimitReached", isCodLimitReached);
        model.addAttribute("pendingAssignments", pendingAssignments);
        model.addAttribute("pastCollections", pastCollections);
        return "shipper/cod-history";
    }

    @GetMapping("/{orderId}")
    public String detail(@PathVariable Integer orderId, HttpSession session, Model model) {
        var user = currentUser(session);
        if (user == null) return "redirect:/auth/login";
        model.addAttribute("detail", details.getDetail(orderId, user.getUserId()));
        model.addAttribute("delivererName", user.getFullName());
        model.addAttribute("delivererAvatar", user.getAvatarUrl());
        return "shipper/order-detail";
    }

    @PostMapping("/{orderId}/picked-up")
    public String pickUp(@PathVariable Integer orderId, HttpSession session, RedirectAttributes redirect) {
        var user = currentUser(session);
        if (user == null) return "redirect:/auth/login";
        Integer shipperId = user.getUserId();
        return change(orderId, () -> delivery.markPickedUp(orderId, shipperId), redirect);
    }

    @PostMapping("/{orderId}/delivering")
    public String delivering(@PathVariable Integer orderId, HttpSession session, RedirectAttributes redirect) {
        var user = currentUser(session);
        if (user == null) return "redirect:/auth/login";
        Integer shipperId = user.getUserId();
        return change(orderId, () -> delivery.startDelivering(orderId, shipperId), redirect);
    }

    @PostMapping("/{orderId}/delivered")
    public String delivered(@PathVariable Integer orderId, HttpSession session, RedirectAttributes redirect,
            @RequestParam(defaultValue = "false") boolean codCollected) {
        var user = currentUser(session);
        if (user == null) return "redirect:/auth/login";
        Integer shipperId = user.getUserId();
        return change(orderId, () -> delivery.markDelivered(orderId, shipperId, codCollected), redirect);
    }

    @PostMapping("/{orderId}/report-incident")
    public String reportIncident(@PathVariable Integer orderId,
            @RequestParam(defaultValue = "Hỏng xe trên đường") String reason,
            @RequestParam(required = false) String note,
            HttpSession session, RedirectAttributes redirect) {
        var user = currentUser(session);
        if (user == null) return "redirect:/auth/login";
        Integer shipperId = user.getUserId();
        return change(orderId, () -> delivery.reportIncident(orderId, shipperId, reason, note), redirect);
    }


    private String change(Integer orderId, Runnable action, RedirectAttributes redirect) {
        try {
            action.run();
            redirect.addFlashAttribute("statusMessage", "Đã cập nhật trạng thái đơn hàng.");
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() != HttpStatus.CONFLICT) throw exception;
            redirect.addFlashAttribute("statusError", exception.getReason());
        }
        return "redirect:/shipper/orders/" + orderId;
    }

    private User currentUser(HttpSession session) {
        return sessions.findCurrentUser(session).orElse(null);
    }
}
