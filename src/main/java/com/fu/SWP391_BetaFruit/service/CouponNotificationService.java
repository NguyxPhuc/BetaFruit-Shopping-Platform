package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.Coupon;

public interface CouponNotificationService {
    /**
     * Bắn thông báo có mã khuyến mãi mới (New Promotion) cho toàn bộ khách hàng
     */
    void notifyCustomersNewCoupon(Coupon coupon);

    /**
     * Quét và gửi thông báo nhắc nhở các mã giảm giá sắp hết hạn trong vòng 48h tới (Expiring Vouchers)
     * @return Số lượng thông báo đã gửi
     */
    int scanAndNotifyExpiringCoupons();
}

