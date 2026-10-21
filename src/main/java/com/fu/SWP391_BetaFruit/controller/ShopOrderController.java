package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.service.ShopOrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping({"/shop/orders", "/shop/orders.html"})
public class ShopOrderController {
    private static final Logger log = LoggerFactory.getLogger(ShopOrderController.class);

    private final ShopOrderService shopOrderService;

    public ShopOrderController(ShopOrderService shopOrderService) {
        this.shopOrderService = shopOrderService;
    }

    @GetMapping
    public String orderList(
            Model model,
            HttpSession session,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Integer shopOwnerId = currentUserId(session);
        if (shopOwnerId == null) return "redirect:/auth/login";
        try {
            model.addAttribute("orders", shopOrderService.getOrdersForShopOwner(shopOwnerId, keyword, status, sortBy, sortDirection, page, size));
        } catch (ResponseStatusException exception) {
            log.warn("Order data validation failed for ownerId={}", shopOwnerId, exception);
            model.addAttribute("orders", Page.empty());
            model.addAttribute("orderLoadError", "Dữ liệu đơn hàng không hợp lệ: " + exception.getReason());
        } catch (DataAccessResourceFailureException | CannotCreateTransactionException exception) {
            log.error("Cannot access order database for ownerId={}", shopOwnerId, exception);
            model.addAttribute("orders", Page.empty());
            model.addAttribute("orderLoadError", "Không thể kết nối cơ sở dữ liệu để tải đơn hàng. Kiểm tra lại SQL Server rồi tải lại trang.");
        } catch (DataAccessException exception) {
            log.error("Order query failed for ownerId={}", shopOwnerId, exception);
            model.addAttribute("orders", Page.empty());
            model.addAttribute("orderLoadError", "Không thể truy vấn dữ liệu đơn hàng. Xem log ứng dụng để biết nguyên nhân.");
        } catch (Exception exception) {
            log.error("Order loading failed for ownerId={}", shopOwnerId, exception);
            model.addAttribute("orders", Page.empty());
            model.addAttribute("orderLoadError", "Có lỗi khi xử lý đơn hàng. Xem log ứng dụng để biết nguyên nhân.");
        }
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDirection", sortDirection);
        addOwnerName(session, model);
        return "shop/orders";
    }

    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Integer orderId,
                              HttpSession session,
                              Model model) {
        Integer ownerId = currentUserId(session);
        if (ownerId == null) return "redirect:/auth/login";
        model.addAttribute("detail", shopOrderService.getOrderDetailForShopOwner(orderId, ownerId));
        addOwnerName(session, model);
        return "shop/order-detail";
    }
    @PostMapping("/{orderId}/confirm")
    public String confirmOrder(@PathVariable Integer orderId,
                               HttpSession session,
                               @RequestParam(defaultValue = "false") boolean returnToDetail,
                               RedirectAttributes redirectAttributes) {
        Integer ownerId = currentUserId(session);
        if (ownerId == null) return "redirect:/auth/login";
        try {
            shopOrderService.confirmOrder(orderId, ownerId);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() != HttpStatus.CONFLICT.value()) throw exception;
            redirectAttributes.addFlashAttribute("orderActionError", exception.getReason());
        }
        return "redirect:/shop/orders" + (returnToDetail ? "/" + orderId : "");
    }

    @PostMapping("/{orderId}/preparing")
    public String startPreparingOrder(@PathVariable Integer orderId,
                                      HttpSession session,
                               @RequestParam(defaultValue = "false") boolean returnToDetail) {
        Integer ownerId = currentUserId(session);
        if (ownerId == null) return "redirect:/auth/login";
        shopOrderService.startPreparingOrder(orderId, ownerId);
        return "redirect:/shop/orders" + (returnToDetail ? "/" + orderId : "");
    }

    @PostMapping("/{orderId}/ready")
    public String markOrderReady(@PathVariable Integer orderId,
                                 HttpSession session,
                               @RequestParam(defaultValue = "false") boolean returnToDetail) {
        Integer ownerId = currentUserId(session);
        if (ownerId == null) return "redirect:/auth/login";
        shopOrderService.markOrderReady(orderId, ownerId);
        return "redirect:/shop/orders" + (returnToDetail ? "/" + orderId : "");
    }

    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable Integer orderId,
                              HttpSession session,
                               @RequestParam(defaultValue = "false") boolean returnToDetail) {
        Integer ownerId = currentUserId(session);
        if (ownerId == null) return "redirect:/auth/login";
        shopOrderService.cancelOrder(orderId, ownerId);
        return "redirect:/shop/orders" + (returnToDetail ? "/" + orderId : "");
    }

    private Integer currentUserId(HttpSession session) {
        return session != null && session.getAttribute("LOGGED_IN_USER_ID") instanceof Integer id ? id : null;
    }

    private void addOwnerName(HttpSession session, Model model) {
        String name = (String) session.getAttribute("LOGGED_IN_FULLNAME");
        model.addAttribute("shopOwnerName", name != null ? name : "Chủ cửa hàng");
    }
}
