package com.fu.SWP391_BetaFruit.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleNoResourceFound trả về HTTP 404 Not Found và không có body khi thiếu tài nguyên tĩnh (favicon, css, js)")
    void handleNoResourceFound_Returns404NotFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/favicon.ico", "No static resource favicon.ico");

        ResponseEntity<Void> response = handler.handleNoResourceFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }
}
