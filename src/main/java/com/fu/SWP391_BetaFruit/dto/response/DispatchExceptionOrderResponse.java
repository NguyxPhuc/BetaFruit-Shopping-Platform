package com.fu.SWP391_BetaFruit.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record DispatchExceptionOrderResponse(
        Integer orderId,
        String orderCode,
        String shopName,
        String customerName,
        String shippingAddress,
        BigDecimal finalAmount,
        String paymentMethod,
        String orderStatus,
        String orderStatusLabel,
        Integer currentShipperId,
        String currentShipperName,
        String exceptionType, // UNASSIGNED, INCIDENT_REPORTED, PICKUP_TIMEOUT
        String incidentDetails,
        LocalDateTime incidentTime
) {}
