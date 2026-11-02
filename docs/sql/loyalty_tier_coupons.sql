-- ==============================================================================
-- BẢNG MÃ GIẢM GIÁ ĐẶC QUYỀN HỘI VIÊN (SHOPEE REWARDS EXCLUSIVE VOUCHERS)
-- Tận dụng 100% bảng [Coupon] của PhucND, không làm thay đổi Schema CSDL
-- ==============================================================================

-- 1. VOUCHER DÀNH CHO HẠNG BẠC (SILVER)
IF NOT EXISTS (SELECT 1 FROM [Coupon] WHERE [CouponCode] = 'SILVER_FREESHIP')
BEGIN
    INSERT INTO [Coupon] (
        [ShopId], [CouponCode], [CouponCategory], [DiscountType], 
        [DiscountValue], [MinOrderValue], [MaxDiscountAmount], 
        [UsageLimit], [UsedCount], [IsActive], [StartDate], [EndDate]
    ) VALUES (
        NULL, 'SILVER_FREESHIP', 'SHIPPING', 'FIXED_AMOUNT', 
        30000.00, 150000.00, NULL, 
        1000, 0, 1, DATEADD(DAY, -1, GETDATE()), DATEADD(MONTH, 6, GETDATE())
    );
END;

-- 2. VOUCHER DÀNH CHO HẠNG VÀNG (GOLD)
IF NOT EXISTS (SELECT 1 FROM [Coupon] WHERE [CouponCode] = 'GOLD_FREESHIP')
BEGIN
    INSERT INTO [Coupon] (
        [ShopId], [CouponCode], [CouponCategory], [DiscountType], 
        [DiscountValue], [MinOrderValue], [MaxDiscountAmount], 
        [UsageLimit], [UsedCount], [IsActive], [StartDate], [EndDate]
    ) VALUES (
        NULL, 'GOLD_FREESHIP', 'SHIPPING', 'FIXED_AMOUNT', 
        40000.00, 200000.00, NULL, 
        500, 0, 1, DATEADD(DAY, -1, GETDATE()), DATEADD(MONTH, 6, GETDATE())
    );
END;

IF NOT EXISTS (SELECT 1 FROM [Coupon] WHERE [CouponCode] = 'GOLD_DISCOUNT50K')
BEGIN
    INSERT INTO [Coupon] (
        [ShopId], [CouponCode], [CouponCategory], [DiscountType], 
        [DiscountValue], [MinOrderValue], [MaxDiscountAmount], 
        [UsageLimit], [UsedCount], [IsActive], [StartDate], [EndDate]
    ) VALUES (
        NULL, 'GOLD_DISCOUNT50K', 'PRODUCT', 'FIXED_AMOUNT', 
        50000.00, 300000.00, NULL, 
        500, 0, 1, DATEADD(DAY, -1, GETDATE()), DATEADD(MONTH, 6, GETDATE())
    );
END;

-- 3. VOUCHER DÀNH CHO HẠNG KIM CƯƠNG (DIAMOND)
IF NOT EXISTS (SELECT 1 FROM [Coupon] WHERE [CouponCode] = 'DIAMOND_FREESHIP')
BEGIN
    INSERT INTO [Coupon] (
        [ShopId], [CouponCode], [CouponCategory], [DiscountType], 
        [DiscountValue], [MinOrderValue], [MaxDiscountAmount], 
        [UsageLimit], [UsedCount], [IsActive], [StartDate], [EndDate]
    ) VALUES (
        NULL, 'DIAMOND_FREESHIP', 'SHIPPING', 'FIXED_AMOUNT', 
        50000.00, 150000.00, NULL, 
        200, 0, 1, DATEADD(DAY, -1, GETDATE()), DATEADD(MONTH, 6, GETDATE())
    );
END;

IF NOT EXISTS (SELECT 1 FROM [Coupon] WHERE [CouponCode] = 'DIAMOND_VIP100K')
BEGIN
    INSERT INTO [Coupon] (
        [ShopId], [CouponCode], [CouponCategory], [DiscountType], 
        [DiscountValue], [MinOrderValue], [MaxDiscountAmount], 
        [UsageLimit], [UsedCount], [IsActive], [StartDate], [EndDate]
    ) VALUES (
        NULL, 'DIAMOND_VIP100K', 'PRODUCT', 'FIXED_AMOUNT', 
        100000.00, 500000.00, NULL, 
        200, 0, 1, DATEADD(DAY, -1, GETDATE()), DATEADD(MONTH, 6, GETDATE())
    );
END;

IF NOT EXISTS (SELECT 1 FROM [Coupon] WHERE [CouponCode] = 'DIAMOND_PERCENT15')
BEGIN
    INSERT INTO [Coupon] (
        [ShopId], [CouponCode], [CouponCategory], [DiscountType], 
        [DiscountValue], [MinOrderValue], [MaxDiscountAmount], 
        [UsageLimit], [UsedCount], [IsActive], [StartDate], [EndDate]
    ) VALUES (
        NULL, 'DIAMOND_PERCENT15', 'PRODUCT', 'PERCENT', 
        15.00, 600000.00, 150000.00, 
        200, 0, 1, DATEADD(DAY, -1, GETDATE()), DATEADD(MONTH, 6, GETDATE())
    );
END;
