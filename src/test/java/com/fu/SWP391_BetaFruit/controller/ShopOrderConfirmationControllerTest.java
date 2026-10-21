package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.service.ShopOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShopOrderConfirmationControllerTest {
    ShopOrderService service = mock(ShopOrderService.class);
    ShopOrderController controller = new ShopOrderController(service);

    @Test void stockOrPaymentConflictIsDisplayedOnBothShopPages() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("LOGGED_IN_USER_ID", 7);
        for (String reason : new String[]{"Không đủ tồn kho", "VNPAY chưa thanh toán thành công"}) {
            doThrow(new ResponseStatusException(HttpStatus.CONFLICT, reason)).when(service).confirmOrder(1, 7);
            for (boolean detail : new boolean[]{false, true}) {
                var redirect = new RedirectAttributesModelMap();
                assertEquals("redirect:/shop/orders" + (detail ? "/1" : ""),
                        controller.confirmOrder(1, session, detail, redirect));
                assertEquals(reason, redirect.getFlashAttributes().get("orderActionError"));
            }
        }
    }

    @Test void foreignOrderIsStillRejected() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("LOGGED_IN_USER_ID", 7);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(service).confirmOrder(1, 7);
        assertThrows(ResponseStatusException.class,
                () -> controller.confirmOrder(1, session, true, new RedirectAttributesModelMap()));
    }
}
