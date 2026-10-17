package com.fu.SWP391_BetaFruit.enums;

public enum RoleName {
    ADMIN(true, "Quản trị viên tối cao"),
    CUSTOMER(true, "Khách hàng mua hoa quả"),
    SHOP_OWNER(true, "Chủ gian hàng hoa quả"),
    DELIVERY_STAFF(false, "Nhân viên giao nhận (Đang phát triển)"),
    STAFF(true, "Nhân viên vận hành & kiểm duyệt");

    private boolean active;
    private final String description;

    RoleName(boolean active, String description) {
        this.active = active;
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isHidden() {
        return !active;
    }

    public String getDescription() {
        return description;
    }
}
