package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.repository.ShipperRepository;
import com.fu.SWP391_BetaFruit.service.CODCollectionService;
import com.fu.SWP391_BetaFruit.service.SessionUserService;
import com.fu.SWP391_BetaFruit.service.impl.DelivererOrderDetailService;
import com.fu.SWP391_BetaFruit.service.impl.OrderDeliveryService;
import com.fu.SWP391_BetaFruit.service.impl.ShipperOrderQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử ShipperOrderController - Xem Ví nợ & Lịch sử COD")
class ShipperOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShipperOrderQueryService orders;

    @Mock
    private SessionUserService sessions;

    @Mock
    private DelivererOrderDetailService details;

    @Mock
    private OrderDeliveryService delivery;

    @Mock
    private ShipperRepository shipperRepository;

    @Mock
    private CODCollectionService codCollectionService;

    @InjectMocks
    private ShipperOrderController shipperOrderController;

    private User shipperUser;
    private Shipper shipper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(shipperOrderController).build();

        shipperUser = new User();
        shipperUser.setUserId(6);
        shipperUser.setFullName("Đỗ Thành Nam");
        shipperUser.setAvatarUrl("https://example.com/avatar.png");

        shipper = new Shipper();
        shipper.setUserId(6);
        shipper.setUser(shipperUser);
        shipper.setCodDebt(new BigDecimal("450000"));
    }

    @Test
    @DisplayName("Chưa đăng nhập truy cập /shipper/orders/cod-history -> Chuyển hướng tới /auth/login")
    void codHistory_NotLoggedIn_RedirectsToLogin() throws Exception {
        when(sessions.findCurrentUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/shipper/orders/cod-history"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    @DisplayName("Đã đăng nhập truy cập /shipper/orders/cod-history -> Trả về view shipper/cod-history và đầy đủ Model attributes")
    void codHistory_LoggedIn_ReturnsCodHistoryViewWithAttributes() throws Exception {
        when(sessions.findCurrentUser(any())).thenReturn(Optional.of(shipperUser));
        when(shipperRepository.findById(6)).thenReturn(Optional.of(shipper));
        when(codCollectionService.getUnremittedAssignmentsByShipper(6)).thenReturn(Collections.emptyList());
        when(codCollectionService.getCollectionsByShipper(6)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/shipper/orders/cod-history"))
                .andExpect(status().isOk())
                .andExpect(view().name("shipper/cod-history"))
                .andExpect(model().attribute("delivererName", "Đỗ Thành Nam"))
                .andExpect(model().attribute("codDebt", new BigDecimal("450000")))
                .andExpect(model().attribute("codCeiling", new BigDecimal("3000000")))
                .andExpect(model().attribute("remainingQuota", new BigDecimal("2550000")))
                .andExpect(model().attribute("availableQuota", new BigDecimal("2550000")))
                .andExpect(model().attribute("isCodLimitReached", false))
                .andExpect(model().attributeExists("pendingAssignments"))
                .andExpect(model().attributeExists("unremittedAssignments"))
                .andExpect(model().attributeExists("pastCollections"));
    }
}
