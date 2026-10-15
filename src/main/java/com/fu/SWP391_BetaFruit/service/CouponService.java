package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.CouponRequest;
import com.fu.SWP391_BetaFruit.dto.response.CouponUsageResponse;
import com.fu.SWP391_BetaFruit.entity.Coupon;

import java.util.Map;

public interface CouponService {

    // ================= SHOP OWNER METHODS =================
    Map<String, Object> getShopCouponsPageData(Integer userId, String keyword, String status, int page, int size);

    Coupon getShopCouponById(Integer userId, Integer couponId);

    CouponRequest getShopCouponRequestForEdit(Integer userId, Integer couponId);

    Coupon createShopCoupon(Integer userId, CouponRequest request);

    Coupon updateShopCoupon(Integer userId, Integer couponId, CouponRequest request);

    boolean toggleShopCouponStatus(Integer userId, Integer couponId);

    CouponUsageResponse getShopCouponUsage(Integer userId, Integer couponId, int page, int size);

    // ================= ADMIN METHODS =================
    Map<String, Object> getAdminCouponsPageData(String keyword, String scope, String status, int page, int size);

    Coupon getAdminCouponById(Integer couponId);

    CouponRequest getAdminCouponRequestForEdit(Integer couponId);

    Coupon createAdminCoupon(CouponRequest request);

    Coupon updateAdminCoupon(Integer couponId, CouponRequest request);

    boolean toggleAdminCouponStatus(Integer couponId);

    CouponUsageResponse getAdminCouponUsage(Integer couponId, int page, int size);
}
