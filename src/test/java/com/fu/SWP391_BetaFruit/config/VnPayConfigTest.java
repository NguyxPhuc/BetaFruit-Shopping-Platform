package com.fu.SWP391_BetaFruit.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VnPayConfigTest {
    VnPayConfig config = new VnPayConfig();

    VnPayConfigTest() {
        ReflectionTestUtils.setField(config, "tmnCode", "TESTCODE");
        ReflectionTestUtils.setField(config, "hashSecret", "test-secret-only");
        ReflectionTestUtils.setField(config, "vnpPayUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(config, "returnUrl", "/payment/vnpay-return");
    }

    Map<String, String> params(String url) {
        Map<String, String> params = new HashMap<>();
        for (String pair : URI.create(url).getRawQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            params.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return params;
    }

    @Test void signedUrlHasCallbackAmountAndOriginalSystemTimeFormatting() throws Exception {
        var params = params(config.buildPaymentUrl("VNPAY_101_test", 12345600L, "127.0.0.1", "http://localhost:8080"));
        assertEquals("http://localhost:8080/payment/vnpay-return", params.get("vnp_ReturnUrl"));
        assertEquals("12345600", params.get("vnp_Amount"));
        assertEquals("other", params.get("vnp_OrderType"));
        assertTrue(config.verifySignature(params));
        var format = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        var created = LocalDateTime.parse(params.get("vnp_CreateDate"), format);
        var expires = LocalDateTime.parse(params.get("vnp_ExpireDate"), format);
        assertEquals(15, ChronoUnit.MINUTES.between(created, expires));
        assertTrue(Math.abs(ChronoUnit.SECONDS.between(created, LocalDateTime.now(ZoneId.systemDefault()))) < 10);
        params.put("vnp_Amount", "1");
        assertFalse(config.verifySignature(params));
    }

}
