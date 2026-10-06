package com.fu.SWP391_BetaFruit.dto.request;

import jakarta.persistence.Column;

public class AddressRequest {
    private String receiverName;
    private String receiverPhone;
    private String addressLine;
    private String city;
    private String district;
    private String ward;
    private Boolean isDefault;
}
