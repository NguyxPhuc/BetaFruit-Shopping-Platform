package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Coupon;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.DiscountType;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.enums.RoleName;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import com.fu.SWP391_BetaFruit.repository.CouponRepository;
import com.fu.SWP391_BetaFruit.repository.NotificationRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.CouponNotificationService;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponNotificationServiceImpl implements CouponNotificationService {

    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notifyCustomersNewCoupon(Coupon coupon) {
        if (coupon == null || !Boolean.TRUE.equals(coupon.getIsActive())) {
            return;
        }

        List<User> customers = userRepository.findUsersByRoleNameAndStatus(RoleName.CUSTOMER, UserStatus.ACTIVE);
        if (customers == null || customers.isEmpty()) {
            log.info("Không tìm thấy khách hàng nào để gửi thông báo coupon mới.");
            return;
        }

        String discountStr = formatDiscount(coupon);
        String minOrderStr = formatMinOrder(coupon);

        String title;
        String message;
        if (coupon.getShop() == null) {
            // Mã giảm giá toàn sàn từ Admin
            title = "🎉 Ưu Đãi Mới Toàn Sàn: " + coupon.getCouponCode();
            message = "BetaFruit vừa phát hành mã ưu đãi [" + coupon.getCouponCode() + "] giảm " + discountStr + minOrderStr + ". Mua sắm hoa quả tươi ngon ngay hôm nay!";
        } else {
            // Mã giảm giá từ Shop Owner
            String shopName = coupon.getShop().getShopName() != null ? coupon.getShop().getShopName() : "Cửa hàng";
            title = "🎁 Ưu Đãi Mới Từ " + shopName + ": " + coupon.getCouponCode();
            message = "Gian hàng [" + shopName + "] tặng bạn mã giảm giá [" + coupon.getCouponCode() + "] giảm " + discountStr + minOrderStr + ". Áp dụng ngay kẻo lỡ!";
        }

        for (User customer : customers) {
            try {
                notificationService.createBaseNotification(
                        customer,
                        title,
                        message,
                        NotificationType.PROMOTION,
                        "/customer/products"
                );
            } catch (Exception ex) {
                log.warn("Lỗi gửi thông báo coupon mới tới User #{}: {}", customer.getUserId(), ex.getMessage());
            }
        }
        log.info("Đã phát thông báo coupon mới '{}' tới {} khách hàng.", coupon.getCouponCode(), customers.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanAndNotifyExpiringCoupons() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plusHours(48); // Mã hết hạn trong vòng 48h tới
        List<Coupon> expiringCoupons = couponRepository.findExpiringCoupons(now, cutoff);

        if (expiringCoupons == null || expiringCoupons.isEmpty()) {
            log.info("Không có mã giảm giá nào sắp hết hạn trong 48 giờ tới.");
            return 0;
        }

        List<User> customers = userRepository.findUsersByRoleNameAndStatus(RoleName.CUSTOMER, UserStatus.ACTIVE);
        if (customers == null || customers.isEmpty()) {
            return 0;
        }

        int sentCount = 0;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
        LocalDateTime sinceYesterday = now.minusHours(24);

        for (Coupon coupon : expiringCoupons) {
            String discountStr = formatDiscount(coupon);
            String expiryStr = coupon.getEndDate() != null ? coupon.getEndDate().format(dtf) : "sắp tới";
            String title = "⏰ Mã Giảm Giá " + coupon.getCouponCode() + " Sắp Hết Hạn!";
            String message = "Mã ưu đãi [" + coupon.getCouponCode() + "] (giảm " + discountStr + ") sẽ hết hạn vào lúc " + expiryStr + ". Đừng bỏ lỡ ưu đãi, hãy chốt đơn ngay!";

            for (User customer : customers) {
                // Kiểm tra chống spam: không gửi lại thông báo cùng mã trong vòng 24h
                boolean alreadySent = notificationRepository.hasRecentNotification(
                        customer.getUserId(),
                        NotificationType.PROMOTION,
                        title,
                        sinceYesterday
                );
                if (alreadySent) {
                    continue;
                }

                try {
                    notificationService.createBaseNotification(
                            customer,
                            title,
                            message,
                            NotificationType.PROMOTION,
                            "/customer/products"
                    );
                    sentCount++;
                } catch (Exception ex) {
                    log.warn("Lỗi gửi thông báo mã sắp hết hạn tới User #{}: {}", customer.getUserId(), ex.getMessage());
                }
            }
        }

        log.info("Quét mã sắp hết hạn hoàn tất: đã gửi {} thông báo tới khách hàng.", sentCount);
        return sentCount;
    }

    /**
     * Tự động quét và cảnh báo mã sắp hết hạn định kỳ mỗi ngày lúc 8:00 sáng
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduledExpiringCouponScan() {
        log.info("Khởi chạy tác vụ định kỳ quét mã giảm giá sắp hết hạn...");
        scanAndNotifyExpiringCoupons();
    }

    private String formatDiscount(Coupon coupon) {
        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            String pct = coupon.getDiscountValue() != null ? coupon.getDiscountValue().stripTrailingZeros().toPlainString() + "%" : "0%";
            if (coupon.getMaxDiscountAmount() != null && coupon.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                pct += " (tối đa " + String.format("%,d ₫", coupon.getMaxDiscountAmount().longValue()) + ")";
            }
            return pct;
        } else {
            return coupon.getDiscountValue() != null ? String.format("%,d ₫", coupon.getDiscountValue().longValue()) : "0 ₫";
        }
    }

    private String formatMinOrder(Coupon coupon) {
        if (coupon.getMinOrderValue() != null && coupon.getMinOrderValue().compareTo(BigDecimal.ZERO) > 0) {
            return " cho đơn từ " + String.format("%,d ₫", coupon.getMinOrderValue().longValue());
        }
        return "";
    }
}

