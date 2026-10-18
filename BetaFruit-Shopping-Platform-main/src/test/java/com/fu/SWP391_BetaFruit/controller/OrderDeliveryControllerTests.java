package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.service.SessionUserService;
import com.fu.SWP391_BetaFruit.service.impl.OrderDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderDeliveryController.class)
class OrderDeliveryControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean SessionUserService sessions;
    @MockitoBean OrderDeliveryService delivery;

    @Test void anonymousCannotChangeAnyDeliveryStateThroughAPI() throws Exception {
        when(sessions.findCurrentUser(any())).thenReturn(Optional.empty());
        for (String step : new String[]{"picked-up", "delivering", "delivered"}) {
            mvc.perform(post("/shipper/orders/12/" + step)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(delivery);
    }

    @Test void routesUseSessionIdentityAndForwardExplicitCODConfirmation() throws Exception {
        User user = new User(); user.setUserId(7);
        when(sessions.findCurrentUser(any())).thenReturn(Optional.of(user));
        mvc.perform(post("/shipper/orders/12/picked-up").param("shipperId", "99")).andExpect(status().isNoContent());
        mvc.perform(post("/shipper/orders/12/delivering")).andExpect(status().isNoContent());
        mvc.perform(post("/shipper/orders/12/delivered")).andExpect(status().isNoContent());
        mvc.perform(post("/shipper/orders/12/delivered").param("codCollected", "true")).andExpect(status().isNoContent());
        verify(delivery).markPickedUp(12, 7);
        verify(delivery).startDelivering(12, 7);
        verify(delivery).markDelivered(12, 7, false);
        verify(delivery).markDelivered(12, 7, true);
    }
}
