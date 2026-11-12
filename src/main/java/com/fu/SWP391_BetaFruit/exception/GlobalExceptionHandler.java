package com.fu.SWP391_BetaFruit.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidPagination.class)
    public ResponseEntity<String> handleInvalidPagination(InvalidPagination exception) {
        return ResponseEntity.badRequest()
                .contentType(new org.springframework.http.MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                .body(exception.getReason());
    }

    public static class InvalidPagination extends ResponseStatusException {
        public InvalidPagination(String message) {
            super(org.springframework.http.HttpStatus.BAD_REQUEST, message);
        }
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException ex) {
        return new ResponseEntity<>(ex.getBody(), ex.getHeaders(), ex.getStatusCode());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error for {} {}", request.getMethod(), request.getRequestURI(), ex);
        String accept = request.getHeader("Accept");
        if ((accept != null && accept.contains("application/json")) || request.getRequestURI().contains("/loyalty-details")) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Lỗi hệ thống"));
        }
        // Render directly: redirecting to the failing page creates an endless loop.
        ModelAndView error = new ModelAndView("error/500");
        error.setStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        return error;
    }
}
