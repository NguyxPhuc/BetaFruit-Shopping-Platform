package com.fu.SWP391_BetaFruit.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record DispatchShipperCandidateResponse(
        Integer shipperId,
        String fullName,
        String phone,
        long ongoingOrdersCount,
        BigDecimal codDebt,
        boolean isOverloaded,
        boolean isCodWarning
) {}
