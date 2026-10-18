package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.service.SessionUserService;
import com.fu.SWP391_BetaFruit.service.impl.ShipperOrderQueryService;
import com.fu.SWP391_BetaFruit.dto.response.ShipperCODSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShipperOrderQueryController.class)
class ShipperOrderQueryControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean ShipperOrderQueryService orders;
    @MockitoBean SessionUserService sessions;

    @Test void anonymousCannotReadAnyShipperData() throws Exception {
        when(sessions.findCurrentUser(any())).thenReturn(Optional.empty());
        for (String url : new String[]{"/shipper/orders", "/shipper/orders/12", "/shipper/orders/cod-summary"}) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(orders);
    }

    @Test void usesSessionIdentityInsteadOfCallerSuppliedShipperId() throws Exception {
        User user = new User();
        user.setUserId(7);
        when(sessions.findCurrentUser(any())).thenReturn(Optional.of(user));
        when(orders.getOrders(7, 0, 10)).thenReturn(Page.empty());
        when(orders.getCODSummary(7)).thenReturn(new ShipperCODSummaryResponse(2, new BigDecimal("230000")));
        mvc.perform(get("/shipper/orders").param("shipperId", "8")).andExpect(status().isOk());
        mvc.perform(get("/shipper/orders/12").param("shipperId", "8")).andExpect(status().isOk());
        mvc.perform(get("/shipper/orders/cod-summary").param("shipperId", "8"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amountToRemit").value(230000));
        verify(orders).getOrders(7, 0, 10);
        verify(orders).getOrder(12, 7);
        verify(orders).getCODSummary(7);
    }
}
