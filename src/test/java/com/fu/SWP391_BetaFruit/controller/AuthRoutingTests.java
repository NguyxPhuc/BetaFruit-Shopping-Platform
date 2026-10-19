package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.LoginResponse;
import com.fu.SWP391_BetaFruit.enums.RoleName;
import com.fu.SWP391_BetaFruit.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthRoutingTests {
    @Test void loginSavesIntegerIdentityAndRoutesDeliveryStaffToOrders() throws Exception {
        var auth = mock(AuthService.class);
        when(auth.login(any())).thenReturn(new LoginResponse(7, "driver", "Driver", List.of(RoleName.DELIVERY_STAFF)));
        var controller = new AuthController(); ReflectionTestUtils.setField(controller, "authService", auth);
        var mvc = standaloneSetup(controller).build();
        mvc.perform(post("/auth/login").session(new MockHttpSession()).param("email", "driver@example.test").param("password", "test"))
                .andExpect(redirectedUrl("/shipper/orders"))
                .andExpect(request().sessionAttribute("LOGGED_IN_USER_ID", 7))
                .andExpect(request().sessionAttribute("LOGGED_IN_FULLNAME", "Driver"))
                .andExpect(request().sessionAttribute("LOGGED_IN_ROLES", List.of(RoleName.DELIVERY_STAFF)));
    }
}
