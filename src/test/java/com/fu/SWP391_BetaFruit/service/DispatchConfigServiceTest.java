package com.fu.SWP391_BetaFruit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DispatchConfigServiceTest {

    private DispatchConfigService configService;

    @BeforeEach
    void setUp() {
        configService = new DispatchConfigService();
    }

    @Test
    @DisplayName("Giá trị mặc định ban đầu chính xác")
    void defaultValues_AreCorrect() {
        assertEquals(DispatchConfigService.DEFAULT_MAX_ONGOING_ORDERS, configService.getMaxOngoingOrders());
        assertEquals(DispatchConfigService.DEFAULT_COD_CEILING_LIMIT, configService.getCodCeilingLimit());
        assertEquals(DispatchConfigService.DEFAULT_PICKUP_TIMEOUT_MINUTES, configService.getPickupTimeoutMinutes());
    }

    @Test
    @DisplayName("Cập nhật thông số hợp lệ")
    void updateParameters_ValidValues_UpdatesSuccessfully() {
        configService.setMaxOngoingOrders(10);
        configService.setCodCeilingLimit(new BigDecimal("5000000"));
        configService.setPickupTimeoutMinutes(30);

        assertEquals(10, configService.getMaxOngoingOrders());
        assertEquals(new BigDecimal("5000000"), configService.getCodCeilingLimit());
        assertEquals(30, configService.getPickupTimeoutMinutes());
    }

    @Test
    @DisplayName("Bỏ qua giá trị không hợp lệ (âm hoặc null)")
    void updateParameters_InvalidValues_Ignored() {
        configService.setMaxOngoingOrders(-5);
        configService.setCodCeilingLimit(null);
        configService.setPickupTimeoutMinutes(0);

        assertEquals(DispatchConfigService.DEFAULT_MAX_ONGOING_ORDERS, configService.getMaxOngoingOrders());
        assertEquals(DispatchConfigService.DEFAULT_COD_CEILING_LIMIT, configService.getCodCeilingLimit());
        assertEquals(DispatchConfigService.DEFAULT_PICKUP_TIMEOUT_MINUTES, configService.getPickupTimeoutMinutes());
    }

    @Test
    @DisplayName("Khôi phục cài đặt về mặc định thành công")
    void resetToDefaults_RestoresDefaultValues() {
        configService.setMaxOngoingOrders(8);
        configService.setCodCeilingLimit(new BigDecimal("10000000"));
        configService.setPickupTimeoutMinutes(45);

        configService.resetToDefaults();

        assertEquals(DispatchConfigService.DEFAULT_MAX_ONGOING_ORDERS, configService.getMaxOngoingOrders());
        assertEquals(DispatchConfigService.DEFAULT_COD_CEILING_LIMIT, configService.getCodCeilingLimit());
        assertEquals(DispatchConfigService.DEFAULT_PICKUP_TIMEOUT_MINUTES, configService.getPickupTimeoutMinutes());
    }
}
