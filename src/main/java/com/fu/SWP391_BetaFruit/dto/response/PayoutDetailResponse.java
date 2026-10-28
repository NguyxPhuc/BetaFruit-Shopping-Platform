package com.fu.SWP391_BetaFruit.dto.response;

import com.fu.SWP391_BetaFruit.enums.PayoutStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayoutDetailResponse {
    private String bankName;
    private String bankAccountNumber;
    private String accountName;
    private BigDecimal requestedAmount;
    private PayoutStatus status;
    private LocalDateTime createdAt;
    private String processesBy;
}
