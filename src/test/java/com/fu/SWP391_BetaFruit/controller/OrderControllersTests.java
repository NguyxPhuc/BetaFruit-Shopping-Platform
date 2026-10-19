package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.ShipperCODSummaryResponse;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.*;
import com.fu.SWP391_BetaFruit.exception.GlobalExceptionHandler;
import com.fu.SWP391_BetaFruit.interceptor.*;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.*;
import com.fu.SWP391_BetaFruit.service.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.RedirectView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/** Registers page and JSON controllers together with the production access interceptors.
 * Templates are deliberately not rendered until their migration in step 6. */
class OrderControllersTests {
    private final ShopOrderService shop = mock(ShopOrderService.class);
    private final ShipperOrderQueryService orders = mock(ShipperOrderQueryService.class);
    private final DelivererOrderDetailService details = mock(DelivererOrderDetailService.class);
    private final OrderDeliveryService delivery = mock(OrderDeliveryService.class);
    private final UserRepository users = mock(UserRepository.class);
    private MockMvc mvc;

    @BeforeEach void setup() {
        var sessions = new SessionUserService(users);
        mvc = standaloneSetup(new ShopOrderController(shop), new ShipperOrderController(orders, sessions, details, delivery),
                new ShipperOrderQueryController(orders, sessions), new OrderDeliveryController(delivery, sessions))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addMappedInterceptors(new String[]{"/shop/**"}, new ShopOwnerInterceptor())
                .addMappedInterceptors(new String[]{"/shipper/**"}, new ShipperInterceptor())
                .setViewResolvers((name, locale) -> {
                    if (name.startsWith("redirect:")) return new RedirectView(name.substring(9));
                    return new AbstractView() {
                        @Override protected void renderMergedOutputModel(Map<String, Object> model,
                                HttpServletRequest request, HttpServletResponse response) { }
                    };
                }).build();
        User user = new User(); user.setUserId(7); user.setFullName("Driver");
        when(users.findById(7)).thenReturn(Optional.of(user));
    }

    private MockHttpSession session(RoleName role) {
        var session = new MockHttpSession(); session.setAttribute("LOGGED_IN_USER_ID", 7);
        session.setAttribute("LOGGED_IN_ROLES", List.of(role)); return session;
    }

    @Test void anonymousPagesAndFormsRedirectWithoutAccessingServices() throws Exception {
        for (String base : List.of("/shop/orders", "/shipper/orders")) {
            mvc.perform(get(base)).andExpect(redirectedUrl("/auth/login"));
            mvc.perform(get(base + "/12")).andExpect(redirectedUrl("/auth/login"));
        }
        for (String step : List.of("confirm", "preparing", "ready", "cancel")) {
            mvc.perform(post("/shop/orders/12/" + step)).andExpect(redirectedUrl("/auth/login"));
        }
        for (String step : List.of("picked-up", "delivering", "delivered")) {
            mvc.perform(post("/shipper/orders/12/" + step)).andExpect(redirectedUrl("/auth/login"));
        }
        verifyNoInteractions(shop, orders, details, delivery);
    }

    @Test void anonymousApisReturn401IncludingHtmlAlias() throws Exception {
        for (String url : List.of("/shop/orders/api", "/shop/orders/api/12", "/shop/orders.html/api",
                "/shipper/orders/api", "/shipper/orders/api/12", "/shipper/orders/api/cod-summary")) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
        for (String step : List.of("picked-up", "delivering", "delivered")) {
            mvc.perform(post("/shipper/orders/api/12/" + step)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(shop, orders, details, delivery);
    }

    @Test void wrongRoleCannotAccessPagesOrApis() throws Exception {
        var customer = session(RoleName.CUSTOMER);
        for (String base : List.of("/shop/orders", "/shipper/orders")) {
            mvc.perform(get(base).session(customer)).andExpect(redirectedUrl("/403"));
            mvc.perform(get(base + "/api").session(customer)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/shop/orders/12/confirm").session(customer)).andExpect(redirectedUrl("/403"));
        mvc.perform(post("/shipper/orders/api/12/delivered").session(customer)).andExpect(status().isForbidden());
        verifyNoInteractions(shop, orders, details, delivery);
    }

    @Test void malformedOrLegacyIdentityIsRejected() throws Exception {
        var session = session(RoleName.SHOP_OWNER);
        session.setAttribute("LOGGED_IN_USER_ID", "7");
        mvc.perform(get("/shop/orders").session(session)).andExpect(redirectedUrl("/auth/login"));
        mvc.perform(get("/shop/orders/api").session(session)).andExpect(status().isUnauthorized());
        session.removeAttribute("LOGGED_IN_USER_ID"); session.setAttribute("CURRENT_USER_ID", 7);
        mvc.perform(get("/shop/orders").session(session)).andExpect(redirectedUrl("/auth/login"));
        var shipper = session(RoleName.DELIVERY_STAFF); shipper.setAttribute("LOGGED_IN_USER_ID", 7L);
        mvc.perform(get("/shipper/orders/api").session(shipper)).andExpect(status().isUnauthorized());
        verifyNoInteractions(shop, orders, details, delivery);
    }

    @Test void shopPagesAndApisUseSessionOwnerAndNameIgnoringRequestOwner() throws Exception {
        var owner = session(RoleName.SHOP_OWNER); owner.setAttribute("LOGGED_IN_FULLNAME", "Chủ shop A");
        when(shop.getOrdersForShopOwner(7, "apple", OrderStatus.PENDING, "createdAt", "desc", 0, 10))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        mvc.perform(get("/shop/orders").session(owner).param("ownerId", "99").param("keyword", "apple").param("status", "PENDING"))
                .andExpect(view().name("shop/orders")).andExpect(model().attribute("shopOwnerName", "Chủ shop A"));
        mvc.perform(get("/shop/orders/12").session(owner).param("ownerId", "99"))
                .andExpect(view().name("shop/order-detail"));
        mvc.perform(get("/shop/orders/api/12").session(owner).param("ownerId", "99")).andExpect(status().isOk());
        mvc.perform(get("/shop/orders/api").session(owner).param("ownerId", "99").param("keyword", "apple").param("status", "PENDING"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        verify(shop, times(2)).getOrdersForShopOwner(7, "apple", OrderStatus.PENDING, "createdAt", "desc", 0, 10);
        verify(shop, times(2)).getOrderDetailForShopOwner(12, 7);
        verifyNoMoreInteractions(shop);
    }

    @Test void shopNameFallbackAndHtmlAliasWork() throws Exception {
        when(shop.getOrdersForShopOwner(eq(7), isNull(), isNull(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(Page.empty());
        mvc.perform(get("/shop/orders.html").session(session(RoleName.SHOP_OWNER)))
                .andExpect(view().name("shop/orders")).andExpect(model().attribute("shopOwnerName", "Chủ cửa hàng"));
    }

    @Test void allShopFormsUseSessionAndRedirectWithoutOwnerParameter() throws Exception {
        var owner = session(RoleName.SHOP_OWNER);
        for (String step : List.of("confirm", "preparing", "ready", "cancel")) {
            mvc.perform(post("/shop/orders/12/" + step).session(owner).param("ownerId", "99").param("returnToDetail", "true"))
                    .andExpect(redirectedUrl("/shop/orders/12"));
        }
        verify(shop).confirmOrder(12, 7); verify(shop).startPreparingOrder(12, 7);
        verify(shop).markOrderReady(12, 7); verify(shop).cancelOrder(12, 7);
        mvc.perform(post("/shop/orders/12/confirm").session(owner)).andExpect(redirectedUrl("/shop/orders"));
    }

    @Test void foreignShopOrderIsNotExposedAndCannotBeChanged() throws Exception {
        when(shop.getOrderDetailForShopOwner(12, 7)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(shop).confirmOrder(12, 7);
        var owner = session(RoleName.SHOP_OWNER);
        mvc.perform(get("/shop/orders/12").session(owner).param("ownerId", "99")).andExpect(status().isNotFound());
        mvc.perform(post("/shop/orders/12/confirm").session(owner).param("ownerId", "99")).andExpect(status().isNotFound());
    }

    @Test void shipperPagesFilterVnpayAndUseRealSessionIdentity() throws Exception {
        when(orders.searchOrders(7, "apple", OrderStatus.READY, PaymentMethod.VNPAY, "assignedAt", "desc", 0, 10)).thenReturn(Page.empty());
        var shipper = session(RoleName.DELIVERY_STAFF);
        mvc.perform(get("/shipper/orders").session(shipper).param("shipperId", "99").param("keyword", "apple")
                        .param("status", "READY").param("paymentMethod", "VNPAY").param("sortBy", "bad").param("sortDirection", "bad"))
                .andExpect(view().name("shipper/orders")).andExpect(model().attribute("delivererName", "Driver"));
        mvc.perform(get("/shipper/orders/12").session(shipper).param("shipperId", "99"))
                .andExpect(view().name("shipper/order-detail"));
        verify(orders).searchOrders(7, "apple", OrderStatus.READY, PaymentMethod.VNPAY, "assignedAt", "desc", 0, 10);
        verify(details).getDetail(12, 7);
    }

    @Test void shipperFormsAndApisStayDistinctAndUseSessionIdentity() throws Exception {
        var shipper = session(RoleName.DELIVERY_STAFF);
        for (String prefix : List.of("/shipper/orders/12/", "/shipper/orders/api/12/")) {
            for (String step : List.of("picked-up", "delivering", "delivered")) {
                var result = mvc.perform(post(prefix + step).session(shipper).param("shipperId", "99").param("codCollected", "true"));
                if (prefix.contains("/api/")) result.andExpect(status().isNoContent());
                else result.andExpect(redirectedUrl("/shipper/orders/12")).andExpect(flash().attributeExists("statusMessage"));
            }
        }
        verify(delivery, times(2)).markPickedUp(12, 7);
        verify(delivery, times(2)).startDelivering(12, 7);
        verify(delivery, times(2)).markDelivered(12, 7, true);
        mvc.perform(post("/shipper/orders/api/12/delivered").session(shipper)).andExpect(status().isNoContent());
        verify(delivery).markDelivered(12, 7, false);
    }

    @Test void shipperJsonReadsAndSummaryUseSessionIdentity() throws Exception {
        var shipper = session(RoleName.DELIVERY_STAFF);
        when(orders.getOrders(7, 0, 10)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(orders.getCODSummary(7)).thenReturn(new ShipperCODSummaryResponse(2, new BigDecimal("230000")));
        mvc.perform(get("/shipper/orders/api").session(shipper).param("shipperId", "99"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/shipper/orders/api/12").session(shipper).param("shipperId", "99")).andExpect(status().isOk());
        mvc.perform(get("/shipper/orders/api/cod-summary").session(shipper)).andExpect(status().isOk())
                .andExpect(jsonPath("$.amountToRemit").value(230000));
        verify(orders).getOrders(7, 0, 10); verify(orders).getOrder(12, 7); verify(orders).getCODSummary(7);
    }

    @Test void deletedShipperCannotReadOrChangeOrders() throws Exception {
        when(users.findById(7)).thenReturn(Optional.empty());
        var shipper = session(RoleName.DELIVERY_STAFF);
        mvc.perform(get("/shipper/orders").session(shipper)).andExpect(redirectedUrl("/auth/login"));
        mvc.perform(post("/shipper/orders/12/delivered").session(shipper)).andExpect(redirectedUrl("/auth/login"));
        mvc.perform(get("/shipper/orders/api").session(shipper)).andExpect(status().isUnauthorized());
        mvc.perform(post("/shipper/orders/api/12/delivered").session(shipper)).andExpect(status().isUnauthorized());
        verifyNoInteractions(orders, details, delivery);
    }

    @Test void deliveryConflictShowsFlashButForbiddenIsNotSwallowed() throws Exception {
        var shipper = session(RoleName.DELIVERY_STAFF);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Trạng thái không hợp lệ")).when(delivery).markDelivered(12, 7, false);
        mvc.perform(post("/shipper/orders/12/delivered").session(shipper))
                .andExpect(redirectedUrl("/shipper/orders/12")).andExpect(flash().attribute("statusError", "Trạng thái không hợp lệ"));
        mvc.perform(post("/shipper/orders/api/12/delivered").session(shipper)).andExpect(status().isConflict());
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(delivery).markPickedUp(12, 7);
        mvc.perform(post("/shipper/orders/12/picked-up").session(shipper)).andExpect(status().isForbidden());
        when(details.getDetail(12, 7)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(get("/shipper/orders/12").session(shipper)).andExpect(status().isNotFound());
    }
}
