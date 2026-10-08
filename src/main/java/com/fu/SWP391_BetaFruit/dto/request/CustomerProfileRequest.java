package com.fu.SWP391_BetaFruit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class CustomerProfileRequest {
    private String username;
    private String fullName;
    private String email;
    private String phone;
}
