package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierVoucherResponse {
    private String couponCode;
    private String title;              // vd: "Freeship 40.000 ₫" hoặc "Giảm 50.000 ₫"
    private String description;        // vd: "Voucher vận chuyển toàn sàn"
    private String minOrderValueText;  // vd: "Đơn tối thiểu 200.000 ₫"
    private String maxDiscountText;    // vd: "Tối đa 150.000 ₫"
    private String expiryDateText;     // vd: "HSD: 31/12/2026"
    private String badgeType;          // "FREESHIP" | "DISCOUNT"
    private String targetTierName;     // "Hạng Bạc" | "Hạng Vàng" | "Hạng Kim Cương"
    private boolean isAvailable;       // true nếu đang hoạt động
}
