package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.*;
import com.fu.SWP391_BetaFruit.service.SessionUserService;
import com.fu.SWP391_BetaFruit.service.impl.ShipperOrderQueryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/shipper/orders")
public class ShipperOrderQueryController {
    private final ShipperOrderQueryService orders;
    private final SessionUserService sessions;

    public ShipperOrderQueryController(ShipperOrderQueryService orders, SessionUserService sessions) {
        this.orders = orders;
        this.sessions = sessions;
    }

    @GetMapping
    public Page<ShipperOrderResponse> list(HttpSession session,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return orders.getOrders(currentUserId(session), page, size);
    }

    @GetMapping("/{orderId}")
    public ShipperOrderResponse detail(@PathVariable Integer orderId, HttpSession session) {
        return orders.getOrder(orderId, currentUserId(session));
    }

    @GetMapping("/cod-summary")
    public ShipperCODSummaryResponse codSummary(HttpSession session) {
        return orders.getCODSummary(currentUserId(session));
    }

    private Integer currentUserId(HttpSession session) {
        return sessions.findCurrentUser(session).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập")).getUserId();
    }
}
