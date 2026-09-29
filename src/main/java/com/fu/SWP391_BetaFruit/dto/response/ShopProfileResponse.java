package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopProfileResponse {
    private String shopName;
    private String shopDescription;
    private String email;
    private String phone;
    private String bankName;
    private String bankAccountNumber;
    private String accountName;
}
