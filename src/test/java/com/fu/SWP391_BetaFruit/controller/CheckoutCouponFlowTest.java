package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.customer.CheckoutResponse;
import com.fu.SWP391_BetaFruit.enums.CouponCategory;
import com.fu.SWP391_BetaFruit.service.CartService;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutPaymentService;
import com.fu.SWP391_BetaFruit.service.impl.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CheckoutCouponFlowTest {
    CheckoutService checkout = mock(CheckoutService.class);
    MockHttpSession session = new MockHttpSession();
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new CheckoutController(checkout,
            mock(CartService.class), mock(CheckoutPaymentService.class))).build();
    CheckoutResponse base = new CheckoutResponse(List.of(), List.of(1), 1,
            new BigDecimal("100000"), new BigDecimal("15000"), new BigDecimal("115000"));

    CheckoutResponse discounted(String code, CouponCategory category, int id, String discount) {
        return new CheckoutResponse(base.shops(), base.itemIds(), base.quantity(), base.subtotal(),
                base.shippingFee(), base.finalAmount().subtract(new BigDecimal(discount)),
                new CheckoutResponse.AppliedCoupon(id, code, category, new BigDecimal(discount), true));
    }

    @BeforeEach void setup() {
        session.setAttribute("LOGGED_IN_USER_ID", 7);
        when(checkout.preview(7, List.of(1))).thenReturn(base);
    }

    String token() throws Exception {
        return (String) mvc.perform(get("/customer/checkout").session(session).param("itemIds", "1"))
                .andReturn().getModelAndView().getModel().get("checkoutToken");
    }

    void apply(String token, String code, String action) throws Exception {
        mvc.perform(post("/customer/checkout/coupon").session(session).param("checkoutToken", token)
                        .param("couponCode", code).param("couponAction", action)
                        .param("addressId", "9").param("paymentMethod", "VNPAY"))
                .andExpect(redirectedUrl("/customer/checkout"))
                .andExpect(flash().attribute("selectedAddressId", 9))
                .andExpect(flash().attribute("selectedPaymentMethod", "VNPAY"));
    }

    @Test void applyingSecondCouponReplacesFirstAndRemoveRestoresOriginalTotal() throws Exception {
        var product = discounted("PRODUCT", CouponCategory.PRODUCT, 1, "10000");
        var shipping = discounted("SHIP", CouponCategory.SHIPPING, 2, "15000");
        when(checkout.preview(7, List.of(1), "PRODUCT")).thenReturn(product);
        when(checkout.preview(7, List.of(1), "SHIP")).thenReturn(shipping);
        String original = token();
        apply(original, "PRODUCT", "apply");
        String productToken = token(); assertNotEquals(original, productToken);
        apply(productToken, "SHIP", "apply");
        mvc.perform(get("/customer/checkout").session(session))
                .andExpect(model().attribute("checkout", shipping));
        apply(token(), "SHIP", "remove");
        mvc.perform(get("/customer/checkout").session(session))
                .andExpect(model().attribute("checkout", base));
        verify(checkout, never()).place(any(), any(), any(), any());
    }

    @Test void invalidCodeKeepsLastValidCouponAndInvalidTokenCannotApply() throws Exception {
        var valid = discounted("GOOD", CouponCategory.PRODUCT, 1, "10000");
        when(checkout.preview(7, List.of(1), "GOOD")).thenReturn(valid);
        when(checkout.preview(7, List.of(1), "BAD")).thenThrow(new IllegalArgumentException("Mã không hợp lệ"));
        apply(token(), "GOOD", "apply");
        apply(token(), "BAD", "apply");
        mvc.perform(get("/customer/checkout").session(session)).andExpect(model().attribute("checkout", valid));
        mvc.perform(post("/customer/checkout/coupon").session(session)
                        .param("checkoutToken", "invalid").param("couponCode", "OTHER"))
                .andExpect(redirectedUrl("/cart"));
        verify(checkout, never()).preview(7, List.of(1), "OTHER");
    }

    @Test void expiredCouponIsRemovedWithWarningOnRefresh() throws Exception {
        var valid = discounted("EXPIRE", CouponCategory.PRODUCT, 1, "10000");
        when(checkout.preview(7, List.of(1), "EXPIRE")).thenReturn(valid);
        apply(token(), "EXPIRE", "apply");
        when(checkout.preview(7, List.of(1), "EXPIRE")).thenThrow(new IllegalArgumentException("Đã hết hạn"));
        mvc.perform(get("/customer/checkout").session(session))
                .andExpect(model().attribute("checkout", base))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test void placeUsesReviewedCouponAndIgnoresUnappliedCodeOrAmountFromBrowser() throws Exception {
        var valid = discounted("GOOD", CouponCategory.PRODUCT, 1, "10000");
        when(checkout.preview(7, List.of(1), "GOOD")).thenReturn(valid);
        when(checkout.place(7, valid, 9)).thenReturn(List.of(new CheckoutResponse.PlacedOrder(5, "Shop", valid.finalAmount())));
        apply(token(), "GOOD", "apply");
        mvc.perform(post("/customer/checkout").session(session).param("checkoutToken", token())
                        .param("addressId", "9").param("couponCode", "FORGED").param("finalAmount", "1"))
                .andExpect(redirectedUrl("/customer/checkout/success"));
        verify(checkout).place(7, valid, 9);
        verify(checkout, never()).preview(7, List.of(1), "FORGED");
    }
}
