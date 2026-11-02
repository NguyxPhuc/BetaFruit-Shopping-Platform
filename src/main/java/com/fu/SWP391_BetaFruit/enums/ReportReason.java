package com.fu.SWP391_BetaFruit.enums;

import lombok.Getter;

@Getter
public enum ReportReason {
    INAPPROPRIATE_LANGUAGE("Ngôn từ thô tục, xúc phạm hoặc đe dọa"),
    FALSE_INFORMATION("Thông tin sai sự thật, vu khống gian lận"),
    IRRELEVANT_CONTENT("Nội dung không liên quan / Quảng cáo ngoài"),
    COMPETITOR_SABOTAGE("Cạnh tranh không lành mạnh / Đơn ảo"),
    WRONG_PRODUCT("Đánh giá nhầm mặt hàng hoặc nhầm gian hàng");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }
}
