package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.ShopOrderListItemResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopOrderDetailResponse;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.service.ShopOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping({"/shop/orders", "/shop/orders.html"})
public class ShopOrderController {
    /** Temporary test owner. Change it or pass ?ownerId={id} while login is unavailable. */
    private static final Integer TEST_OWNER_ID = 1;

    private final ShopOrderService shopOrderService;

    public ShopOrderController(ShopOrderService shopOrderService) {
        this.shopOrderService = shopOrderService;
    }

    @GetMapping
    public String orderList(
            Model model,
            @RequestParam(defaultValue = "1") Integer ownerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean preview) {
        Integer shopOwnerId = ownerId == null ? TEST_OWNER_ID : ownerId;
        // Restore after login exists:
        // User currentUser = sessionUserService.findCurrentUser(session).orElse(null);
        // if (currentUser == null) return "redirect:/login";
        // Integer shopOwnerId = currentUser.getUserId();
        if (preview) {
            model.addAttribute("orders", Page.empty());
        } else try {
            model.addAttribute("orders", shopOrderService.getOrdersForShopOwner(shopOwnerId, keyword, status, sortBy, sortDirection, page, size));
        } catch (Exception exception) {
            // Keep the page testable when the local SQL Server is not reachable.
            model.addAttribute("orders", Page.empty());
            model.addAttribute("orderLoadError", "Không thể kết nối cơ sở dữ liệu để tải đơn hàng. Kiểm tra lại SQL Server rồi tải lại trang.");
        }
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDirection", sortDirection);
        model.addAttribute("ownerId", shopOwnerId);
        model.addAttribute("shopOwnerName", "Chủ shop test");
        return "shop/orders";
    }

    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Integer orderId,
                              @RequestParam(defaultValue = "1") Integer ownerId,
                              Model model) {
        model.addAttribute("detail", shopOrderService.getOrderDetailForShopOwner(orderId, ownerId));
        model.addAttribute("ownerId", ownerId);
        model.addAttribute("shopOwnerName", "Chủ shop test");
        return "shop/order-detail";
    }
    @PostMapping("/{orderId}/confirm")
    public String confirmOrder(@PathVariable Integer orderId,
                               @RequestParam(defaultValue = "1") Integer ownerId,
                               @RequestParam(defaultValue = "false") boolean returnToDetail) {
        shopOrderService.confirmOrder(orderId, ownerId);
        return "redirect:/shop/orders" + (returnToDetail ? "/" + orderId : "") + "?ownerId=" + ownerId;
    }

    @PostMapping("/{orderId}/preparing")
    public String startPreparingOrder(@PathVariable Integer orderId,
                                      @RequestParam(defaultValue = "1") Integer ownerId,
                               @RequestParam(defaultValue = "false") boolean returnToDetail) {
        shopOrderService.startPreparingOrder(orderId, ownerId);
        return "redirect:/shop/orders" + (returnToDetail ? "/" + orderId : "") + "?ownerId=" + ownerId;
    }

    @PostMapping("/{orderId}/ready")
    public String markOrderReady(@PathVariable Integer orderId,
                                 @RequestParam(defaultValue = "1") Integer ownerId,
                               @RequestParam(defaultValue = "false") boolean returnToDetail) {
        shopOrderService.markOrderReady(orderId, ownerId);
        return "redirect:/shop/orders" + (returnToDetail ? "/" + orderId : "") + "?ownerId=" + ownerId;
    }

    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable Integer orderId,
                              @RequestParam(defaultValue = "1") Integer ownerId,
                               @RequestParam(defaultValue = "false") boolean returnToDetail) {
        shopOrderService.cancelOrder(orderId, ownerId);
        return "redirect:/shop/orders" + (returnToDetail ? "/" + orderId : "") + "?ownerId=" + ownerId;
    }

    @GetMapping("/api")
    @ResponseBody
    public Page<ShopOrderListItemResponse> getOrdersApi(
            @RequestParam(defaultValue = "1") Integer ownerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return shopOrderService.getOrdersForShopOwner(ownerId, keyword, status, sortBy, sortDirection, page, size);
    }

    @GetMapping("/api/{orderId}")
    @ResponseBody
    public ShopOrderDetailResponse getOrderDetailApi(
            @PathVariable Integer orderId,
            @RequestParam(defaultValue = "1") Integer ownerId) {
        // Same temporary owner parameter as the list; replace with session identity when login is connected.
        return shopOrderService.getOrderDetailForShopOwner(orderId, ownerId);
    }
}
