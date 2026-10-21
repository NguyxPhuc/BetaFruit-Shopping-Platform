package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.CheckoutResponse;
import com.fu.SWP391_BetaFruit.entity.Address;
import com.fu.SWP391_BetaFruit.service.CartService;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutService;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CheckoutFlowTest {
    private final CheckoutService checkout = mock(CheckoutService.class);
    private final MockHttpSession session = new MockHttpSession();
    private final CheckoutResponse quote = new CheckoutResponse(List.of(), List.of(1, 2), 2,
            BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
    private MockMvc mvc;

    @BeforeEach void setup() {
        session.setAttribute("LOGGED_IN_USER_ID", 7);
        when(checkout.preview(7, List.of(1, 2))).thenReturn(quote);
        when(checkout.place(7, quote, 9)).thenReturn(List.of(
                new CheckoutResponse.PlacedOrder(101, "Shop A", BigDecimal.TEN)));
        var resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/"); resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8"); resolver.setTemplateMode("HTML");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        mvc = MockMvcBuilders.standaloneSetup(new CheckoutController(checkout, mock(CartService.class), mock(CheckoutPaymentService.class)))
                .setViewResolvers((name, locale) -> {
                    if (name.startsWith("redirect:")) return new RedirectView(name.substring(9));
                    return new View() {
                        public void render(Map<String, ?> model, jakarta.servlet.http.HttpServletRequest request,
                                           jakarta.servlet.http.HttpServletResponse response) throws Exception {
                            response.setCharacterEncoding("UTF-8");
                            var app = JakartaServletWebApplication.buildApplication(request.getServletContext());
                            engine.process(name, new WebContext(app.buildExchange(request, response), locale,
                                    new java.util.HashMap<>(model)), response.getWriter());
                        }
                    };
                }).build();
    }

    @Test void addAddressThenReturnFromCartAndSubmitRenderedForm() throws Exception {
        when(checkout.shippingAddresses(7)).thenReturn(List.of());
        var initial = openCheckout();
        assertTrue(initial.getResponse().getContentAsString().contains("Bạn chưa có địa chỉ"));
        assertTrue(initial.getResponse().getContentAsString().contains("disabled=\"disabled\""));

        // Address creation belongs to another feature: simulate its result without calling or changing it.
        Address address = new Address(); address.setAddressId(9); address.setIsDefault(true);
        address.setReceiverName("Nguyễn An"); address.setReceiverPhone("0901234567");
        address.setAddressLine("12 Lê Lợi"); address.setWard("Phường 1");
        address.setDistrict("Quận 1"); address.setCity("TP.HCM");
        when(checkout.shippingAddresses(7)).thenReturn(List.of(address));
        var returned = openCheckout();
        String html = returned.getResponse().getContentAsString();
        assertTrue(html.contains("Nguyễn An"));
        String token = token(returned);
        // A second GET for the same cart must not invalidate the already rendered order form.
        assertEquals(token, token(openCheckout()));
        mvc.perform(post("/customer/checkout").session(session).param("addressId", "9").param("checkoutToken", token))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/customer/checkout/success"));
        verify(checkout).place(7, quote, 9);
        mvc.perform(post("/customer/checkout").session(session).param("addressId", "9").param("checkoutToken", token))
                .andExpect(redirectedUrl("/cart"));
        verify(checkout, times(1)).place(any(), any(), any());
    }

    @Test void missingSubmittedTokenNeverCreatesOrder() throws Exception {
        openCheckout();
        mvc.perform(post("/customer/checkout").session(session).param("addressId", "9"))
                .andExpect(redirectedUrl("/cart"));
        verify(checkout, never()).place(any(), any(), any());
    }

    @Test void changedReviewedCartInvalidatesOldForm() throws Exception {
        String oldToken = token(openCheckout());
        var changed = new CheckoutResponse(List.of(), List.of(1, 2), 2,
                new BigDecimal("20"), BigDecimal.ZERO, new BigDecimal("20"));
        when(checkout.preview(7, List.of(1, 2))).thenReturn(changed);
        String newToken = token(openCheckout());
        assertNotEquals(oldToken, newToken);
        mvc.perform(post("/customer/checkout").session(session).param("addressId", "9").param("checkoutToken", oldToken))
                .andExpect(redirectedUrl("/cart"));
        verify(checkout, never()).place(any(), any(), any());
        mvc.perform(post("/customer/checkout").session(session).param("addressId", "9").param("checkoutToken", newToken))
                .andExpect(redirectedUrl("/customer/checkout/success"));
        verify(checkout).place(7, changed, 9);
    }

    @Test void changedCustomerCannotUseExistingDraft() throws Exception {
        String oldToken = token(openCheckout());
        session.setAttribute("LOGGED_IN_USER_ID", 8);
        mvc.perform(post("/customer/checkout").session(session).param("addressId", "9").param("checkoutToken", oldToken))
                .andExpect(redirectedUrl("/cart"));
        verify(checkout, never()).place(any(), any(), any());
    }

    private MvcResult openCheckout() throws Exception {
        return mvc.perform(get("/customer/checkout").session(session).param("itemIds", "1", "2"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andReturn();
    }

    private String token(MvcResult result) throws Exception {
        var match = Pattern.compile("name=\"checkoutToken\"[^>]*value=\"([^\"]+)\"")
                .matcher(result.getResponse().getContentAsString());
        assertTrue(match.find(), "Rendered checkout form must include its token");
        return match.group(1);
    }
}
