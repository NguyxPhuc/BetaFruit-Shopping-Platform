-- ====================================================================================================
-- DỰ ÁN: BETAFRUIT SHOPPING PLATFORM (SWP391)
-- SCRIPT: BỘ DỮ LIỆU MOCK TOÀN DIỆN CHO VOUCHER ANALYTICS DASHBOARD (LEVEL 7 SCREEN)
-- Tác giả: Đặng Quang Khải (HE186918)
-- Mục đích: Nạp dữ liệu đa dạng (30 ngày, đa danh mục, đa coupon, cảnh báo thông minh, top hoa quả bán chạy)
--          để kiểm thử toàn diện giao diện Dashboard Admin (/admin/coupons/dashboard) và Shop (/shop/coupons/dashboard)
-- ====================================================================================================

SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

PRINT N'======================================================================';
PRINT N'==> BẮT ĐẦU NẠP DỮ LIỆU MOCK TEST CHO VOUCHER ANALYTICS DASHBOARD...';
PRINT N'======================================================================';

-- ----------------------------------------------------------------------------------------------------
-- 1. XÁC ĐỊNH HOẶC TẠO CÁC SHOP HỢP LỆ TRONG HỆ THỐNG
-- ----------------------------------------------------------------------------------------------------
DECLARE @ShopDalatId INT = (SELECT TOP 1 ShopId FROM Shop WHERE ShopName LIKE N'%Đà Lạt%' OR ShopId = 1 ORDER BY ShopId ASC);
DECLARE @ShopMienTayId INT = (SELECT TOP 1 ShopId FROM Shop WHERE ShopName LIKE N'%Miền Tây%' OR ShopId = 2 ORDER BY ShopId ASC);
DECLARE @ShopImportId INT = (SELECT TOP 1 ShopId FROM Shop WHERE ShopName LIKE N'%Global%' OR ShopName LIKE N'%Nhập Khẩu%' OR ShopId = 3 ORDER BY ShopId ASC);

IF @ShopDalatId IS NULL SET @ShopDalatId = (SELECT TOP 1 ShopId FROM Shop ORDER BY ShopId ASC);
IF @ShopMienTayId IS NULL SET @ShopMienTayId = @ShopDalatId;
IF @ShopImportId IS NULL SET @ShopImportId = @ShopDalatId;

DECLARE @Customer1 INT = (SELECT TOP 1 UserId FROM [User] WHERE Username = 'customer_linh' OR Username LIKE 'customer%' ORDER BY UserId ASC);
DECLARE @Customer2 INT = (SELECT TOP 1 UserId FROM [User] WHERE Username = 'customer_hoa' OR Username LIKE 'customer%' ORDER BY UserId DESC);
DECLARE @Customer3 INT = (SELECT TOP 1 UserId FROM [User] WHERE Username = 'customer_duc' OR UserId = @Customer1);

IF @Customer1 IS NULL SET @Customer1 = (SELECT TOP 1 UserId FROM [User] ORDER BY UserId ASC);
IF @Customer2 IS NULL SET @Customer2 = @Customer1;
IF @Customer3 IS NULL SET @Customer3 = @Customer1;

DECLARE @VariantDauTay INT = (SELECT TOP 1 VariantId FROM ProductVariant WHERE VariantName LIKE N'%Dâu%' OR VariantId = 1);
DECLARE @VariantSauRieng INT = (SELECT TOP 1 VariantId FROM ProductVariant WHERE VariantName LIKE N'%Sầu%' OR VariantId = 7);
DECLARE @VariantNhoMauDon INT = (SELECT TOP 1 VariantId FROM ProductVariant WHERE VariantName LIKE N'%Nho%' OR VariantId = 10);
DECLARE @VariantBoSap INT = (SELECT TOP 1 VariantId FROM ProductVariant WHERE VariantName LIKE N'%Bơ%' OR VariantId = 3);
DECLARE @VariantCherry INT = (SELECT TOP 1 VariantId FROM ProductVariant WHERE VariantName LIKE N'%Cherry%' OR VariantId = 12);
DECLARE @VariantXoaiCat INT = (SELECT TOP 1 VariantId FROM ProductVariant WHERE VariantName LIKE N'%Xoài%' OR VariantId = 8);

IF @VariantDauTay IS NULL SET @VariantDauTay = (SELECT TOP 1 VariantId FROM ProductVariant ORDER BY VariantId ASC);
IF @VariantSauRieng IS NULL SET @VariantSauRieng = @VariantDauTay;
IF @VariantNhoMauDon IS NULL SET @VariantNhoMauDon = @VariantDauTay;
IF @VariantBoSap IS NULL SET @VariantBoSap = @VariantDauTay;
IF @VariantCherry IS NULL SET @VariantCherry = @VariantDauTay;
IF @VariantXoaiCat IS NULL SET @VariantXoaiCat = @VariantDauTay;

-- ----------------------------------------------------------------------------------------------------
-- 2. TẠO HOẶC CẬP NHẬT CÁC MÃ VOUCHER ĐA DẠNG CHO KỊCH BẢN TEST
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> 1. Đồng bộ danh sách Coupon test phong phú...';

-- Coupon 1: Sàn Freeship toàn sàn (SHIPPING)
IF NOT EXISTS (SELECT 1 FROM Coupon WHERE CouponCode = 'FREESHIP30K')
BEGIN
    INSERT INTO Coupon (ShopId, CouponCode, CouponCategory, DiscountType, DiscountValue, MinOrderValue, MaxDiscountAmount, UsageLimit, UsedCount, IsActive, StartDate, EndDate)
    VALUES (NULL, 'FREESHIP30K', 'SHIPPING', 'FIXED_AMOUNT', 30000.00, 150000.00, 30000.00, 500, 48, 1, DATEADD(MONTH, -2, GETDATE()), DATEADD(MONTH, 2, GETDATE()));
END
ELSE
BEGIN
    UPDATE Coupon SET UsageLimit = 500, UsedCount = 48, IsActive = 1, CouponCategory = 'SHIPPING', ShopId = NULL WHERE CouponCode = 'FREESHIP30K';
END

-- Coupon 2: Sàn Giảm giá quả 10% (PRODUCT)
IF NOT EXISTS (SELECT 1 FROM Coupon WHERE CouponCode = 'BETAFRUIT10')
BEGIN
    INSERT INTO Coupon (ShopId, CouponCode, CouponCategory, DiscountType, DiscountValue, MinOrderValue, MaxDiscountAmount, UsageLimit, UsedCount, IsActive, StartDate, EndDate)
    VALUES (NULL, 'BETAFRUIT10', 'PRODUCT', 'PERCENT', 10.00, 200000.00, 80000.00, 300, 65, 1, DATEADD(MONTH, -2, GETDATE()), DATEADD(MONTH, 2, GETDATE()));
END
ELSE
BEGIN
    UPDATE Coupon SET UsageLimit = 300, UsedCount = 65, IsActive = 1, CouponCategory = 'PRODUCT', ShopId = NULL WHERE CouponCode = 'BETAFRUIT10';
END

-- Coupon 3: Sàn Freeship Đơn Lớn 50K (SHIPPING)
IF NOT EXISTS (SELECT 1 FROM Coupon WHERE CouponCode = 'FREESHIP50K')
BEGIN
    INSERT INTO Coupon (ShopId, CouponCode, CouponCategory, DiscountType, DiscountValue, MinOrderValue, MaxDiscountAmount, UsageLimit, UsedCount, IsActive, StartDate, EndDate)
    VALUES (NULL, 'FREESHIP50K', 'SHIPPING', 'FIXED_AMOUNT', 50000.00, 400000.00, 50000.00, 200, 32, 1, DATEADD(MONTH, -2, GETDATE()), DATEADD(MONTH, 2, GETDATE()));
END

-- Coupon 4: Shop Đà Lạt tự tài trợ 25K (PRODUCT)
IF NOT EXISTS (SELECT 1 FROM Coupon WHERE CouponCode = 'DALATFARM25K')
BEGIN
    INSERT INTO Coupon (ShopId, CouponCode, CouponCategory, DiscountType, DiscountValue, MinOrderValue, MaxDiscountAmount, UsageLimit, UsedCount, IsActive, StartDate, EndDate)
    VALUES (@ShopDalatId, 'DALATFARM25K', 'PRODUCT', 'FIXED_AMOUNT', 25000.00, 250000.00, 25000.00, 100, 22, 1, DATEADD(MONTH, -2, GETDATE()), DATEADD(MONTH, 2, GETDATE()));
END

-- Coupon 5: Shop Miền Tây tự tài trợ 8% (PRODUCT)
IF NOT EXISTS (SELECT 1 FROM Coupon WHERE CouponCode = 'MIENTAY8PCT')
BEGIN
    INSERT INTO Coupon (ShopId, CouponCode, CouponCategory, DiscountType, DiscountValue, MinOrderValue, MaxDiscountAmount, UsageLimit, UsedCount, IsActive, StartDate, EndDate)
    VALUES (@ShopMienTayId, 'MIENTAY8PCT', 'PRODUCT', 'PERCENT', 8.00, 300000.00, 50000.00, 150, 40, 1, DATEADD(MONTH, -2, GETDATE()), DATEADD(MONTH, 2, GETDATE()));
END

-- Coupon 6: Shop Import tự tài trợ 50K (PRODUCT)
IF NOT EXISTS (SELECT 1 FROM Coupon WHERE CouponCode = 'GLOBALVIP50K')
BEGIN
    INSERT INTO Coupon (ShopId, CouponCode, CouponCategory, DiscountType, DiscountValue, MinOrderValue, MaxDiscountAmount, UsageLimit, UsedCount, IsActive, StartDate, EndDate)
    VALUES (@ShopImportId, 'GLOBALVIP50K', 'PRODUCT', 'FIXED_AMOUNT', 50000.00, 500000.00, 50000.00, 80, 18, 1, DATEADD(MONTH, -2, GETDATE()), DATEADD(MONTH, 2, GETDATE()));
END

-- Coupon 7 [KỊCH BẢN TEST CẢNH BÁO 1]: Mã sắp hết lượt dùng (95/100 = 95% >= 90% Ngân sách)
IF NOT EXISTS (SELECT 1 FROM Coupon WHERE CouponCode = 'FLASHDEAL90')
BEGIN
    INSERT INTO Coupon (ShopId, CouponCode, CouponCategory, DiscountType, DiscountValue, MinOrderValue, MaxDiscountAmount, UsageLimit, UsedCount, IsActive, StartDate, EndDate)
    VALUES (NULL, 'FLASHDEAL90', 'PRODUCT', 'FIXED_AMOUNT', 20000.00, 100000.00, 20000.00, 100, 95, 1, DATEADD(MONTH, -1, GETDATE()), DATEADD(MONTH, 1, GETDATE()));
END
ELSE
BEGIN
    UPDATE Coupon SET UsageLimit = 100, UsedCount = 95, IsActive = 1 WHERE CouponCode = 'FLASHDEAL90';
END

-- Coupon 8 [KỊCH BẢN TEST CẢNH BÁO 2]: Mã trợ giá quá sâu, ROI thấp (ROI < 1.5)
IF NOT EXISTS (SELECT 1 FROM Coupon WHERE CouponCode = 'DEALSHOCK30')
BEGIN
    INSERT INTO Coupon (ShopId, CouponCode, CouponCategory, DiscountType, DiscountValue, MinOrderValue, MaxDiscountAmount, UsageLimit, UsedCount, IsActive, StartDate, EndDate)
    VALUES (@ShopDalatId, 'DEALSHOCK30', 'PRODUCT', 'FIXED_AMOUNT', 85000.00, 100000.00, 85000.00, 50, 8, 1, DATEADD(MONTH, -1, GETDATE()), DATEADD(MONTH, 1, GETDATE()));
END
ELSE
BEGIN
    UPDATE Coupon SET UsageLimit = 50, UsedCount = 8, IsActive = 1 WHERE CouponCode = 'DEALSHOCK30';
END

-- Lấy ID các Coupon
DECLARE @IdFreeship30K INT = (SELECT CouponId FROM Coupon WHERE CouponCode = 'FREESHIP30K');
DECLARE @IdBetaFruit10 INT = (SELECT CouponId FROM Coupon WHERE CouponCode = 'BETAFRUIT10');
DECLARE @IdFreeship50K INT = (SELECT CouponId FROM Coupon WHERE CouponCode = 'FREESHIP50K');
DECLARE @IdDalatFarm25K INT = (SELECT CouponId FROM Coupon WHERE CouponCode = 'DALATFARM25K');
DECLARE @IdMienTay8Pct INT = (SELECT CouponId FROM Coupon WHERE CouponCode = 'MIENTAY8PCT');
DECLARE @IdGlobalVIP50K INT = (SELECT CouponId FROM Coupon WHERE CouponCode = 'GLOBALVIP50K');
DECLARE @IdFlashDeal90 INT = (SELECT CouponId FROM Coupon WHERE CouponCode = 'FLASHDEAL90');
DECLARE @IdDealShock30 INT = (SELECT CouponId FROM Coupon WHERE CouponCode = 'DEALSHOCK30');

-- ----------------------------------------------------------------------------------------------------
-- 3. XÓA CÁC ĐƠN HÀNG MOCK CŨ CỦA DASHBOARD (NẾU CÓ) ĐỂ TRÁNH TRÙNG LẶP
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> 2. Dọn dẹp dữ liệu đơn hàng mock dashboard cũ (OrderId >= 100)...';
DELETE FROM OrderItem WHERE OrderId >= 100 AND OrderId <= 150;
DELETE FROM [Order] WHERE OrderId >= 100 AND OrderId <= 150;

-- ----------------------------------------------------------------------------------------------------
-- 4. TẠO 28 ĐƠN HÀNG PHÂN PHỐI ĐỀU 30 NGÀY QUA (TẠO ĐƯỜNG CONG BIỂU ĐỒ UỐN LƯỢN RÕ ĐẸP)
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> 3. Nạp 28 đơn hàng trải dài 30 ngày qua...';

SET IDENTITY_INSERT [Order] ON;

INSERT INTO [Order] (
    OrderId, CustomerId, ShopId, CouponId, SnapshotShippingAddress, 
    TotalAmount, DiscountAmount, ShippingFee, FinalAmount, 
    PlatformFee, ShopNetReceived, PaymentMethod, OrderStatus, 
    IsSettled, IsCODRemitted, SettledAt, CreatedAt
) VALUES
-- Hôm nay (Day 0): 2 đơn bùng nổ
(101, @Customer1, @ShopDalatId, @IdBetaFruit10, N'Hoàng Thùy Linh | 0967890123 | Tòa Landmark 81, TP. HCM', 520000.00, 52000.00, 25000.00, 493000.00, 26000.00, 442000.00, 'VNPAY', 'SUCCESS', 1, 0, GETDATE(), GETDATE()),
(102, @Customer2, @ShopMienTayId, @IdFreeship30K, N'Vũ Thị Mai Hoa | 0989012345 | 45 Lê Duẩn, Đà Nẵng', 380000.00, 30000.00, 30000.00, 380000.00, 19000.00, 331000.00, 'COD', 'DELIVERING', 0, 0, NULL, DATEADD(HOUR, -3, GETDATE())),

-- 1 ngày trước: 1 đơn
(103, @Customer3, @ShopDalatId, @IdDalatFarm25K, N'Đỗ Anh Đức | 0978901234 | 128 Nguyễn Trãi, Hà Nội', 350000.00, 25000.00, 20000.00, 345000.00, 17500.00, 307500.00, 'COD', 'SUCCESS', 1, 1, DATEADD(DAY, -1, GETDATE()), DATEADD(DAY, -1, GETDATE())),

-- 2 ngày trước: 2 đơn
(104, @Customer1, @ShopImportId, @IdGlobalVIP50K, N'Hoàng Thùy Linh | 0967890123 | Landmark 81, TP. HCM', 850000.00, 50000.00, 35000.00, 835000.00, 51000.00, 749000.00, 'VNPAY', 'SUCCESS', 1, 0, DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE())),
(105, @Customer2, @ShopDalatId, @IdDealShock30, N'Vũ Thị Mai Hoa | 0989012345 | 45 Lê Duẩn, Đà Nẵng', 110000.00, 85000.00, 20000.00, 45000.00, 5500.00, 19500.00, 'VNPAY', 'DELIVERED', 1, 0, DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -2, GETDATE())),

-- 3 ngày trước: 1 đơn
(106, @Customer3, @ShopMienTayId, @IdMienTay8Pct, N'Đỗ Anh Đức | 0978901234 | 128 Nguyễn Trãi, Hà Nội', 420000.00, 33600.00, 25000.00, 411400.00, 21000.00, 365400.00, 'COD', 'SUCCESS', 1, 1, DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -3, GETDATE())),

-- 4 ngày trước: 2 đơn
(107, @Customer1, @ShopDalatId, @IdFlashDeal90, N'Hoàng Thùy Linh | 0967890123 | Landmark 81, TP. HCM', 280000.00, 20000.00, 20000.00, 280000.00, 14000.00, 246000.00, 'VNPAY', 'SUCCESS', 1, 0, DATEADD(DAY, -4, GETDATE()), DATEADD(DAY, -4, GETDATE())),
(108, @Customer2, @ShopImportId, @IdFreeship50K, N'Vũ Thị Mai Hoa | 0989012345 | 45 Lê Duẩn, Đà Nẵng', 720000.00, 50000.00, 50000.00, 720000.00, 43200.00, 626800.00, 'VNPAY', 'SUCCESS', 1, 0, DATEADD(DAY, -4, GETDATE()), DATEADD(DAY, -4, GETDATE())),

-- 5 ngày trước: 1 đơn
(109, @Customer3, @ShopDalatId, @IdBetaFruit10, N'Đỗ Anh Đức | 0978901234 | 128 Nguyễn Trãi, Hà Nội', 640000.00, 64000.00, 25000.00, 601000.00, 32000.00, 544000.00, 'COD', 'SUCCESS', 1, 1, DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, -5, GETDATE())),

-- 6 ngày trước (Cuối tuần - doanh số đỉnh cao): 2 đơn
(110, @Customer1, @ShopImportId, @IdGlobalVIP50K, N'Hoàng Thùy Linh | 0967890123 | Landmark 81, TP. HCM', 1150000.00, 50000.00, 30000.00, 1130000.00, 69000.00, 1031000.00, 'VNPAY', 'SUCCESS', 1, 0, DATEADD(DAY, -6, GETDATE()), DATEADD(DAY, -6, GETDATE())),
(111, @Customer2, @ShopMienTayId, @IdFreeship30K, N'Vũ Thị Mai Hoa | 0989012345 | 45 Lê Duẩn, Đà Nẵng', 460000.00, 30000.00, 30000.00, 460000.00, 23000.00, 407000.00, 'COD', 'SUCCESS', 1, 1, DATEADD(DAY, -6, GETDATE()), DATEADD(DAY, -6, GETDATE())),

-- 7 ngày trước: 1 đơn
(112, @Customer3, @ShopDalatId, @IdDalatFarm25K, N'Đỗ Anh Đức | 0978901234 | 128 Nguyễn Trãi, Hà Nội', 320000.00, 25000.00, 20000.00, 315000.00, 16000.00, 279000.00, 'COD', 'SUCCESS', 1, 1, DATEADD(DAY, -7, GETDATE()), DATEADD(DAY, -7, GETDATE())),

-- 8 ngày trước: 1 đơn
(113, @Customer1, @ShopMienTayId, @IdMienTay8Pct, N'Hoàng Thùy Linh | 0967890123 | Landmark 81, TP. HCM', 580000.00, 46400.00, 25000.00, 558600.00, 29000.00, 504600.00, 'VNPAY', 'SUCCESS', 1, 0, DATEADD(DAY, -8, GETDATE()), DATEADD(DAY, -8, GETDATE())),

-- 10 ngày trước: 2 đơn
(114, @Customer2, @ShopDalatId, @IdBetaFruit10, N'Vũ Thị Mai Hoa | 0989012345 | 45 Lê Duẩn, Đà Nẵng', 480000.00, 48000.00, 25000.00, 457000.00, 24000.00, 408000.00, 'VNPAY', 'SUCCESS', 1, 0, DATEADD(DAY, -10, GETDATE()), DATEADD(DAY, -10, GETDATE())),
(115, @Customer3, @ShopImportId, @IdFreeship30K, N'Đỗ Anh Đức | 0978901234 | 128 Nguyễn Trãi, Hà Nội', 650000.00, 30000.00, 30000.00, 650000.00, 39000.00, 581000.00, 'COD', 'SUCCESS', 1, 1, DATEADD(DAY, -10, GETDATE()), DATEADD(DAY, -10, GETDATE())),

-- 12 ngày trước: 1 đơn
(116, @Customer1, @ShopDalatId, @IdDalatFarm25K, N'Hoàng Thùy Linh | 0967890123 | Landmark 81, TP. HCM', 290000.00, 25000.00, 20000.00, 285000.00, 14500.00, 250500.00, 'COD', 'SUCCESS', 1, 1, DATEADD(DAY, -12, GETDATE()), DATEADD(DAY, -12, GETDATE())),

-- 14 ngày trước: 2 đơn
(117, @Customer2, @ShopMienTayId, @IdMienTay8Pct, N'Vũ Thị Mai Hoa | 0989012345 | 45 Lê Duẩn, Đà Nẵng', 390000.00, 31200.00, 25000.00, 383800.00, 19500.00, 339300.00, 'VNPAY', 'SUCCESS', 1, 0, DATEADD(DAY, -14, GETDATE()), DATEADD(DAY, -14, GETDATE())),
(118, @Customer3, @ShopDalatId, @IdFreeship30K, N'Đỗ Anh Đức | 0978901234 | 128 Nguyễn Trãi, Hà Nội', 410000.00, 30000.00, 30000.00, 410000.00, 20500.00, 359500.00, 'COD', 'SUCCESS', 1, 1, DATEADD(DAY, -14, GETDATE()), DATEADD(DAY, -14, GETDATE())),

-- 16 ngày trước: 1 đơn
(119, @Customer1, @ShopImportId, @IdGlobalVIP50K, N'Hoàng Thùy Linh | 0967890123 | Landmark 81, TP. HCM', 980000.00, 50000.00, 35000.00, 965000.00, 58800.00, 871200.00, 'VNPAY', 'SUCCESS', 1, 0, DATEADD(DAY, -16, GETDATE()), DATEADD(DAY, -16, GETDATE())),

-- 18 ngày trước: 2 đơn
(120, @Customer2, @ShopDalatId, @IdBetaFruit10, N'Vũ Thị Mai Hoa | 0989012345 | 45 Lê Duẩn, Đà Nẵng', 550000.00, 55000.00, 20000.00, 515000.00, 27500.00, 467500.00, 'COD', 'SUCCESS', 1, 1, DATEADD(DAY, -18, GETDATE()), DATEADD(DAY, -18, GETDATE())),
(121, @Customer3, @ShopMienTayId, @IdFreeship30K, N'Đỗ Anh Đức | 0978901234 | 128 Nguyễn Trãi, Hà Nội', 310000.00, 30000.00, 30000.00, 310000.00, 15500.00, 264500.00, 'VNPAY', 'SUCCESS', 1, 0, DATEADD(DAY, -18, GETDATE()), DATEADD(DAY, -18, GETDATE())),

-- 20 ngày trước: 1 đơn
(122, @Customer1, @ShopDalatId, @IdDalatFarm25K, N'Hoàng Thùy Linh | 0967890123 | Landmark 81, TP. HCM', 360000.00, 25000.00, 20000.00, 355000.00, 18000.00, 317000.00, 'COD', 'SUCCESS', 1, 1, DATEADD(DAY, -20, GETDATE()), DATEADD(DAY, -20, GETDATE())),

-- 22 ngày trước: 2 đơn
(123, @Customer2, @ShopImportId, @IdFreeship50K, N'Vũ Thị Mai Hoa | 0989012345 | 45 Lê Duẩn, Đà Nẵng', 820000.00, 50000.00, 50000.00, 820000.00, 49200.00, 720800.00, 'VNPAY', 'SUCCESS', 1, 0, DATEADD(DAY, -22, GETDATE()), DATEADD(DAY, -22, GETDATE())),
(124, @Customer3, @ShopDalatId, @IdBetaFruit10, N'Đỗ Anh Đức | 0978901234 | 128 Nguyễn Trãi, Hà Nội', 450000.00, 45000.00, 25000.00, 430000.00, 22500.00, 382500.00, 'COD', 'SUCCESS', 1, 1, DATEADD(DAY, -22, GETDATE()), DATEADD(DAY, -22, GETDATE())),

-- 25 ngày trước: 2 đơn
(125, @Customer1, @ShopMienTayId, @IdMienTay8Pct, N'Hoàng Thùy Linh | 0967890123 | Landmark 81, TP. HCM', 620000.00, 49600.00, 30000.00, 600400.00, 31000.00, 539400.00, 'VNPAY', 'SUCCESS', 1, 0, DATEADD(DAY, -25, GETDATE()), DATEADD(DAY, -25, GETDATE())),
(126, @Customer2, @ShopDalatId, @IdFreeship30K, N'Vũ Thị Mai Hoa | 0989012345 | 45 Lê Duẩn, Đà Nẵng', 330000.00, 30000.00, 30000.00, 330000.00, 16500.00, 283500.00, 'COD', 'SUCCESS', 1, 1, DATEADD(DAY, -25, GETDATE()), DATEADD(DAY, -25, GETDATE())),

-- 28 ngày trước: 2 đơn
(127, @Customer3, @ShopImportId, @IdGlobalVIP50K, N'Đỗ Anh Đức | 0978901234 | 128 Nguyễn Trãi, Hà Nội', 1050000.00, 50000.00, 35000.00, 1035000.00, 63000.00, 937000.00, 'VNPAY', 'SUCCESS', 1, 0, DATEADD(DAY, -28, GETDATE()), DATEADD(DAY, -28, GETDATE())),
(128, @Customer1, @ShopDalatId, @IdBetaFruit10, N'Hoàng Thùy Linh | 0967890123 | Landmark 81, TP. HCM', 490000.00, 49000.00, 20000.00, 461000.00, 24500.00, 416500.00, 'VNPAY', 'SUCCESS', 1, 0, DATEADD(DAY, -28, GETDATE()), DATEADD(DAY, -28, GETDATE()));

SET IDENTITY_INSERT [Order] OFF;

-- ----------------------------------------------------------------------------------------------------
-- 5. NẠP CHI TIẾT SẢN PHẨM [OrderItem] VỚI TÊN TRÁI CÂY THẬT (PHỤC VỤ BẢNG TOP SẢN PHẨM & LỢI NHUẬN GỘP)
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> 4. Nạp OrderItem cho 28 đơn hàng với dữ liệu quả thật...';

SET IDENTITY_INSERT OrderItem ON;

INSERT INTO OrderItem (
    OrderItemId, OrderId, VariantId, Quantity, 
    PriceSnapshot, CostSnapshot, SnapshotProductName, SnapshotVariantName
) VALUES
-- Đơn 101: Dâu tây giống Nhật (2 hộp)
(101, 101, @VariantDauTay, 2, 260000.00, 180000.00, N'Dâu Tây Giống Nhật Cao Cấp Đà Lạt', N'Hộp 500g Quà Tặng'),

-- Đơn 102: Sầu riêng Ri6 (1 quả)
(102, 102, @VariantSauRieng, 1, 380000.00, 250000.00, N'Sầu Riêng Ri6 Chín Cây Tiền Giang', N'Trái 2.5kg - 3.0kg'),

-- Đơn 103: Bơ sáp 034 (3 túi)
(103, 103, @VariantBoSap, 3, 116666.67, 70000.00, N'Bơ Sáp 034 Lâm Đồng Cơm Vàng', N'Túi 2kg (5-6 trái)'),

-- Đơn 104: Nho Mẫu Đơn Shine Muscat (1 chùm lớn) + Dâu
(104, 104, @VariantNhoMauDon, 1, 550000.00, 380000.00, N'Nho Mẫu Đơn Shine Muscat Hàn Quốc VIP', N'Chùm 750g Hộp Nhựa'),
(105, 104, @VariantDauTay, 1, 300000.00, 200000.00, N'Dâu Tây Giống Nhật Cao Cấp Đà Lạt', N'Hộp 500g Quà Tặng'),

-- Đơn 105: Bơ sáp (deal giảm sâu)
(106, 105, @VariantBoSap, 1, 110000.00, 70000.00, N'Bơ Sáp 034 Lâm Đồng Cơm Vàng', N'Túi 2kg'),

-- Đơn 106: Xoài cát Hòa Lộc (2 hộp)
(107, 106, @VariantXoaiCat, 2, 210000.00, 130000.00, N'Xoài Cát Hòa Lộc Tiền Giang', N'Hộp 2kg (4-5 quả)'),

-- Đơn 107: Dâu tây Nhật (1 hộp)
(108, 107, @VariantDauTay, 1, 280000.00, 180000.00, N'Dâu Tây Giống Nhật Cao Cấp Đà Lạt', N'Hộp 250g Thượng Hạng'),

-- Đơn 108: Cherry đỏ Mỹ (2 hộp)
(109, 108, @VariantCherry, 2, 360000.00, 240000.00, N'Cherry Đỏ Mỹ Size 9.5 Premium', N'Hộp 500g Size 9.5'),

-- Đơn 109: Dâu tây Nhật (2 hộp) + Bơ (1 túi)
(110, 109, @VariantDauTay, 2, 260000.00, 180000.00, N'Dâu Tây Giống Nhật Cao Cấp Đà Lạt', N'Hộp 500g'),
(111, 109, @VariantBoSap, 1, 120000.00, 70000.00, N'Bơ Sáp 034 Lâm Đồng Cơm Vàng', N'Túi 2kg'),

-- Đơn 110: Nho Mẫu Đơn VIP (2 chùm)
(112, 110, @VariantNhoMauDon, 2, 575000.00, 390000.00, N'Nho Mẫu Đơn Shine Muscat Hàn Quốc VIP', N'Chùm 750g'),

-- Đơn 111: Sầu riêng Ri6 (1 quả)
(113, 111, @VariantSauRieng, 1, 460000.00, 310000.00, N'Sầu Riêng Ri6 Chín Cây Tiền Giang', N'Trái 3.1kg - 3.8kg'),

-- Đơn 112: Bơ sáp 034 (3 túi)
(114, 112, @VariantBoSap, 3, 106666.67, 70000.00, N'Bơ Sáp 034 Lâm Đồng Cơm Vàng', N'Túi 2kg'),

-- Đơn 113: Sầu riêng (1 quả) + Xoài (1 hộp)
(115, 113, @VariantSauRieng, 1, 390000.00, 270000.00, N'Sầu Riêng Ri6 Chín Cây Tiền Giang', N'Trái 2.8kg'),
(116, 113, @VariantXoaiCat, 1, 190000.00, 120000.00, N'Xoài Cát Hòa Lộc Tiền Giang', N'Hộp 2kg'),

-- Đơn 114: Dâu tây Nhật (2 hộp)
(117, 114, @VariantDauTay, 2, 240000.00, 160000.00, N'Dâu Tây Giống Nhật Cao Cấp Đà Lạt', N'Hộp 500g'),

-- Đơn 115: Cherry đỏ Mỹ (2 hộp)
(118, 115, @VariantCherry, 2, 325000.00, 220000.00, N'Cherry Đỏ Mỹ Size 9.5 Premium', N'Hộp 500g'),

-- Đơn 116: Bơ sáp (2 túi)
(119, 116, @VariantBoSap, 2, 145000.00, 90000.00, N'Bơ Sáp 034 Lâm Đồng Cơm Vàng', N'Túi 2kg'),

-- Đơn 117: Xoài cát Hòa Lộc (2 hộp)
(120, 117, @VariantXoaiCat, 2, 195000.00, 125000.00, N'Xoài Cát Hòa Lộc Tiền Giang', N'Hộp 2kg'),

-- Đơn 118: Dâu tây Đà Lạt (1 hộp) + Bơ (1 túi)
(121, 118, @VariantDauTay, 1, 280000.00, 180000.00, N'Dâu Tây Giống Nhật Cao Cấp Đà Lạt', N'Hộp 500g'),
(122, 118, @VariantBoSap, 1, 130000.00, 80000.00, N'Bơ Sáp 034 Lâm Đồng Cơm Vàng', N'Túi 2kg'),

-- Đơn 119: Nho mẫu đơn VIP (1 chùm) + Cherry (1 hộp)
(123, 119, @VariantNhoMauDon, 1, 580000.00, 400000.00, N'Nho Mẫu Đơn Shine Muscat Hàn Quốc VIP', N'Chùm 750g'),
(124, 119, @VariantCherry, 1, 400000.00, 260000.00, N'Cherry Đỏ Mỹ Size 9.5 Premium', N'Hộp 500g'),

-- Đơn 120: Dâu tây Nhật (2 hộp)
(125, 120, @VariantDauTay, 2, 275000.00, 185000.00, N'Dâu Tây Giống Nhật Cao Cấp Đà Lạt', N'Hộp 500g'),

-- Đơn 121: Sầu riêng Ri6 (1 quả)
(126, 121, @VariantSauRieng, 1, 310000.00, 210000.00, N'Sầu Riêng Ri6 Chín Cây Tiền Giang', N'Trái 2.5kg'),

-- Đơn 122: Bơ sáp (3 túi)
(127, 122, @VariantBoSap, 3, 120000.00, 75000.00, N'Bơ Sáp 034 Lâm Đồng Cơm Vàng', N'Túi 2kg'),

-- Đơn 123: Cherry đỏ Mỹ (2 hộp)
(128, 123, @VariantCherry, 2, 410000.00, 280000.00, N'Cherry Đỏ Mỹ Size 9.5 Premium', N'Hộp 500g'),

-- Đơn 124: Dâu tây Nhật (1 hộp) + Bơ (1 túi)
(129, 124, @VariantDauTay, 1, 300000.00, 200000.00, N'Dâu Tây Giống Nhật Cao Cấp Đà Lạt', N'Hộp 500g'),
(130, 124, @VariantBoSap, 1, 150000.00, 90000.00, N'Bơ Sáp 034 Lâm Đồng Cơm Vàng', N'Túi 2kg'),

-- Đơn 125: Sầu riêng (1 quả) + Xoài (1 hộp)
(131, 125, @VariantSauRieng, 1, 420000.00, 290000.00, N'Sầu Riêng Ri6 Chín Cây Tiền Giang', N'Trái 3.0kg'),
(132, 125, @VariantXoaiCat, 1, 200000.00, 130000.00, N'Xoài Cát Hòa Lộc Tiền Giang', N'Hộp 2kg'),

-- Đơn 126: Bơ sáp (3 túi)
(133, 126, @VariantBoSap, 3, 110000.00, 70000.00, N'Bơ Sáp 034 Lâm Đồng Cơm Vàng', N'Túi 2kg'),

-- Đơn 127: Nho Mẫu Đơn (1 chùm) + Cherry (1 hộp)
(134, 127, @VariantNhoMauDon, 1, 600000.00, 420000.00, N'Nho Mẫu Đơn Shine Muscat Hàn Quốc VIP', N'Chùm 750g'),
(135, 127, @VariantCherry, 1, 450000.00, 300000.00, N'Cherry Đỏ Mỹ Size 9.5 Premium', N'Hộp 500g'),

-- Đơn 128: Dâu tây Nhật (1 hộp) + Xoài (1 hộp)
(136, 128, @VariantDauTay, 1, 290000.00, 190000.00, N'Dâu Tây Giống Nhật Cao Cấp Đà Lạt', N'Hộp 500g'),
(137, 128, @VariantXoaiCat, 1, 200000.00, 130000.00, N'Xoài Cát Hòa Lộc Tiền Giang', N'Hộp 2kg');

SET IDENTITY_INSERT OrderItem OFF;

-- ----------------------------------------------------------------------------------------------------
-- 6. TỔNG KẾT VÀ BÁO CÁO THỐNG KÊ SAU KHI NẠP
-- ----------------------------------------------------------------------------------------------------
PRINT N'======================================================================';
PRINT N'==> NẠP DỮ LIỆU MOCK VOUCHER DASHBOARD THÀNH CÔNG!';
PRINT N'======================================================================';

SELECT 
    COUNT(o.OrderId) AS [Tổng Số Đơn Có Mã],
    SUM(o.TotalAmount) AS [Tổng Doanh Thu Đơn Có Mã (VND)],
    SUM(o.DiscountAmount) AS [Tổng Tiền Trợ Giá (VND)],
    COUNT(DISTINCT o.CouponId) AS [Số Mã Khác Nhau Đã Dùng]
FROM [Order] o 
WHERE o.CouponId IS NOT NULL AND o.OrderStatus != 'CANCELLED';

PRINT N'>> BẠN CÓ THỂ MỞ TRÌNH DUYỆT VÀ TEST TRÊN GIAO DIỆN NGAY:';
PRINT N'1. Dashboard Toàn Sàn Admin: http://localhost:8080/admin/coupons/dashboard';
PRINT N'2. Dashboard Shop Đà Lạt:    http://localhost:8080/shop/coupons/dashboard';
PRINT N'3. Tải file Excel/CSV:       Bấm nút [Xuất Báo Cáo Excel] màu xanh lá trên giao diện.';
PRINT N'======================================================================';
