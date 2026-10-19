package com.fu.SWP391_BetaFruit.dto.response;

import java.math.BigDecimal;

/** All outstanding delivered COD orders, including previous days and settled shop orders. */
public record ShipperCODSummaryResponse(long pendingOrderCount, BigDecimal amountToRemit) {}
