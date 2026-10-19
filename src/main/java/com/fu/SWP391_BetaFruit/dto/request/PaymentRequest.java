package com.fu.SWP391_BetaFruit.dto.request;

import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private Integer referenceId;      // ID của đơn hàng
    private BigDecimal amount;        // Số tiền cần thanh toán
    private PaymentMethod paymentMethod;     // COD, VNPAY, PAYOS
    private String clientIpAddress;   // Đã bóc tách IP ra khỏi HttpServletRequest
    private String contextPath;       // Để VNPay biết đường dẫn Web mà Return về
}
