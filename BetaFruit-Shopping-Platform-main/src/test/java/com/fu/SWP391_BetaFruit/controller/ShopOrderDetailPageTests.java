package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.*;
import com.fu.SWP391_BetaFruit.service.ShopOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.nio.file.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShopOrderController.class)
class ShopOrderDetailPageTests {
    @Autowired MockMvc mvc;
    @MockitoBean ShopOrderService service;

    private ShopOrderDetailResponse fixture(String status, ShopOrderDetailResponse.Delivery delivery) {
        var summary = new ShopOrderListItemResponse(12, "12", "Nguyễn Ngọc Hà",
                new BigDecimal("110000"), new BigDecimal("85000"), new BigDecimal("85000"),
                status, status, status.equals("PENDING"), status.equals("CONFIRMED"),
                status.equals("PREPARING"), List.of("PENDING", "CONFIRMED", "PREPARING", "READY").contains(status),
                LocalDateTime.of(2026, 9, 26, 9, 30));
        return new ShopOrderDetailResponse(summary, 2, "BetaFruit", 3, "0901234567",
                "12 Nguyễn Huệ, Phường Sài Gòn, TP. Hồ Chí Minh", new BigDecimal("100000"),
                new BigDecimal("10000"), new BigDecimal("20000"), "COD", false, null, "FRUIT10",
                List.of(new ShopOrderDetailResponse.Item(1, "Táo Fuji Nhật Bản", "Hộp 1 kg", 2,
                        new BigDecimal("50000"), new BigDecimal("100000"))), List.of(), delivery, List.of());
    }

    @Test void rendersItemsMoneyAndUnassignedShipper() throws Exception {
        when(service.getOrderDetailForShopOwner(12, 1)).thenReturn(fixture("PENDING", null));
        var html = mvc.perform(get("/shop/orders/12").param("ownerId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đơn này chưa được bàn giao cho shipper nào cả.")))
                .andExpect(content().string(containsString("Hộp 1 kg")))
                .andExpect(content().string(containsString("110,000đ")))
                .andExpect(content().string(containsString("85,000đ")))
                .andExpect(content().string(containsString("/shop/orders/12/confirm")))
                .andExpect(content().string(not(containsString("/shop/orders/12/ready"))))
                .andReturn().getResponse().getContentAsString();
        Files.createDirectories(Path.of("target", "ui-preview"));
        Files.writeString(Path.of("target", "ui-preview", "order-detail.html"), html);
    }

    @Test void rendersAssignedShipperAndCorrectStateActions() throws Exception {
        var delivery = new ShopOrderDetailResponse.Delivery(1, 8, "Trần Minh", "0912345678",
                LocalDateTime.of(2026, 9, 26, 10, 0));
        when(service.getOrderDetailForShopOwner(12, 1)).thenReturn(fixture("PREPARING", delivery));
        mvc.perform(get("/shop/orders/12"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trần Minh")))
                .andExpect(content().string(containsString("/shop/orders/12/ready")))
                .andExpect(content().string(not(containsString("/shop/orders/12/confirm"))))
                .andExpect(content().string(not(containsString("Đơn này chưa được bàn giao"))));
    }

    @Test void statusChangesReturnToOriginatingPage() throws Exception {
        mvc.perform(post("/shop/orders/12/confirm").param("ownerId", "1").param("returnToDetail", "true"))
                .andExpect(redirectedUrl("/shop/orders/12?ownerId=1"));
        verify(service).confirmOrder(12, 1);
        mvc.perform(post("/shop/orders/12/preparing").param("ownerId", "1").param("returnToDetail", "true"))
                .andExpect(redirectedUrl("/shop/orders/12?ownerId=1"));
        verify(service).startPreparingOrder(12, 1);
        mvc.perform(post("/shop/orders/12/ready").param("ownerId", "1").param("returnToDetail", "true"))
                .andExpect(redirectedUrl("/shop/orders/12?ownerId=1"));
        verify(service).markOrderReady(12, 1);
        mvc.perform(post("/shop/orders/12/cancel").param("ownerId", "1"))
                .andExpect(redirectedUrl("/shop/orders?ownerId=1"));
        verify(service).cancelOrder(12, 1);
    }
}
