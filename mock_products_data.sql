-- ==============================================================================
-- KỊCH BẢN TẠO DỮ LIỆU MOCK: QUẢN LÝ DUYỆT & ẨN HOA QUẢ (PRODUCTS)
-- Dự án: BetaFruit - SWP391 Shopping Platform
-- Mục đích: Hỗ trợ kiểm thử đầy đủ các chức năng:
--   1. Lọc theo các Tab: Tất cả, Chờ duyệt, Đang mở bán, Đang ẩn, Đã từ chối
--   2. Bấm "Duyệt" hoa quả mới đăng ký mở bán (Tab Chờ duyệt -> Đang mở bán)
--   3. Bấm "Ẩn" hoa quả vi phạm quy định / chất lượng (Tab Đang mở bán -> Đang ẩn)
--   4. Bấm "Hiện" (Mở bán lại) hoa quả đã khắc phục vi phạm (Tab Đang ẩn -> Đang mở bán)
--   5. Bấm "Từ chối" hoa quả không đạt tiêu chuẩn (Tab Chờ duyệt -> Đã từ chối)
--   6. Xem Modal Chi tiết hoa quả & Tìm kiếm theo từ khóa
-- ==============================================================================

USE [BetaFruit];
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
GO

-- 1. Xóa dữ liệu cũ liên quan đến Product
DELETE FROM [ProductImage] WHERE [ProductId] IN (SELECT [ProductId] FROM [Product]);
DELETE FROM [ProductVariant] WHERE [ProductId] IN (SELECT [ProductId] FROM [Product]);
DELETE FROM [ReviewMedia] WHERE [ReviewId] IN (SELECT [ReviewId] FROM [Review] WHERE [ProductId] IN (SELECT [ProductId] FROM [Product]));
DELETE FROM [Review] WHERE [ProductId] IN (SELECT [ProductId] FROM [Product]);
DELETE FROM [OrderItem] WHERE [VariantId] IN (SELECT [VariantId] FROM [ProductVariant]);
DELETE FROM [CartItem] WHERE [VariantId] IN (SELECT [VariantId] FROM [ProductVariant]);
DELETE FROM [Product];
GO

-- ==============================================================================
-- 2. INSERT HOA QUẢ VÀO BẢNG [Product]
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- NHÓM 1: CHỜ KIỂM DUYỆT (Pending + Hidden) - Dùng để test BẤM "DUYỆT" MỞ BÁN HOẶC "TỪ CHỐI"
-- ------------------------------------------------------------------------------
INSERT INTO [Product] ([ShopId], [CategoryId], [ProductName], [Description], [ApprovalStatus], [VisibilityStatus], [CreatedAt])
VALUES
(2, 1, N'Nho Mẫu Đơn Shine Muscat Hàn Quốc (Chùm 700g)', 
 N'Nho sữa Shine Muscat chuẩn nhập khẩu Hàn Quốc, chùm to từ 600g - 800g, quả xanh mọng không hạt, giòn ngọt đậm đà phảng phất hương thơm sữa đặc trưng. Cửa hàng đã nộp chứng từ CO/CQ và giấy kiểm định ATTP chờ ban quản trị phê duyệt mở bán.', 
 'Pending', 'Hidden', DATEADD(HOUR, -2, GETDATE())),

(1, 2, N'Sầu Riêng Ri6 Chín Cây Tiền Giang (Bao Ăn 1 Đổi 1)', 
 N'Sầu riêng Ri6 thu hoạch chín tự nhiên tại miệt vườn Tiền Giang, múi dày, cơm vàng ươm dẻo béo, hạt lép hoàn toàn. Không nhúng thuốc thúc chín, bảo hành 1 đổi 1. Hồ sơ mở bán mùa vụ mới chờ kiểm duyệt.', 
 'Pending', 'Hidden', DATEADD(HOUR, -4, GETDATE())),

(2, 3, N'Dâu Tây Bạch Tuyết Organic Mộc Châu', 
 N'Dâu tây giống Bạch Tuyết quý hiếm trồng theo tiêu chuẩn hữu cơ công nghệ cao tại Mộc Châu, quả trắng ngà điểm mắt đỏ, hương thơm ngào ngạt tựa mùi dứa, vị ngọt thanh tao. Sản phẩm nông nghiệp hữu cơ mới đăng ký sàn.', 
 'Pending', 'Hidden', DATEADD(HOUR, -6, GETDATE())),

(2, 4, N'Hộp Quà Trái Cây Phú Quý Cát Tường (Gỗ Sang Trọng)', 
 N'Set quà biếu thượng hạng kết hợp Lê Hàn Quốc, Táo Envy New Zealand, Cam Cara ruột đỏ và Nho ngón tay Úc đóng hộp gỗ vân sồi trang trí hoa tươi tinh tế. Đăng ký kiểm định chất lượng phục vụ quà biếu doanh nghiệp.', 
 'Pending', 'Hidden', DATEADD(HOUR, -8, GETDATE())),

(1, 5, N'Mãng Cầu Xiêm Sấy Dẻo Lắc Muối Ớt Tây Ninh (OCOP 4 Sao)', 
 N'Mãng cầu xiêm Đồng Tháp tách hạt sấy lạnh giữ trọn vitamin và vị chua thanh tự nhiên, phủ nhẹ muối ớt Tây Ninh cay the kích thích vị giác. Đạt chuẩn chứng nhận sản phẩm OCOP 4 sao tỉnh.', 
 'Pending', 'Hidden', DATEADD(HOUR, -12, GETDATE()));

-- ------------------------------------------------------------------------------
-- NHÓM 2: ĐANG MỞ BÁN (Approved + Active) - Dùng để test BẤM "ẨN" HOA QUẢ VI PHẠM
-- ------------------------------------------------------------------------------
INSERT INTO [Product] ([ShopId], [CategoryId], [ProductName], [Description], [ApprovalStatus], [VisibilityStatus], [CreatedAt])
VALUES
-- [VI PHẠM ĐỂ TEST BẤM ẨN]
(3, 1, N'[VI PHẠM] Cherry Đỏ Mỹ Size 9.0 (Khách khiếu nại úng hỏng & thiếu tem QR)', 
 N'[SẢN PHẨM CẦN ẨN KIỂM TRA] Nhận được 4 phản hồi từ người mua về tình trạng cherry bị úng nhũn, cuống thâm đen và không quét được mã QR truy xuất nguồn gốc. Admin cần bấm nút [ẨN] để tạm dừng hiển thị trên sàn và gửi cảnh báo đến gian hàng.', 
 'Approved', 'Active', DATEADD(DAY, -5, GETDATE())),

-- [VI PHẠM ĐỂ TEST BẤM ẨN]
(3, 2, N'[VI PHẠM] Xoài Cát Hòa Lộc Tiền Giang (Nghi vấn giả mạo xuất xứ & tráo hàng)', 
 N'[SẢN PHẨM CẦN ẨN KIỂM TRA] Đang có tranh chấp khiếu nại: Shop đăng bán Xoài Cát Hòa Lộc chuẩn Tiền Giang nhưng giao xoài lai tạp phẩm chất kém, vi phạm chính sách trung thực sản phẩm của BetaFruit. Admin bấm nút [ẨN] để thanh tra chất lượng.', 
 'Approved', 'Active', DATEADD(DAY, -4, GETDATE())),

-- [Hàng bình thường đang bán]
(2, 1, N'Táo Envy New Zealand Size 30 Nhập Khẩu', 
 N'Táo Envy chuẩn nhập khẩu New Zealand đường hàng không, độ giòn 9.5/10, ngọt đậm và mọng nước. Quả tròn đều, màu đỏ ruby sọc vàng tuyệt đẹp, đã kiểm dịch thực vật đầy đủ.', 
 'Approved', 'Active', DATEADD(DAY, -10, GETDATE())),

(1, 2, N'Cam Sành Bến Tre Mọng Nước Chuẩn VietGAP', 
 N'Cam sành cành lá tươi xanh thu hái tại Bến Tre, vỏ mỏng rám đặc trưng, tép cam vàng óng mọng nước, vị ngọt thanh nhẹ giàu khoáng chất và Vitamin C. Canh tác an toàn theo quy trình VietGAP.', 
 'Approved', 'Active', DATEADD(DAY, -8, GETDATE())),

(2, 3, N'Dưa Lưới Huỳnh Long Ruột Cam Organic (Trái 1.5 - 2kg)', 
 N'Dưa lưới Huỳnh Long giống Nhật canh tác trong nhà màng hữu cơ công nghệ cao tại Đà Lạt. Ruột cam giòn ngọt lịm, độ đường đạt Brix 14-16, có tem truy xuất QR đến từng gốc dưa.', 
 'Approved', 'Active', DATEADD(DAY, -6, GETDATE())),

(2, 4, N'Giỏ Quà Trái Cây Ngũ Phúc Lâm Môn', 
 N'Giỏ quà trái cây nghệ thuật mang ý nghĩa tài lộc gồm Bưởi da xanh ruột hồng, Thanh long ruột đỏ, Lê Nam Phi, Cam vàng Úc và Nho đen Midnight không hạt kèm thiệp chúc mừng thiết kế riêng.', 
 'Approved', 'Active', DATEADD(DAY, -7, GETDATE())),

(2, 5, N'Xoài Cát Sấy Dẻo Xuất Khẩu Không Đường', 
 N'Xoài cát chín cây sấy nhiệt độ thấp 100% không tẩm ướp thêm đường hóa học và phẩm màu, dẻo mềm vị chua ngọt hài hòa tự nhiên, giàu chất xơ, đạt tiêu chuẩn xuất khẩu sang thị trường Nhật Bản.', 
 'Approved', 'Active', DATEADD(DAY, -9, GETDATE()));

-- ------------------------------------------------------------------------------
-- NHÓM 3: ĐANG BỊ TẠM ẨN (Approved + Hidden) - Dùng để test Tab "Đang ẩn" & BẤM "HIỆN" (MỞ BÁN LẠI)
-- ------------------------------------------------------------------------------
INSERT INTO [Product] ([ShopId], [CategoryId], [ProductName], [Description], [ApprovalStatus], [VisibilityStatus], [CreatedAt])
VALUES
(1, 2, N'Bơ Booth 7 Đắk Lắk Sáp Dẻo (Đã khắc phục quy cách bao bì)', 
 N'Bơ Booth 7 sáp già hái tận vườn Đắk Lắk, vỏ dày bóng, ruột vàng đặc béo ngậy. Trước đó bị Admin ẩn do shop đóng gói thiếu xốp chống dập khi vận chuyển; nay shop đã bổ sung khay và xốp bảo vệ đạt chuẩn, có thể duyệt [Hiện] để mở bán lại.', 
 'Approved', 'Hidden', DATEADD(DAY, -15, GETDATE())),

(2, 1, N'Kiwi Vàng New Zealand Zespri SunGold (Tạm ẩn chờ nhập lô mới)', 
 N'Kiwi vàng Zespri nổi tiếng mọng nước, vị ngọt như mật ong và chứa hàm lượng vitamin C gấp 3 lần cam. Đã được duyệt mở bán nhưng đang tạm ẩn do lô hàng cũ vừa hết, chờ cập nhật mã kiểm dịch lô mới.', 
 'Approved', 'Hidden', DATEADD(DAY, -20, GETDATE())),

(2, 2, N'Mận Hậu Ruby Mộc Châu Sơn La (Tạm ẩn hết mùa vụ thu hoạch)', 
 N'Mận hậu Ruby Mộc Châu quả to tròn đều, lớp phấn trắng tự nhiên bao phủ, cắn giòn tan ngọt lịm pha chút chua dịu ở vỏ. Đã kiểm duyệt đạt chuẩn nhưng tạm ẩn trên sàn vì đã kết thúc mùa thu hoạch năm nay.', 
 'Approved', 'Hidden', DATEADD(DAY, -30, GETDATE()));

-- ------------------------------------------------------------------------------
-- NHÓM 4: ĐÃ TỪ CHỐI KIỂM DUYỆT (Rejected + Hidden) - Dùng để test Tab "Đã từ chối"
-- ------------------------------------------------------------------------------
INSERT INTO [Product] ([ShopId], [CategoryId], [ProductName], [Description], [ApprovalStatus], [VisibilityStatus], [CreatedAt])
VALUES
(3, 5, N'Táo Đỏ Tân Cương Tẩm Hóa Chất (Bị từ chối do vi phạm ATTP)', 
 N'TỪ CHỐI PHÊ DUYỆT: Mẫu kiểm định chất lượng phát hiện dư lượng hóa chất bảo quản và chất tạo ngọt tổng hợp vượt quá ngưỡng an toàn cho phép theo quy định vệ sinh ATTP của Bộ Y Tế.', 
 'Rejected', 'Hidden', DATEADD(DAY, -12, GETDATE())),

(3, 2, N'Nho Đỏ Ninh Thuận Không Rõ Nguồn Gốc (Bị từ chối do thiếu chứng từ)', 
 N'TỪ CHỐI PHÊ DUYỆT: Chủ cửa hàng không cung cấp được giấy chứng nhận nguồn gốc xuất xứ nông sản từ hợp tác xã trồng nho và không có hóa đơn chứng từ hợp lệ.', 
 'Rejected', 'Hidden', DATEADD(DAY, -14, GETDATE()));
GO

-- ==============================================================================
-- 3. INSERT DỮ LIỆU BIẾN THỂ [ProductVariant] THEO PRODUCTNAME
-- ==============================================================================
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

INSERT INTO [ProductVariant] ([ProductId], [VariantName], [OriginalPrice], [SalePrice], [StockQuantity], [LowStockThreshold], [IsAvailable])
SELECT p.ProductId, v.VariantName, v.OriginalPrice, v.SalePrice, v.StockQuantity, v.LowStockThreshold, v.IsAvailable
FROM [Product] p
JOIN (
    VALUES
    (N'Nho Mẫu Đơn Shine Muscat Hàn Quốc (Chùm 700g)', N'Hộp 1 Chùm (khoảng 700g)', 450000, 399000, 30, 5, 1),
    (N'Nho Mẫu Đơn Shine Muscat Hàn Quốc (Chùm 700g)', N'Thùng 2 Chùm (khoảng 1.4kg)', 880000, 780000, 15, 3, 1),
    (N'Sầu Riêng Ri6 Chín Cây Tiền Giang (Bao Ăn 1 Đổi 1)', N'Trái 2.5kg - 3.0kg', 320000, 290000, 50, 10, 1),
    (N'Sầu Riêng Ri6 Chín Cây Tiền Giang (Bao Ăn 1 Đổi 1)', N'Trái 3.1kg - 3.8kg', 420000, 380000, 40, 5, 1),
    (N'Dâu Tây Bạch Tuyết Organic Mộc Châu', N'Hộp 250g Thượng Hạng', 280000, 250000, 20, 5, 1),
    (N'Dâu Tây Bạch Tuyết Organic Mộc Châu', N'Hộp 500g Quà Tặng', 540000, 480000, 15, 3, 1),
    (N'Hộp Quà Trái Cây Phú Quý Cát Tường (Gỗ Sang Trọng)', N'Set Tiêu Chuẩn (Hộp Gỗ)', 1200000, 1050000, 10, 2, 1),
    (N'Mãng Cầu Xiêm Sấy Dẻo Lắc Muối Ớt Tây Ninh (OCOP 4 Sao)', N'Túi Zip 250g', 85000, 75000, 100, 20, 1),
    (N'Mãng Cầu Xiêm Sấy Dẻo Lắc Muối Ớt Tây Ninh (OCOP 4 Sao)', N'Hộp Quà 500g', 160000, 145000, 60, 10, 1),
    (N'[VI PHẠM] Cherry Đỏ Mỹ Size 9.0 (Khách khiếu nại úng hỏng & thiếu tem QR)', N'Hộp 1kg Size 9.0', 480000, 390000, 25, 5, 1),
    (N'[VI PHẠM] Xoài Cát Hòa Lộc Tiền Giang (Nghi vấn giả mạo xuất xứ & tráo hàng)', N'Hộp 3kg Loại 1', 270000, 230000, 35, 5, 1),
    (N'Táo Envy New Zealand Size 30 Nhập Khẩu', N'Túi 1kg (khoảng 3 trái)', 180000, 159000, 80, 15, 1),
    (N'Táo Envy New Zealand Size 30 Nhập Khẩu', N'Thùng 5kg Quà Biếu', 850000, 760000, 25, 5, 1),
    (N'Cam Sành Bến Tre Mọng Nước Chuẩn VietGAP', N'Túi 2kg Vắt Nước', 70000, 59000, 150, 30, 1),
    (N'Cam Sành Bến Tre Mọng Nước Chuẩn VietGAP', N'Thùng 5kg Chọn Lọc', 165000, 140000, 70, 15, 1),
    (N'Dưa Lưới Huỳnh Long Ruột Cam Organic (Trái 1.5 - 2kg)', N'Trái 1.4kg - 1.8kg', 140000, 125000, 45, 10, 1),
    (N'Giỏ Quà Trái Cây Ngũ Phúc Lâm Môn', N'Giỏ Lớn Kèm Thiệp', 950000, 850000, 12, 3, 1),
    (N'Xoài Cát Sấy Dẻo Xuất Khẩu Không Đường', N'Gói 200g Hút Chân Không', 75000, 65000, 90, 20, 1),
    (N'Xoài Cát Sấy Dẻo Xuất Khẩu Không Đường', N'Gói 500g Tiết Kiệm', 170000, 150000, 50, 10, 1),
    (N'Bơ Booth 7 Đắk Lắk Sáp Dẻo (Đã khắc phục quy cách bao bì)', N'Túi 2kg (khoảng 5-6 trái)', 110000, 95000, 60, 10, 1),
    (N'Kiwi Vàng New Zealand Zespri SunGold (Tạm ẩn chờ nhập lô mới)', N'Hộp 6 Trái (khoảng 800g)', 210000, 185000, 40, 8, 1),
    (N'Mận Hậu Ruby Mộc Châu Sơn La (Tạm ẩn hết mùa vụ thu hoạch)', N'Hộp 1kg Vip', 130000, 110000, 30, 5, 1),
    (N'Táo Đỏ Tân Cương Tẩm Hóa Chất (Bị từ chối do vi phạm ATTP)', N'Túi 500g', 120000, 95000, 0, 0, 0),
    (N'Nho Đỏ Ninh Thuận Không Rõ Nguồn Gốc (Bị từ chối do thiếu chứng từ)', N'Hộp 1kg', 80000, 65000, 0, 0, 0)
) AS v(ProductName, VariantName, OriginalPrice, SalePrice, StockQuantity, LowStockThreshold, IsAvailable)
ON p.ProductName = v.ProductName;
GO

-- ==============================================================================
-- 4. INSERT HÌNH ẢNH [ProductImage] THEO PRODUCTNAME
-- ==============================================================================
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

INSERT INTO [ProductImage] ([ProductId], [ImageUrl], [IsPrimary])
SELECT p.ProductId, img.ImageUrl, img.IsPrimary
FROM [Product] p
JOIN (
    VALUES
    (N'Nho Mẫu Đơn Shine Muscat Hàn Quốc (Chùm 700g)', 'https://images.unsplash.com/photo-1537640538966-79f369143f8f?w=600', 1),
    (N'Sầu Riêng Ri6 Chín Cây Tiền Giang (Bao Ăn 1 Đổi 1)', 'https://images.unsplash.com/photo-1587132137056-bfbf0166836e?w=600', 1),
    (N'Dâu Tây Bạch Tuyết Organic Mộc Châu', 'https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=600', 1),
    (N'Hộp Quà Trái Cây Phú Quý Cát Tường (Gỗ Sang Trọng)', 'https://images.unsplash.com/photo-1610832958506-aa56368176cf?w=600', 1),
    (N'Mãng Cầu Xiêm Sấy Dẻo Lắc Muối Ớt Tây Ninh (OCOP 4 Sao)', 'https://images.unsplash.com/photo-1553279768-865429fa0078?w=600', 1),
    (N'[VI PHẠM] Cherry Đỏ Mỹ Size 9.0 (Khách khiếu nại úng hỏng & thiếu tem QR)', 'https://images.unsplash.com/photo-1528825871115-3581a5387919?w=600', 1),
    (N'[VI PHẠM] Xoài Cát Hòa Lộc Tiền Giang (Nghi vấn giả mạo xuất xứ & tráo hàng)', 'https://images.unsplash.com/photo-1553279768-865429fa0078?w=600', 1),
    (N'Táo Envy New Zealand Size 30 Nhập Khẩu', 'https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=600', 1),
    (N'Cam Sành Bến Tre Mọng Nước Chuẩn VietGAP', 'https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=600', 1),
    (N'Dưa Lưới Huỳnh Long Ruột Cam Organic (Trái 1.5 - 2kg)', 'https://images.unsplash.com/photo-1571575179703-4bde44fb1758?w=600', 1),
    (N'Giỏ Quà Trái Cây Ngũ Phúc Lâm Môn', 'https://images.unsplash.com/photo-1610832958506-aa56368176cf?w=600', 1),
    (N'Xoài Cát Sấy Dẻo Xuất Khẩu Không Đường', 'https://images.unsplash.com/photo-1596560548464-f010549b84d7?w=600', 1),
    (N'Bơ Booth 7 Đắk Lắk Sáp Dẻo (Đã khắc phục quy cách bao bì)', 'https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=600', 1),
    (N'Kiwi Vàng New Zealand Zespri SunGold (Tạm ẩn chờ nhập lô mới)', 'https://images.unsplash.com/photo-1518492104633-130d0cc84637?w=600', 1),
    (N'Mận Hậu Ruby Mộc Châu Sơn La (Tạm ẩn hết mùa vụ thu hoạch)', 'https://images.unsplash.com/photo-1568702846914-96b305d2aaeb?w=600', 1),
    (N'Táo Đỏ Tân Cương Tẩm Hóa Chất (Bị từ chối do vi phạm ATTP)', 'https://images.unsplash.com/photo-1502741224143-90386d7f8c82?w=600', 1),
    (N'Nho Đỏ Ninh Thuận Không Rõ Nguồn Gốc (Bị từ chối do thiếu chứng từ)', 'https://images.unsplash.com/photo-1537640538966-79f369143f8f?w=600', 1)
) AS img(ProductName, ImageUrl, IsPrimary)
ON p.ProductName = img.ProductName;
GO

-- ==============================================================================
-- 5. KIỂM TRA KẾT QUẢ VÀ THỐNG KÊ THEO TỪNG TAB
-- ==============================================================================
SELECT 
    COUNT(*) AS [TotalProducts],
    SUM(CASE WHEN [ApprovalStatus] = 'Pending' THEN 1 ELSE 0 END) AS [Tab_ChoDuyet_Pending],
    SUM(CASE WHEN [ApprovalStatus] = 'Approved' AND [VisibilityStatus] = 'Active' THEN 1 ELSE 0 END) AS [Tab_DangMoBan_Active],
    SUM(CASE WHEN [ApprovalStatus] = 'Approved' AND [VisibilityStatus] = 'Hidden' THEN 1 ELSE 0 END) AS [Tab_DangAn_Hidden],
    SUM(CASE WHEN [ApprovalStatus] = 'Rejected' THEN 1 ELSE 0 END) AS [Tab_DaTuChoi_Rejected]
FROM [Product];

SELECT 
    (SELECT COUNT(*) FROM [Product]) AS [ProductCount],
    (SELECT COUNT(*) FROM [ProductVariant]) AS [VariantCount],
    (SELECT COUNT(*) FROM [ProductImage]) AS [ImageCount];
GO
