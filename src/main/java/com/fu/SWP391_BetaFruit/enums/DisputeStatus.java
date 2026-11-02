package com.fu.SWP391_BetaFruit.enums;

import lombok.Getter;

@Getter
public enum DisputeStatus {
    PENDING("Chờ kiểm duyệt"),
    UNDER_REVIEW("Đang thụ lý"),
    APPROVED_HIDDEN("Đã duyệt ẩn"),
    REJECTED("Đã từ chối"),
    REJECTED_WITH_WARNING("Từ chối & Cảnh cáo");

    private final String displayName;

    DisputeStatus(String displayName) {
        this.displayName = displayName;
    }
}
