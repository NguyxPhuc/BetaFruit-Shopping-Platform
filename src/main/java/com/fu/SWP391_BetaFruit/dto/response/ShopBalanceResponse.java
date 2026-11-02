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
public class ShopBalanceResponse {
    private BigDecimal availableBalance;
    private BigDecimal pendingPayoutAmount;
    private BigDecimal totalWithdrawnAmount;
    private String bankName;
    private String bankAccountNumber;
    private String accountName;
}
