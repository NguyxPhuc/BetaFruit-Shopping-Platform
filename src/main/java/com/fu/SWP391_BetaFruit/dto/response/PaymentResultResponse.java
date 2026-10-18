package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResultResponse {
    private boolean success;
    private Integer orderId;
    private String orderCode;
    private String transactionRef;
    private String gatewayTransactionNo;
    private String bankCode;
    private BigDecimal amount;
    private String formattedAmount;
    private String payDate;
    private String responseCode;
    private String message;
    private String failureReason;
    private String customerName;
    private String customerPhone;
    private String shippingAddress;
    private String paymentMethod;
}
