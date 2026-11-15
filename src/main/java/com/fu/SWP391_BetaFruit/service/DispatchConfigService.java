package com.fu.SWP391_BetaFruit.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service lưu trữ và quản lý cấu hình thông số điều phối tập trung (Dispatch Settings).
 * Cho phép Operations Staff điều chỉnh linh hoạt trên UI theo thời gian thực:
 * - Số đơn dở dang tối đa / Shipper (Cân bằng tải)
 * - Trần nợ COD tối đa / Shipper (Kiểm soát rủi ro)
 * - Thời gian tối đa chờ tài xế đến lấy hàng (Timeout)
 */
@Service
public class DispatchConfigService {

    public static final int DEFAULT_MAX_ONGOING_ORDERS = 5;
    public static final BigDecimal DEFAULT_COD_CEILING_LIMIT = new BigDecimal("3000000");
    public static final int DEFAULT_PICKUP_TIMEOUT_MINUTES = 15;

    private volatile int maxOngoingOrders = DEFAULT_MAX_ONGOING_ORDERS;
    private volatile BigDecimal codCeilingLimit = DEFAULT_COD_CEILING_LIMIT;
    private volatile int pickupTimeoutMinutes = DEFAULT_PICKUP_TIMEOUT_MINUTES;

    public int getMaxOngoingOrders() {
        return maxOngoingOrders;
    }

    public void setMaxOngoingOrders(int maxOngoingOrders) {
        if (maxOngoingOrders > 0) {
            this.maxOngoingOrders = maxOngoingOrders;
        }
    }

    public BigDecimal getCodCeilingLimit() {
        return codCeilingLimit;
    }

    public void setCodCeilingLimit(BigDecimal codCeilingLimit) {
        if (codCeilingLimit != null && codCeilingLimit.compareTo(BigDecimal.ZERO) >= 0) {
            this.codCeilingLimit = codCeilingLimit;
        }
    }

    public int getPickupTimeoutMinutes() {
        return pickupTimeoutMinutes;
    }

    public void setPickupTimeoutMinutes(int pickupTimeoutMinutes) {
        if (pickupTimeoutMinutes > 0) {
            this.pickupTimeoutMinutes = pickupTimeoutMinutes;
        }
    }

    public void resetToDefaults() {
        this.maxOngoingOrders = DEFAULT_MAX_ONGOING_ORDERS;
        this.codCeilingLimit = DEFAULT_COD_CEILING_LIMIT;
        this.pickupTimeoutMinutes = DEFAULT_PICKUP_TIMEOUT_MINUTES;
    }
}
