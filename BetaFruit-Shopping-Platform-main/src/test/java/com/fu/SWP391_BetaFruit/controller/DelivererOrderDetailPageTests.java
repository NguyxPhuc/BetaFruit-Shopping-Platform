package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.*;
import com.fu.SWP391_BetaFruit.service.SessionUserService;
import com.fu.SWP391_BetaFruit.service.impl.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.nio.file.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DelivererOrderController.class)
class DelivererOrderDetailPageTests {
    @Autowired MockMvc mvc;
    @MockitoBean ShipperOrderQueryService orders;
    @MockitoBean SessionUserService sessions;
    @MockitoBean DelivererOrderDetailService details;
    @MockitoBean OrderDeliveryService delivery;

    DelivererOrderDetailResponse fixture(String status, String method) {
        BigDecimal amount = new BigDecimal("115000");
        boolean cod = method.equals("COD");
        var summary = new ShipperOrderResponse(12, status, "Ngọc Hà", "0901234567",
                "12 Nguyễn Huệ, TP. Hồ Chí Minh", "BetaFruit", method, amount,
                cod ? amount : BigDecimal.ZERO,
                cod && !status.equals("DELIVERED") ? amount : BigDecimal.ZERO,
                cod && status.equals("DELIVERED") ? amount : BigDecimal.ZERO,
                cod ? (status.equals("DELIVERED") ? "PENDING_REMITTANCE" : "NOT_COLLECTED") : "NOT_APPLICABLE",
                false, false, null, status.equals("DELIVERING"));
        return new DelivererOrderDetailResponse(summary, null, LocalDateTime.of(2026, 10, 15, 9, 0),
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("15000"),
                List.of(new DelivererOrderDetailResponse.Item("Táo Fuji", "Hộp 1 kg", 2,
                        new BigDecimal("50000"), new BigDecimal("100000"))),
                List.of(new DelivererOrderDetailResponse.StatusHistory("Sẵn sàng giao", "Chủ shop", "Đã chuẩn bị đủ hàng", null)),
                status.equals("READY"), status.equals("PICKED_UP"));
    }

    @Test void fullWidthOverviewComesBeforeItemsAndPaymentAndOnlyNextActionRenders() throws Exception {
        when(sessions.findCurrentUser(any())).thenReturn(Optional.empty());
        for (String state : List.of("READY", "PICKED_UP", "DELIVERING", "DELIVERED")) {
            when(details.getDetail(12, 3)).thenReturn(fixture(state, "COD"));
            String html = mvc.perform(get("/deliverer/orders/12").param("shipperId", "99"))
                    .andExpect(status().isOk()).andExpect(view().name("deliverer/order-detail"))
                    .andExpect(content().string(containsString("Táo Fuji")))
                    .andExpect(content().string(containsString("12 Nguyễn Huệ")))
                    .andExpect(content().string(containsString("Chủ shop")))
                    .andExpect(content().string(not(containsString("Shop thực nhận"))))
                    .andReturn().getResponse().getContentAsString();
            assertTrue(html.indexOf("id=\"order-info-heading\"") < html.indexOf("id=\"items-heading\""));
            assertTrue(html.indexOf("id=\"items-heading\"") < html.indexOf("id=\"payment-heading\""));
            assertEquals(state.equals("READY"), html.contains("action=\"/deliverer/orders/12/picked-up\""));
            assertEquals(state.equals("PICKED_UP"), html.contains("action=\"/deliverer/orders/12/delivering\""));
            assertEquals(state.equals("DELIVERING"), html.contains("action=\"/deliverer/orders/12/delivered\""));
            assertEquals(state.equals("DELIVERING"), html.contains("name=\"codCollected\""));
            assertFalse(html.contains("action=\"/deliverer/orders/12/success\""));
            if (state.equals("DELIVERING")) {
                Files.createDirectories(Path.of("target", "ui-preview"));
                Files.writeString(Path.of("target", "ui-preview", "deliverer-order-detail.html"), html);
            }
        }
    }

    @Test void onlineDeliveryDoesNotAskForCOD() throws Exception {
        when(details.getDetail(12, 3)).thenReturn(fixture("DELIVERING", "ONLINE"));
        mvc.perform(get("/deliverer/orders/12")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Không thu thêm tiền mặt từ khách")))
                .andExpect(content().string(not(containsString("name=\"codCollected\""))));
    }

    @Test void postsUseSameTemporaryIdentityAndRedirectBackToDetail() throws Exception {
        mvc.perform(post("/deliverer/orders/12/picked-up").param("shipperId", "99"))
                .andExpect(redirectedUrl("/deliverer/orders/12"));
        mvc.perform(post("/deliverer/orders/12/delivering")).andExpect(redirectedUrl("/deliverer/orders/12"));
        mvc.perform(post("/deliverer/orders/12/delivered").param("codCollected", "true"))
                .andExpect(redirectedUrl("/deliverer/orders/12"));
        verify(delivery).markPickedUp(12, 3);
        verify(delivery).startDelivering(12, 3);
        verify(delivery).markDelivered(12, 3, true);
    }

    @Test void conflictIsShownOnDetailAndForbiddenIsNotReportedAsSuccess() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Chưa thu đủ COD"))
                .when(delivery).markDelivered(12, 3, false);
        mvc.perform(post("/deliverer/orders/12/delivered"))
                .andExpect(redirectedUrl("/deliverer/orders/12"))
                .andExpect(flash().attribute("statusError", "Chưa thu đủ COD"));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(delivery).markPickedUp(12, 3);
        mvc.perform(post("/deliverer/orders/12/picked-up")).andExpect(status().isForbidden());
        when(details.getDetail(12, 3)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(get("/deliverer/orders/12")).andExpect(status().isNotFound());
    }
}
