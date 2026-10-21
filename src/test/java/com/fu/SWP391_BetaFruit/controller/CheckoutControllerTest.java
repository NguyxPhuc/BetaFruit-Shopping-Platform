package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.customer.CheckoutResponse;
import com.fu.SWP391_BetaFruit.service.CartService;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutService;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutPaymentService;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckoutControllerTest {
    CheckoutService checkout = mock(CheckoutService.class);
    CheckoutController controller = new CheckoutController(checkout, mock(CartService.class), mock(CheckoutPaymentService.class));
    MockHttpSession session = new MockHttpSession();

    @Test void tokenCanBeConsumedOnlyOnceAndReceiptIsBoundToCustomer() {
        session.setAttribute("LOGGED_IN_USER_ID", 7);
        var quote = new CheckoutResponse(List.of(), List.of(1), 1, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
        when(checkout.preview(7, List.of(1))).thenReturn(quote);
        when(checkout.place(7, quote, 9)).thenReturn(List.of(new CheckoutResponse.PlacedOrder(10, "Shop A", BigDecimal.TEN)));
        var model = new ExtendedModelMap();
        controller.view(List.of(1), session, model, new RedirectAttributesModelMap());
        String token = (String) model.get("checkoutToken");
        assertEquals("redirect:/cart", controller.place(9, "wrong", session, new RedirectAttributesModelMap(), PaymentMethod.COD, new MockHttpServletRequest()));
        verify(checkout, never()).place(any(), any(), any());
        assertEquals("redirect:/customer/checkout/success", controller.place(9, token, session, new RedirectAttributesModelMap(), PaymentMethod.COD, new MockHttpServletRequest()));
        assertEquals("redirect:/cart", controller.place(9, token, session, new RedirectAttributesModelMap(), PaymentMethod.COD, new MockHttpServletRequest()));
        verify(checkout).place(7, quote, 9);
        assertEquals("customer/checkout-success", controller.success(session, new ExtendedModelMap()));
        session.setAttribute("LOGGED_IN_USER_ID", 8);
        assertEquals("redirect:/cart", controller.success(session, new ExtendedModelMap()));
    }

    @Test void missingAddressKeepsDraftForRetry() {
        session.setAttribute("LOGGED_IN_USER_ID", 7);
        var quote = new CheckoutResponse(List.of(), List.of(1), 1, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
        when(checkout.preview(7, List.of(1))).thenReturn(quote);
        when(checkout.place(7, quote, null)).thenThrow(new IllegalArgumentException("Chọn địa chỉ"));
        var model = new ExtendedModelMap();
        controller.view(List.of(1), session, model, new RedirectAttributesModelMap());
        assertEquals("redirect:/customer/checkout", controller.place(null, (String) model.get("checkoutToken"),
                session, new RedirectAttributesModelMap(), PaymentMethod.COD, new MockHttpServletRequest()));
        assertNotNull(session.getAttribute("CHECKOUT_DRAFT"));
    }

    @Test void multiShopSelectionReturnsToCartWithWarning() {
        session.setAttribute("LOGGED_IN_USER_ID", 7);
        String warning = "Chỉ được thanh toán các sản phẩm cùng một cửa hàng trong mỗi lần checkout.";
        when(checkout.preview(7, List.of(1, 2))).thenThrow(new IllegalArgumentException(warning));
        var redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/cart", controller.view(List.of(1, 2), session, new ExtendedModelMap(), redirect));
        assertEquals(warning, redirect.getFlashAttributes().get("errorMessage"));
        verify(checkout, never()).place(any(), any(), any());
        verify(checkout, never()).place(any(), any(), any(), any());
    }
}
