-- =========================================================================================
-- HỆ THỐNG GÓI VOUCHER ĐẶC QUYỀN HỘI VIÊN CHUẨN SHOPEE REWARDS (DYNAMIC TIER COUPON BUNDLE)
-- Dự án: SWP391_BetaFruit | Phân hệ: FE-09-tiered-membership-management (dangquangkha)
-- =========================================================================================

-- 1. TẠO BẢNG TRUNG GIAN TIERCOUPON (N-N RELATIONSHIP)
IF OBJECT_ID('TierCoupon', 'U') IS NULL
BEGIN
    CREATE TABLE TierCoupon (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        TierId INT NOT NULL,
        CouponId INT NOT NULL,
        CONSTRAINT FK_TierCoupon_Tier FOREIGN KEY (TierId) REFERENCES MembershipTier(TierId) ON DELETE CASCADE,
        CONSTRAINT FK_TierCoupon_Coupon FOREIGN KEY (CouponId) REFERENCES Coupon(CouponId) ON DELETE CASCADE,
        CONSTRAINT UQ_Tier_Coupon UNIQUE (TierId, CouponId)
    );
    PRINT N'Đã tạo bảng TierCoupon thành công.';
END
ELSE
BEGIN
    PRINT N'Bảng TierCoupon đã tồn tại.';
END
GO

-- 2. KHỞI TẠO MẪU GÓI VOUCHER CHO TỪNG HẠNG (CHỈ ÁP DỤNG MÃ TOÀN SÀN - ShopId IS NULL)
-- Hạng Bạc: Gói gồm SILVER_VIP, BETAFRUIT10
-- Hạng Vàng: Gói gồm GOLD_VIP, BETAFRUIT10
-- Hạng Kim Cương: Gói gồm DIAMOND_VIP, FREESHIP50, BETAFRUIT10

INSERT INTO TierCoupon (TierId, CouponId)
SELECT t.TierId, c.CouponId
FROM MembershipTier t
CROSS JOIN Coupon c
WHERE c.ShopId IS NULL
  AND (
       (t.TierName LIKE N'%Bạc%' AND c.CouponCode IN ('SILVER_VIP', 'BETAFRUIT10'))
    OR (t.TierName LIKE N'%Vàng%' AND c.CouponCode IN ('GOLD_VIP', 'BETAFRUIT10'))
    OR (t.TierName LIKE N'%Kim Cương%' AND c.CouponCode IN ('DIAMOND_VIP', 'FREESHIP50', 'BETAFRUIT10'))
  )
  AND NOT EXISTS (
      SELECT 1 FROM TierCoupon tc WHERE tc.TierId = t.TierId AND tc.CouponId = c.CouponId
  );
GO

-- 3. TRUY VẤN KIỂM TRA
SELECT 
    t.TierId,
    t.TierName,
    t.DiscountPercent,
    c.CouponCode,
    c.DiscountType,
    c.DiscountValue,
    c.MinOrderValue,
    c.CouponCategory
FROM TierCoupon tc
JOIN MembershipTier t ON tc.TierId = t.TierId
JOIN Coupon c ON tc.CouponId = c.CouponId
ORDER BY t.TierId, c.CouponCode;
GO
