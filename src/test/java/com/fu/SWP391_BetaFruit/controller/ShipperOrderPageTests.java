package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.ShipperOrderResponse;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.service.SessionUserService;
import com.fu.SWP391_BetaFruit.service.impl.ShipperOrderQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.dao.DataAccessResourceFailureException;
import java.math.BigDecimal;
import java.util.*;
import java.nio.file.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ShipperOrderPageTests extends OrderPageTestSupport {
    @org.junit.jupiter.api.BeforeEach void setupPage() {
        User user = new User(); user.setUserId(7); user.setFullName("Nguyễn Minh Anh");
        when(sessions.findCurrentUser(any())).thenReturn(Optional.of(user));
        mvc = pageMvc(RoleName.DELIVERY_STAFF, new ShipperOrderController(orders, sessions, details, delivery));
    }
    private final ShipperOrderQueryService orders = mock(ShipperOrderQueryService.class);
    private final SessionUserService sessions = mock(SessionUserService.class);
    private final com.fu.SWP391_BetaFruit.service.impl.DelivererOrderDetailService details = mock(com.fu.SWP391_BetaFruit.service.impl.DelivererOrderDetailService.class);
    private final com.fu.SWP391_BetaFruit.service.impl.OrderDeliveryService delivery = mock(com.fu.SWP391_BetaFruit.service.impl.OrderDeliveryService.class);

    void login() {
        User user = new User(); user.setUserId(7); user.setFullName("Nguyễn Minh Anh");
        when(sessions.findCurrentUser(any())).thenReturn(Optional.of(user));
    }

    ShipperOrderResponse order(int id, String method, String status) {
        boolean collect = method.equals("COD") && status.equals("DELIVERING");
        return new ShipperOrderResponse(id, status, "Ngọc Hà", "0901234567", "12 Nguyễn Huệ", "BetaFruit",
                method, new BigDecimal("115000"), method.equals("COD") ? new BigDecimal("115000") : BigDecimal.ZERO,
                collect ? new BigDecimal("115000") : BigDecimal.ZERO, BigDecimal.ZERO,
                method.equals("COD") ? "NOT_COLLECTED" : "NOT_APPLICABLE", false, false, null, collect);
    }

    @Test void rendersSidebarPaymentBadgesCashAmountAndPagination() throws Exception {
        login();
        when(orders.searchOrders(7, null, null, null, "assignedAt", "desc", 0, 10))
                .thenReturn(new PageImpl<>(List.of(order(12, "COD", "DELIVERING"),
                        order(13, "VNPAY", "READY"), order(14, "COD", "DELIVERED")), PageRequest.of(0, 10), 24));
        String html = mvc.perform(get("/shipper/orders").param("shipperId", "99"))
                .andExpect(status().isOk()).andExpect(view().name("shipper/orders"))
                .andExpect(content().string(containsString("deliverer-sidebar__link--active")))
                .andExpect(content().string(containsString("Nguyễn Minh Anh")))
                .andExpect(content().string(containsString("THANH TOÁN")))
                .andExpect(content().string(not(containsString("TIỀN NHẬN"))))
                .andExpect(content().string(containsString("Cần thu 115,000đ")))
                .andExpect(content().string(containsString("Không thu tiền mặt")))
                .andExpect(content().string(containsString("Đã giao - chờ hoàn tất")))
                .andExpect(content().string(containsString("Trang 1 / 3")))
                .andExpect(content().string(containsString("/shipper/orders/12")))
                .andReturn().getResponse().getContentAsString();
        Files.createDirectories(Path.of("target", "ui-preview"));
        Files.writeString(Path.of("target", "ui-preview", "shipper-orders.html"), html);
        verify(orders).searchOrders(7, null, null, null, "assignedAt", "desc", 0, 10);
    }

    @Test void preservesFiltersAndSortInPagination() throws Exception {
        login();
        when(orders.searchOrders(7, "Hà", OrderStatus.DELIVERING, PaymentMethod.COD, "totalAmount", "asc", 1, 10))
                .thenReturn(new PageImpl<>(List.of(order(12, "COD", "DELIVERING")), PageRequest.of(1, 10), 25));
        mvc.perform(get("/shipper/orders").param("keyword", "Hà").param("status", "DELIVERING")
                        .param("paymentMethod", "COD").param("sortBy", "totalAmount")
                        .param("sortDirection", "asc").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("paymentMethod=COD")))
                .andExpect(content().string(containsString("status=DELIVERING")))
                .andExpect(content().string(containsString("sortBy=totalAmount")))
                .andExpect(content().string(containsString("Trang 2 / 3")));
    }

    @Test void emptyAndDatabaseFailureAreDifferentStates() throws Exception {
        login();
        when(orders.searchOrders(7, null, null, null, "assignedAt", "desc", 0, 10)).thenReturn(Page.empty());
        mvc.perform(get("/shipper/orders")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Không có đơn giao hàng phù hợp")));
        when(orders.searchOrders(7, null, null, null, "assignedAt", "desc", 0, 10))
                .thenThrow(new DataAccessResourceFailureException("test"));
        mvc.perform(get("/shipper/orders")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Không thể tải đơn giao hàng")))
                .andExpect(content().string(not(containsString("Không có đơn giao hàng phù hợp"))));
    }

    @Test void noLoginRedirectsWithoutFallbackAndInvalidFilterIsRejected() throws Exception {
        when(sessions.findCurrentUser(any())).thenReturn(Optional.empty());
        mvc.perform(get("/shipper/orders").param("shipperId", "99"))
                .andExpect(redirectedUrl("/auth/login"));
        verifyNoInteractions(orders);
        login();
        mvc.perform(get("/shipper/orders").param("paymentMethod", "INVALID"))
                .andExpect(status().isBadRequest());
    }
}
