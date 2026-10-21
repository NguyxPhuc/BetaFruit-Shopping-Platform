package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.customer.CheckoutResponse;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import com.fu.SWP391_BetaFruit.service.CartService;
import com.fu.SWP391_BetaFruit.service.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CheckoutVnpayFlowTest {
    CheckoutService checkout = mock(CheckoutService.class);
    CheckoutPaymentService payments = mock(CheckoutPaymentService.class);
    MockHttpSession session = new MockHttpSession();
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new CheckoutController(checkout, mock(CartService.class), payments)).build();
    CheckoutResponse quote = new CheckoutResponse(List.of(), List.of(1), 1, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
    List<CheckoutResponse.PlacedOrder> orders = List.of(new CheckoutResponse.PlacedOrder(101, "Shop A", BigDecimal.TEN));

    @BeforeEach void setup() throws Exception {
        session.setAttribute("LOGGED_IN_USER_ID", 7);
        when(checkout.preview(7, List.of(1))).thenReturn(quote);
        when(checkout.place(7, quote, 9, PaymentMethod.VNPAY)).thenReturn(orders);
        when(payments.start(eq(7), eq(101), anyString(), eq("http://localhost")))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?test=1");
    }

    String draftToken() throws Exception {
        return (String) mvc.perform(get("/customer/checkout").session(session).param("itemIds", "1"))
                .andReturn().getModelAndView().getModel().get("checkoutToken");
    }

    @Test void checkoutRedirectsToVnpayAndReceiptAllowsRetryForSameOrder() throws Exception {
        String token = draftToken();
        mvc.perform(post("/customer/checkout").session(session).param("addressId", "9")
                        .param("checkoutToken", token).param("paymentMethod", "VNPAY"))
                .andExpect(redirectedUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?test=1"));
        assertNull(session.getAttribute("CHECKOUT_DRAFT"));
        when(payments.states(7, List.of(101))).thenReturn(Map.of(101, new CheckoutPaymentService.State(false, true)));
        var receipt = mvc.perform(get("/customer/checkout/success").session(session))
                .andExpect(model().attribute("paymentMethod", "VNPAY")).andReturn().getModelAndView().getModel();
        String paymentToken = (String) receipt.get("paymentToken");
        mvc.perform(post("/customer/checkout/101/pay").session(session).param("paymentToken", paymentToken))
                .andExpect(redirectedUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?test=1"));
        mvc.perform(post("/customer/checkout/999/pay").session(session).param("paymentToken", paymentToken))
                .andExpect(redirectedUrl("/cart"));
        verify(payments, never()).start(any(), eq(999), anyString(), anyString());
        mvc.perform(post("/customer/checkout").session(session).param("addressId", "9")
                        .param("checkoutToken", token).param("paymentMethod", "VNPAY"))
                .andExpect(redirectedUrl("/cart"));
        verify(checkout, times(1)).place(7, quote, 9, PaymentMethod.VNPAY);
    }

    @Test void paymentLinkFailureKeepsReceiptAndNeverRecreatesOrders() throws Exception {
        when(payments.start(eq(7), eq(101), anyString(), anyString())).thenThrow(new Exception("Gateway unavailable"));
        mvc.perform(post("/customer/checkout").session(session).param("addressId", "9")
                        .param("checkoutToken", draftToken()).param("paymentMethod", "VNPAY"))
                .andExpect(redirectedUrl("/customer/checkout/success")).andExpect(flash().attributeExists("errorMessage"));
        assertNotNull(session.getAttribute("CHECKOUT_RECEIPT"));
        assertNull(session.getAttribute("CHECKOUT_DRAFT"));
        verify(checkout, times(1)).place(7, quote, 9, PaymentMethod.VNPAY);
    }

    @Test void invalidMethodDoesNotCreateOrders() throws Exception {
        mvc.perform(post("/customer/checkout").session(session).param("addressId", "9")
                        .param("checkoutToken", draftToken()).param("paymentMethod", "INVALID"))
                .andExpect(redirectedUrl("/cart"));
        verify(checkout, never()).place(any(), any(), any(), any());
    }
}
