package com.fu.SWP391_BetaFruit.dto.request;

import com.fu.SWP391_BetaFruit.enums.PayoutStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessPayoutRequest {
    private String note;
    private PayoutStatus payoutStatus;
}
