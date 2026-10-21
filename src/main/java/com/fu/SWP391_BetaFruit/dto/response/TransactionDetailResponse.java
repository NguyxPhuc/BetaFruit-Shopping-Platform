package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDetailResponse {
    private String transactionRef;
    private String gateway;
    private LocalDateTime createdAt;
    private BigDecimal amount;
    private String status;
    private Integer orderId;
    private String orderCode;
    private String orderStatus;
    private String customerName;
    private String shippingAddress;
}
