package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.service.SessionUserService;
import com.fu.SWP391_BetaFruit.service.impl.OrderDeliveryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/shipper/orders")
public class OrderDeliveryController {
    private final OrderDeliveryService delivery;
    private final SessionUserService sessions;

    public OrderDeliveryController(OrderDeliveryService delivery, SessionUserService sessions) {
        this.delivery = delivery;
        this.sessions = sessions;
    }

    @PostMapping("/{orderId}/delivered")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markDelivered(@PathVariable Integer orderId, HttpSession session,
            @RequestParam(defaultValue = "false") boolean codCollected) {
        var user = sessions.findCurrentUser(session).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập"));
        delivery.markDelivered(orderId, user.getUserId(), codCollected);
    }

    @PostMapping("/{orderId}/picked-up")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markPickedUp(@PathVariable Integer orderId, HttpSession session) {
        var user = sessions.findCurrentUser(session).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập"));
        delivery.markPickedUp(orderId, user.getUserId());
    }

    @PostMapping("/{orderId}/delivering")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startDelivering(@PathVariable Integer orderId, HttpSession session) {
        var user = sessions.findCurrentUser(session).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập"));
        delivery.startDelivering(orderId, user.getUserId());
    }
}
