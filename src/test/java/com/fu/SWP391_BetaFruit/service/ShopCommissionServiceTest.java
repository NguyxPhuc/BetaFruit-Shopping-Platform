package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.UpdateCommissionRequest;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.service.impl.ShopServiceImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopCommissionServiceTest {

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private ShopServiceImpl shopService;

    private Shop mockShop;
    private Validator validator;

    @BeforeEach
    void setUp() {
        mockShop = new Shop();
        mockShop.setShopId(1);
        mockShop.setShopName("Vườn Dâu Mộc Châu");
        mockShop.setCommissionRate(new BigDecimal("5.00"));

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "3.00", "5.00", "15.00", "30.00"})
    @DisplayName("Cập nhật phí sàn thành công với các tỷ lệ hợp lệ từ 0% đến 30%")
    void updateCommissionRate_Success_ValidRates(String rateStr) {
        BigDecimal rate = new BigDecimal(rateStr);
        when(shopRepository.findById(1)).thenReturn(Optional.of(mockShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(inv -> inv.getArgument(0));

        shopService.updateCommissionRate(1, rate);

        assertEquals(rate, mockShop.getCommissionRate());
        verify(shopRepository).save(mockShop);
    }

    @ParameterizedTest
    @ValueSource(strings = {"30.01", "35.00", "50.00", "100.00"})
    @DisplayName("Ném ngoại lệ khi tỷ lệ phí sàn vượt quá ngưỡng trần 30.00%")
    void updateCommissionRate_Throws_WhenRateExceeds30(String rateStr) {
        BigDecimal rate = new BigDecimal(rateStr);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                shopService.updateCommissionRate(1, rate));

        assertTrue(ex.getMessage().contains("0.00% đến 30.00%"));
        verify(shopRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "-5.00", "-10.00"})
    @DisplayName("Ném ngoại lệ khi tỷ lệ phí sàn là số âm (< 0%)")
    void updateCommissionRate_Throws_WhenRateNegative(String rateStr) {
        BigDecimal rate = new BigDecimal(rateStr);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                shopService.updateCommissionRate(1, rate));

        assertTrue(ex.getMessage().contains("0.00% đến 30.00%"));
        verify(shopRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tỷ lệ phí sàn là null")
    void updateCommissionRate_Throws_WhenRateNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                shopService.updateCommissionRate(1, null));

        assertTrue(ex.getMessage().contains("0.00% đến 30.00%"));
        verify(shopRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi không tìm thấy Shop ID")
    void updateCommissionRate_Throws_WhenShopNotFound() {
        when(shopRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                shopService.updateCommissionRate(999, new BigDecimal("5.00")));
    }

    @Test
    @DisplayName("DTO Bean Validation: Hợp lệ với các mức 0%, 5%, 30%")
    void updateCommissionRequest_Validation_Valid() {
        UpdateCommissionRequest req1 = new UpdateCommissionRequest(new BigDecimal("0.00"));
        UpdateCommissionRequest req2 = new UpdateCommissionRequest(new BigDecimal("5.00"));
        UpdateCommissionRequest req3 = new UpdateCommissionRequest(new BigDecimal("30.00"));

        assertTrue(validator.validate(req1).isEmpty());
        assertTrue(validator.validate(req2).isEmpty());
        assertTrue(validator.validate(req3).isEmpty());
    }

    @Test
    @DisplayName("DTO Bean Validation: Chặn khi vượt quá 30.00% hoặc số âm")
    void updateCommissionRequest_Validation_Invalid() {
        UpdateCommissionRequest over100 = new UpdateCommissionRequest(new BigDecimal("100.00"));
        UpdateCommissionRequest over30 = new UpdateCommissionRequest(new BigDecimal("30.01"));
        UpdateCommissionRequest negative = new UpdateCommissionRequest(new BigDecimal("-1.00"));
        UpdateCommissionRequest nullVal = new UpdateCommissionRequest(null);

        Set<ConstraintViolation<UpdateCommissionRequest>> v1 = validator.validate(over100);
        Set<ConstraintViolation<UpdateCommissionRequest>> v2 = validator.validate(over30);
        Set<ConstraintViolation<UpdateCommissionRequest>> v3 = validator.validate(negative);
        Set<ConstraintViolation<UpdateCommissionRequest>> v4 = validator.validate(nullVal);

        assertFalse(v1.isEmpty());
        assertFalse(v2.isEmpty());
        assertFalse(v3.isEmpty());
        assertFalse(v4.isEmpty());

        assertTrue(v2.stream().anyMatch(v -> v.getMessage().contains("30.00%")));
    }

    @Test
    @DisplayName("DTO Bean Validation: Chặn khi nhập quá 2 chữ số thập phân (3 chữ số)")
    void updateCommissionRequest_Validation_MaxTwoDecimals() {
        UpdateCommissionRequest tooManyDecimals = new UpdateCommissionRequest(new BigDecimal("5.123"));

        Set<ConstraintViolation<UpdateCommissionRequest>> violations = validator.validate(tooManyDecimals);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("tối đa 2 chữ số thập phân")));
    }
}
