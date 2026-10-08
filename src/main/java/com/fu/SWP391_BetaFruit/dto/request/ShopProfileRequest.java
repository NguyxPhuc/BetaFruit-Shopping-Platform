package com.fu.SWP391_BetaFruit.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopProfileRequest {
    private String shopName;
    private String shopDescription;
    private String email;
    private String phone;
    private String bankName;
    private String bankAccountNumber;
    private String accountName;
}
