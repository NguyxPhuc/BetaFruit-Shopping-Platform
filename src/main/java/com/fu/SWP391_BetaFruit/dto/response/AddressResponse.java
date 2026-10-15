package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressResponse {
    private Integer addressId;
    private String receiverName;
    private String receiverPhone;
    private String addressLine;
    private String city;
    private String district;
    private String ward;
    private Boolean isDefault;
    private String addressType;
}
