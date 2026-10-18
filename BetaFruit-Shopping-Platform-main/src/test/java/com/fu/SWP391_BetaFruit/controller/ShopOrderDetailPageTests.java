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

    @Test void orderListLinksToSelectedOrderDetailForSameOwner() throws Exception {
        var detail = fixture("PENDING", null);
        when(service.getOrdersForShopOwner(7, null, null, "createdAt", "desc", 0, 10))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(detail.summary())));
        when(service.getOrderDetailForShopOwner(12, 7)).thenReturn(detail);

        mvc.perform(get("/shop/orders").param("ownerId", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/shop/orders/12?ownerId=7\"")));
        mvc.perform(get("/shop/orders/12").param("ownerId", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/order-detail"))
                .andExpect(model().attribute("detail", detail))
                .andExpect(content().string(containsString("href=\"/shop/orders?ownerId=7\"")));
        verify(service).getOrderDetailForShopOwner(12, 7);
    }

    @Test void deliveredFilterAndHoldingNoticeRender() throws Exception {
        var detail = fixture("DELIVERED", null);
        when(service.getOrderDetailForShopOwner(12, 1)).thenReturn(detail);
        when(service.getOrdersForShopOwner(1, null, null, "createdAt", "desc", 0, 10))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(detail.summary())));
        mvc.perform(get("/shop/orders"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"DELIVERED\"")));
        mvc.perform(get("/shop/orders/12"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tiền chưa được cộng vào số dư shop")));
    }

    private ShopOrderDetailResponse fixture(String status, ShopOrderDetailResponse.Delivery delivery) {
        return fixture(status, delivery, "COD", false, new BigDecimal("60000"), new BigDecimal("25000"));
    }

    private ShopOrderDetailResponse fixture(String status, ShopOrderDetailResponse.Delivery delivery,
            String paymentMethod, Boolean remitted, BigDecimal totalCost, BigDecimal profit) {
        return fixture(status, delivery, paymentMethod, remitted, totalCost, profit, "SHOP");
    }

    private ShopOrderDetailResponse fixture(String status, ShopOrderDetailResponse.Delivery delivery,
            String paymentMethod, Boolean remitted, BigDecimal totalCost, BigDecimal profit, String couponSource) {
        String couponCode = couponSource.equals("NONE") ? null : "FRUIT10";
        BigDecimal net = new BigDecimal(couponSource.equals("SHOP") ? "85000" : "95000");
        var summary = new ShopOrderListItemResponse(12, "12", "Nguyễn Ngọc Hà",
                new BigDecimal("110000"), net, net,
                status, status, status.equals("PENDING"), status.equals("CONFIRMED"),
                status.equals("PREPARING"), List.of("PENDING", "CONFIRMED", "PREPARING", "READY").contains(status),
                LocalDateTime.of(2026, 9, 26, 9, 30), couponCode, couponSource);
        return new ShopOrderDetailResponse(summary, 2, "BetaFruit", 3, "0901234567",
                "12 Nguyễn Huệ, Phường Sài Gòn, TP. Hồ Chí Minh", new BigDecimal("100000"),
                new BigDecimal("10000"), new BigDecimal("20000"), paymentMethod, false, null, couponCode,
                List.of(new ShopOrderDetailResponse.Item(1, "Táo Fuji Nhật Bản", "Hộp 1 kg", 2,
                        new BigDecimal("50000"), new BigDecimal("100000"), new BigDecimal("30000"))),
                List.of(), delivery, List.of(), new BigDecimal("5000"), remitted, totalCost, profit,
                new BigDecimal("5.00"), "SHOP", couponSource.equals("SHOP") ? new BigDecimal("10000") : BigDecimal.ZERO);
    }

    @Test void labelsCouponFundingOnlyOnDetail() throws Exception {
        for (String source : List.of("SHOP", "PLATFORM", "NONE")) {
            var detail = fixture("PENDING", null, "COD", false, null, null, source);
            when(service.getOrderDetailForShopOwner(12, 1)).thenReturn(detail);
            when(service.getOrdersForShopOwner(1, null, null, "createdAt", "desc", 0, 10))
                    .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(detail.summary())));
            mvc.perform(get("/shop/orders"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("VOUCHER"))))
                    .andExpect(content().string(not(containsString("FRUIT10"))));
            var response = mvc.perform(get("/shop/orders/12")).andExpect(status().isOk());
            if (source.equals("PLATFORM")) {
                response.andExpect(content().string(containsString("Nền tảng chịu chi phí giảm giá")))
                        .andExpect(content().string(not(containsString("− Voucher do shop chịu"))));
            } else if (source.equals("SHOP")) {
                response.andExpect(content().string(containsString("Shop chịu chi phí giảm giá")))
                        .andExpect(content().string(containsString("− Voucher do shop chịu")));
            } else {
                response.andExpect(content().string(not(containsString("FRUIT10"))));
            }
        }
    }

    @Test void rendersItemsMoneyAndUnassignedShipper() throws Exception {
        when(service.getOrderDetailForShopOwner(12, 1)).thenReturn(fixture("PENDING", null));
        var html = mvc.perform(get("/shop/orders/12").param("ownerId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đơn này chưa được bàn giao cho shipper nào cả.")))
                .andExpect(content().string(containsString("Hộp 1 kg")))
                .andExpect(content().string(containsString("110,000đ")))
                .andExpect(content().string(containsString("85,000đ")))
                .andExpect(content().string(containsString("5,000đ")))
                .andExpect(content().string(containsString("60,000đ")))
                .andExpect(content().string(containsString("25,000đ")))
                .andExpect(content().string(containsString("Chưa nộp tiền COD")))
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

    @Test void rendersFractionalCostsAndNegativeProfitForRemittedCod() throws Exception {
        when(service.getOrderDetailForShopOwner(12, 1)).thenReturn(fixture(
                "SUCCESS", null, "COD", true, new BigDecimal("85012.80"), new BigDecimal("-12.80")));
        mvc.perform(get("/shop/orders/12").param("ownerId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("85,012.80đ")))
                .andExpect(content().string(containsString("-12.80đ")))
                .andExpect(content().string(containsString("Đã nộp tiền COD")))
                .andExpect(content().string(containsString("Chưa chuyển tiền vào số dư shop")));
    }

    @Test void rendersUnknownCostsAndHidesCodForOnlinePayment() throws Exception {
        when(service.getOrderDetailForShopOwner(12, 1)).thenReturn(fixture(
                "PENDING", null, "ONLINE", null, null, null));
        mvc.perform(get("/shop/orders/12").param("ownerId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chưa đủ dữ liệu")))
                .andExpect(content().string(not(containsString("nộp tiền COD"))));
    }

    @Test void pricingValidationIsNotReportedAsDatabaseConnectionFailure() throws Exception {
        when(service.getOrdersForShopOwner(1, null, null, "createdAt", "desc", 0, 10))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, "Khoản phí sàn đã lưu không hợp lệ"));
        mvc.perform(get("/shop/orders"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dữ liệu đơn hàng không hợp lệ")))
                .andExpect(content().string(containsString("Khoản phí sàn đã lưu không hợp lệ")))
                .andExpect(content().string(not(containsString("Không thể kết nối cơ sở dữ liệu"))));
    }

    @Test void connectionQueryAndProcessingErrorsHaveDifferentMessages() throws Exception {
        var failures = List.of(
                new org.springframework.dao.DataAccessResourceFailureException("offline"),
                new org.springframework.dao.InvalidDataAccessResourceUsageException("bad SQL"),
                new IllegalStateException("unexpected"));
        var messages = List.of("Không thể kết nối cơ sở dữ liệu", "Không thể truy vấn dữ liệu đơn hàng", "Có lỗi khi xử lý đơn hàng");
        for (int i = 0; i < failures.size(); i++) {
            doThrow(failures.get(i)).when(service).getOrdersForShopOwner(1, null, null, "createdAt", "desc", 0, 10);
            mvc.perform(get("/shop/orders"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(messages.get(i))));
        }
    }
}
