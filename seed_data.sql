-- ====================================================================================================
-- DỰ ÁN: BETAFRUIT SHOPPING PLATFORM (SWP391)
-- SCRIPT: TẠO DỮ LIỆU MẪU (SEED DATA) CHO MICROSOFT SQL SERVER
-- TÀI KHOẢN MẶC ĐỊNH:
--   1. Quản trị viên (ADMIN):
--      - Email: admin@betafruit.vn | Username: admin | Mật khẩu: admin123
--   2. Cửa hàng (SHOP_OWNER):
--      - Email: dalatfruit@gmail.com   | Username: shop_dalat   | Mật khẩu: 1234567890
--      - Email: mientayfarm@gmail.com  | Username: shop_mientay | Mật khẩu: 1234567890
--      - Email: organicfruit@gmail.com | Username: shop_import  | Mật khẩu: 1234567890
--   3. Khách hàng (CUSTOMER):
--      - Email: phucnguyen@gmail.com | Username: customer_phuc | Mật khẩu: 1234567890
--      - Email: hoanguyen@gmail.com  | Username: customer_hoa  | Mật khẩu: 1234567890
--      - Email: linhpham@gmail.com   | Username: customer_linh | Mật khẩu: 1234567890
--   4. Nhân viên & Shipper:
--      - Email: staff.minh@betafruit.vn | Username: staff_minh   | Mật khẩu: 1234567890
--      - Email: shipper.nam@gmail.com   | Username: shipper_nam  | Mật khẩu: 1234567890
-- ====================================================================================================

SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

-- ----------------------------------------------------------------------------------------------------
-- 1. XÓA DỮ LIỆU CŨ THEO THỨ TỰ RÀNG BUỘC KHÓA NGOẠI (CLEANUP)
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang dọn dẹp dữ liệu cũ (nếu có)...';

IF OBJECT_ID('ReviewMedia', 'U') IS NOT NULL DELETE FROM ReviewMedia;
IF OBJECT_ID('Review', 'U') IS NOT NULL DELETE FROM Review;
IF OBJECT_ID('CODDetail', 'U') IS NOT NULL DELETE FROM CODDetail;
IF OBJECT_ID('CODCollection', 'U') IS NOT NULL DELETE FROM CODCollection;
IF OBJECT_ID('DeliveryAssignment', 'U') IS NOT NULL DELETE FROM DeliveryAssignment;
IF OBJECT_ID('PaymentTransaction', 'U') IS NOT NULL DELETE FROM PaymentTransaction;
IF OBJECT_ID('OrderStatusHistory', 'U') IS NOT NULL DELETE FROM OrderStatusHistory;
IF OBJECT_ID('OrderItem', 'U') IS NOT NULL DELETE FROM OrderItem;
IF OBJECT_ID('[Order]', 'U') IS NOT NULL DELETE FROM [Order];
IF OBJECT_ID('CartItem', 'U') IS NOT NULL DELETE FROM CartItem;
IF OBJECT_ID('Cart', 'U') IS NOT NULL DELETE FROM Cart;
IF OBJECT_ID('Coupon', 'U') IS NOT NULL DELETE FROM Coupon;
IF OBJECT_ID('PayoutRequest', 'U') IS NOT NULL DELETE FROM PayoutRequest;
IF OBJECT_ID('InventoryTransaction', 'U') IS NOT NULL DELETE FROM InventoryTransaction;
IF OBJECT_ID('ImportNoteDetail', 'U') IS NOT NULL DELETE FROM ImportNoteDetail;
IF OBJECT_ID('ImportNote', 'U') IS NOT NULL DELETE FROM ImportNote;
IF OBJECT_ID('ProductImage', 'U') IS NOT NULL DELETE FROM ProductImage;
IF OBJECT_ID('ProductVariant', 'U') IS NOT NULL DELETE FROM ProductVariant;
IF OBJECT_ID('Product', 'U') IS NOT NULL DELETE FROM Product;
IF OBJECT_ID('MasterCategory', 'U') IS NOT NULL DELETE FROM MasterCategory;
IF OBJECT_ID('[Address]', 'U') IS NOT NULL DELETE FROM [Address];
IF OBJECT_ID('CustomerMembership', 'U') IS NOT NULL DELETE FROM CustomerMembership;
IF OBJECT_ID('MembershipTier', 'U') IS NOT NULL DELETE FROM MembershipTier;
IF OBJECT_ID('Shipper', 'U') IS NOT NULL DELETE FROM Shipper;
IF OBJECT_ID('Shop', 'U') IS NOT NULL DELETE FROM Shop;
IF OBJECT_ID('Notification', 'U') IS NOT NULL DELETE FROM Notification;
IF OBJECT_ID('UserRole', 'U') IS NOT NULL DELETE FROM UserRole;
IF OBJECT_ID('[User]', 'U') IS NOT NULL DELETE FROM [User];
IF OBJECT_ID('[Role]', 'U') IS NOT NULL DELETE FROM [Role];

-- ----------------------------------------------------------------------------------------------------
-- 2. TẠO DỮ LIỆU BẢNG [Role]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [Role]...';
SET IDENTITY_INSERT [Role] ON;
INSERT INTO [Role] (RoleId, roleName) VALUES 
(1, 'ADMIN'),
(2, 'STAFF'),
(3, 'SHOP_OWNER'),
(4, 'CUSTOMER'),
(5, 'DELIVERY_STAFF');
SET IDENTITY_INSERT [Role] OFF;

-- ----------------------------------------------------------------------------------------------------
-- 3. TẠO DỮ LIỆU BẢNG [User]
-- BCrypt Hash:
-- admin123   : $2a$10$PyVZBBx12/EKeLlFFVLEt.dlYewU1BDjLR7pxPF7KQ5aIyZJABxxi
-- 1234567890 : $2a$10$fF6CUrlQslFM00lb9txlyeUIBtCwRKvIbPBg/Sp64HLCORlLq9rtC
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [User]...';
SET IDENTITY_INSERT [User] ON;
INSERT INTO [User] (UserId, Username, PasswordHash, Email, FullName, Phone, AvatarUrl, Status) VALUES
-- Admin
(1, 'admin', '$2a$10$PyVZBBx12/EKeLlFFVLEt.dlYewU1BDjLR7pxPF7KQ5aIyZJABxxi', 'admin@betafruit.vn', N'Quản Trị Viên BetaFruit', '0901000001', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150', 'ACTIVE'),

-- Staff
(2, 'staff_minh', '$2a$10$fF6CUrlQslFM00lb9txlyeUIBtCwRKvIbPBg/Sp64HLCORlLq9rtC', 'staff.minh@betafruit.vn', N'Nguyễn Hoàng Minh (Kiểm Duyệt)', '0901000002', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150', 'ACTIVE'),

-- Shop Owners
(3, 'shop_dalat', '$2a$10$fF6CUrlQslFM00lb9txlyeUIBtCwRKvIbPBg/Sp64HLCORlLq9rtC', 'dalatfruit@gmail.com', N'Trần Văn Đà Lạt', '0912345671', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', 'ACTIVE'),
(4, 'shop_mientay', '$2a$10$fF6CUrlQslFM00lb9txlyeUIBtCwRKvIbPBg/Sp64HLCORlLq9rtC', 'mientayfarm@gmail.com', N'Lê Thị Miền Tây', '0912345672', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150', 'ACTIVE'),
(5, 'shop_import', '$2a$10$fF6CUrlQslFM00lb9txlyeUIBtCwRKvIbPBg/Sp64HLCORlLq9rtC', 'organicfruit@gmail.com', N'Phạm Quốc Tế', '0912345673', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150', 'ACTIVE'),

-- Shipper
(6, 'shipper_nam', '$2a$10$fF6CUrlQslFM00lb9txlyeUIBtCwRKvIbPBg/Sp64HLCORlLq9rtC', 'shipper.nam@gmail.com', N'Đỗ Thành Nam (Giao Hàng)', '0933221100', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150', 'ACTIVE'),

-- Customers
(7, 'customer_phuc', '$2a$10$fF6CUrlQslFM00lb9txlyeUIBtCwRKvIbPBg/Sp64HLCORlLq9rtC', 'phucnguyen@gmail.com', N'Nguyễn Phúc Hoàng', '0988776655', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', 'ACTIVE'),
(8, 'customer_hoa', '$2a$10$fF6CUrlQslFM00lb9txlyeUIBtCwRKvIbPBg/Sp64HLCORlLq9rtC', 'hoanguyen@gmail.com', N'Nguyễn Thị Kim Hoa', '0977889900', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150', 'ACTIVE'),
(9, 'customer_linh', '$2a$10$fF6CUrlQslFM00lb9txlyeUIBtCwRKvIbPBg/Sp64HLCORlLq9rtC', 'linhpham@gmail.com', N'Phạm Thùy Linh', '0966554433', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150', 'ACTIVE');
SET IDENTITY_INSERT [User] OFF;

-- ----------------------------------------------------------------------------------------------------
-- 4. BẢNG PHÂN QUYỀN [UserRole]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang gán quyền UserRole...';
INSERT INTO UserRole (UserId, RoleId) VALUES
(1, 1), -- Admin
(2, 2), -- Staff
(3, 3), -- Shop Đà Lạt
(4, 3), -- Shop Miền Tây
(5, 3), -- Shop Import
(6, 5), -- Shipper
(7, 4), -- Customer Phúc
(8, 4), -- Customer Hoa
(9, 4); -- Customer Linh

-- ----------------------------------------------------------------------------------------------------
-- 5. BẢNG HẠNG THÀNH VIÊN [MembershipTier]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [MembershipTier]...';
SET IDENTITY_INSERT MembershipTier ON;
INSERT INTO MembershipTier (TierId, TierName, MinSpentRequired, DiscountPercent) VALUES
(1, N'Thành viên Đồng', 0.00, 0.00),
(2, N'Thành viên Bạc', 1000000.00, 2.00),
(3, N'Thành viên Vàng', 5000000.00, 5.00),
(4, N'Thành viên Kim Cương', 15000000.00, 10.00);
SET IDENTITY_INSERT MembershipTier OFF;

-- ----------------------------------------------------------------------------------------------------
-- 6. BẢNG THÀNH VIÊN KHÁCH HÀNG [CustomerMembership]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang liên kết hạng hội viên cho khách hàng [CustomerMembership]...';
INSERT INTO CustomerMembership (UserId, TotalCumulativeSpend, TierId) VALUES
(7, 5450000.00, 3), -- Customer Phúc: Vàng
(8, 1250000.00, 2), -- Customer Hoa: Bạc
(9, 250000.00, 1);  -- Customer Linh: Đồng

-- ----------------------------------------------------------------------------------------------------
-- 7. BẢNG CỬA HÀNG [Shop]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [Shop]...';
SET IDENTITY_INSERT Shop ON;
INSERT INTO Shop (ShopId, OwnerId, ShopName, ShopDescription, LogoUrl, BannerUrl, BankName, BankAccountNumber, AccountName, AvailableBalance, CommissionRate, ApprovalStatus) VALUES
(1, 3, N'Nông Sản Đà Lạt Xanh', N'Chuyên cung cấp dâu tây giống Nhật, bơ sáp 034, phúc bồn tử và hoa quả sạch thu hoạch tận vườn tại Đà Lạt.', 'https://images.unsplash.com/photo-1610832958506-aa56368176cf?w=200', 'https://images.unsplash.com/photo-1464226184884-fa280b87c399?w=1000', N'Ngân hàng Vietcombank', '0071001234567', N'TRAN VAN DA LAT', 25600000.00, 5.00, 'APPROVED'),
(2, 4, N'Trái Cây Miền Tây Đặc Sản', N'Vựa trái cây Tiền Giang - Bến Tre: Sầu riêng Ri6 chín tự nhiên, xoài cát Hòa Lộc, bưởi da xanh ruột hồng xuất khẩu.', 'https://images.unsplash.com/photo-1546548970-71785318a17b?w=200', 'https://images.unsplash.com/photo-1550989460-0adf9ea622e2?w=1000', N'Ngân hàng Techcombank', '19034567891011', N'LE THI MIEN TAY', 18450000.00, 5.00, 'APPROVED'),
(3, 5, N'Organic & Premium Fruit Imports', N'Nhập khẩu chính ngạch các loại trái cây thượng hạng: Nho mẫu đơn Shine Muscat Hàn Quốc, Cherry đỏ Mỹ, Táo Envy New Zealand.', 'https://images.unsplash.com/photo-1519996529931-28324d5a630e?w=200', 'https://images.unsplash.com/photo-1528825871115-3581a5387919?w=1000', N'Ngân hàng MBBank', '0388999888999', N'PHAM QUOC TE', 42000000.00, 5.00, 'APPROVED');
SET IDENTITY_INSERT Shop OFF;

-- ----------------------------------------------------------------------------------------------------
-- 8. BẢNG [Shipper]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [Shipper]...';
INSERT INTO Shipper (UserId, CodDebt) VALUES
(6, 450000.00);

-- ----------------------------------------------------------------------------------------------------
-- 9. BẢNG ĐỊA CHỈ [Address]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [Address]...';
SET IDENTITY_INSERT [Address] ON;
INSERT INTO [Address] (AddressId, UserId, ReceiverName, ReceiverPhone, AddressLine, City, District, Ward, IsDefault, IsActive, AddressType) VALUES
-- Địa chỉ khách hàng Phúc
(1, 7, N'Nguyễn Phúc Hoàng', '0988776655', N'Số 88 Phố Huế', N'Hà Nội', N'Quận Hai Bà Trưng', N'Phường Hàng Bài', 1, 1, 'SHIPPING'),
(2, 7, N'Nguyễn Phúc Hoàng (Công ty)', '0988776655', N'Tòa nhà FPT Tower, Số 10 Phạm Văn Bạch', N'Hà Nội', N'Quận Cầu Giấy', N'Phường Dịch Vọng', 0, 1, 'SHIPPING'),
-- Địa chỉ khách hàng Hoa
(3, 8, N'Nguyễn Thị Kim Hoa', '0977889900', N'125 Đường Lê Lợi', N'TP. Hồ Chí Minh', N'Quận 1', N'Phường Bến Nghé', 1, 1, 'SHIPPING'),
-- Địa chỉ khách hàng Linh
(4, 9, N'Phạm Thùy Linh', '0966554433', N'45 Nguyễn Văn Cừ', N'Đà Nẵng', N'Quận Hải Châu', N'Phường Hải Châu 1', 1, 1, 'SHIPPING'),
-- Địa chỉ kho của Shop
(5, 3, N'Kho Đà Lạt Xanh', '0912345671', N'Tổ 5 Phường 7', N'Lâm Đồng', N'TP. Đà Lạt', N'Phường 7', 1, 1, 'PICKUP'),
(6, 4, N'Vựa Trái Cây Miền Tây', '0912345672', N'QL1A, Ấp Tân Bình', N'Tiền Giang', N'Huyện Cái Bè', N'Xã Tân Thanh', 1, 1, 'PICKUP'),
(7, 5, N'Kho Trái Cây Nhập Khẩu', '0912345673', N'22/4 Đường Tân Sơn Nhất', N'TP. Hồ Chí Minh', N'Quận Tân Bình', N'Phường 2', 1, 1, 'PICKUP');
SET IDENTITY_INSERT [Address] OFF;

-- ----------------------------------------------------------------------------------------------------
-- 10. BẢNG DANH MỤC NÔNG SẢN [MasterCategory]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [MasterCategory]...';
SET IDENTITY_INSERT MasterCategory ON;
INSERT INTO MasterCategory (CategoryId, CategoryName, IsActive) VALUES
(1, N'Trái Cây Nội Địa', 1),
(2, N'Trái Cây Nhập Khẩu', 1),
(3, N'Trái Cây Hữu Cơ (Organic)', 1),
(4, N'Hộp Quà & Giỏ Trái Cây', 1),
(5, N'Trái Cây Sấy & Chế Biến', 1);
SET IDENTITY_INSERT MasterCategory OFF;

-- ----------------------------------------------------------------------------------------------------
-- 11. BẢNG SẢN PHẨM [Product]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [Product]...';
SET IDENTITY_INSERT Product ON;
INSERT INTO Product (ProductId, ShopId, CategoryId, ProductName, Description, ApprovalStatus, VisibilityStatus) VALUES
-- Shop 1 (Đà Lạt)
(1, 1, 3, N'Dâu Tây Giống Nhật Cao Cấp Đà Lạt', N'Dâu tây giống Nhật Bản trồng theo phương pháp bán thủy canh giá thể xơ dừa trong nhà kính tại Đà Lạt. Quả đỏ mọng, vị ngọt thơm thanh khiết, không sử dụng thuốc trừ sâu hóa học.', 'APPROVED', 'ACTIVE'),
(2, 1, 1, N'Bơ Sáp 034 Lâm Đồng Cơm Vàng Dẻo', N'Giống bơ sáp 034 Lâm Đồng quả dài, vỏ xanh bóng, thịt cơm vàng ươm, dẻo quánh, béo ngậy. Hái già tận cây bao chín.', 'APPROVED', 'ACTIVE'),
(3, 1, 5, N'Hồng Treo Gió Công Nghệ Nhật Bản', N'Hồng trứng Đà Lạt gọt vỏ treo gió tự nhiên từ 25-30 ngày theo công nghệ Hoshigaki Nhật Bản. Bên ngoài dẻo dai, bên trong tươm mật ngọt ngào.', 'APPROVED', 'ACTIVE'),

-- Shop 2 (Miền Tây)
(4, 2, 1, N'Sầu Riêng Ri6 Chín Cây Tiền Giang', N'Sầu riêng Ri6 vườn Cái Bè - Tiền Giang cơm vàng hạt lép, múi khô ráo, ngọt đậm đà, mùi thơm nồng nàn đặc trưng. Cam kết 1 đổi 1 nếu sượng hỏng.', 'APPROVED', 'ACTIVE'),
(5, 2, 1, N'Xoài Cát Hòa Lộc Tiêu Chuẩn Xuất Khẩu', N'Đặc sản xoài cát Hòa Lộc nức tiếng miền Tây. Trái to đều từ 450g-600g, khi chín da vàng óng, thịt mịn không xơ, ngọt lịm hương thơm quyến rũ.', 'APPROVED', 'ACTIVE'),
(6, 2, 1, N'Bưởi Da Xanh Ruột Hồng Bến Tre', N'Bưởi da xanh vỏ mỏng, tép bưởi hồng tươi, mọng nước, vị ngọt thanh chua dịu không the đắng.', 'APPROVED', 'ACTIVE'),

-- Shop 3 (Nhập khẩu)
(7, 3, 2, N'Nho Mẫu Đơn Shine Muscat Hàn Quốc VIP', N'Nho mẫu đơn nhập khẩu trực tiếp Hàn Quốc. Chùm bắp ngô quả to tròn, vỏ mỏng ăn cả vỏ không chát, hương thơm mùi xoài quyến rũ đặc sắc.', 'APPROVED', 'ACTIVE'),
(8, 3, 2, N'Cherry Đỏ Mỹ Size 9.5 Premium', N'Cherry đỏ Washington Mỹ quả to căng bóng, giòn ngọt đẫm nước, cuống tươi xanh mướt. Giàu chất chống oxy hóa và vitamin C.', 'APPROVED', 'ACTIVE'),
(9, 3, 4, N'Hộp Quà Trái Cây Thượng Hạng BetaFruit', N'Hộp quà sang trọng gồm Nho mẫu đơn, Cherry Mỹ, Táo Envy, Dâu Nhật và Cam vàng Úc, trang trí hoa tươi nghệ thuật làm quà biếu đối tác, người thân.', 'APPROVED', 'ACTIVE');
SET IDENTITY_INSERT Product OFF;

-- ----------------------------------------------------------------------------------------------------
-- 12. BẢNG PHÂN LOẠI SẢN PHẨM [ProductVariant]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [ProductVariant]...';
SET IDENTITY_INSERT ProductVariant ON;
INSERT INTO ProductVariant (VariantId, ProductId, VariantName, OriginalPrice, SalePrice, CostPrice, StockQuantity, LowStockThreshold, IsAvailable) VALUES
-- Dâu tây (Product 1)
(1, 1, N'Hộp 500g (Loại 1)', 150000.00, 135000.00, 95000.00, 48, 10, 1),
(2, 1, N'Hộp 1kg (Loại VIP)', 290000.00, 260000.00, 185000.00, 25, 5, 1),

-- Bơ 034 (Product 2)
(3, 2, N'Túi 1kg (3-4 quả)', 65000.00, 55000.00, 35000.00, 120, 20, 1),
(4, 2, N'Thùng 5kg Tiết Kiệm', 300000.00, 250000.00, 160000.00, 30, 5, 1),

-- Hồng treo gió (Product 3)
(5, 3, N'Hộp 500g Hút Chân Không', 180000.00, 165000.00, 110000.00, 40, 10, 1),

-- Sầu riêng Ri6 (Product 4)
(6, 4, N'Quả nguyên trái (2.5kg - 3.2kg)', 380000.00, 345000.00, 240000.00, 15, 3, 1),
(7, 4, N'Khay cơm tách sẵn 500g', 220000.00, 195000.00, 140000.00, 35, 8, 1),

-- Xoài cát Hòa Lộc (Product 5)
(8, 5, N'Hộp 2kg (4-5 quả)', 190000.00, 170000.00, 115000.00, 50, 10, 1),

-- Bưởi da xanh (Product 6)
(9, 6, N'Quả 1.3kg - 1.6kg', 75000.00, 68000.00, 45000.00, 80, 15, 1),

-- Nho mẫu đơn (Product 7)
(10, 7, N'Chùm 600g - 750g Hộp Nhựa', 520000.00, 480000.00, 360000.00, 20, 5, 1),
(11, 7, N'Hộp Quà 2 Chùm VIP (1.5kg)', 1150000.00, 1050000.00, 780000.00, 8, 2, 1),

-- Cherry đỏ Mỹ (Product 8)
(12, 8, N'Hộp 500g Size 9.5', 280000.00, 250000.00, 180000.00, 45, 10, 1),
(13, 8, N'Hộp 1kg Size 9.5 VIP', 540000.00, 490000.00, 350000.00, 22, 5, 1),

-- Hộp quà trái cây (Product 9)
(14, 9, N'Set Tiêu Chuẩn Ngũ Quả', 650000.00, 590000.00, 420000.00, 15, 3, 1),
(15, 9, N'Set Thượng Hạng Sang Trọng', 1250000.00, 1150000.00, 800000.00, 6, 2, 1);
SET IDENTITY_INSERT ProductVariant OFF;

-- ----------------------------------------------------------------------------------------------------
-- 13. BẢNG HÌNH ẢNH SẢN PHẨM [ProductImage]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [ProductImage]...';
SET IDENTITY_INSERT ProductImage ON;
INSERT INTO ProductImage (ImageId, ProductId, ImageUrl, IsPrimary) VALUES
(1, 1, 'https://images.unsplash.com/photo-1543528176-61b239494933?w=600', 1),
(2, 1, 'https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=600', 0),
(3, 2, 'https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=600', 1),
(4, 3, 'https://images.unsplash.com/photo-1568584711271-6c929fb49b60?w=600', 1),
(5, 4, 'https://images.unsplash.com/photo-1587132137056-bfbf0166836e?w=600', 1),
(6, 5, 'https://images.unsplash.com/photo-1553279768-865429fa0078?w=600', 1),
(7, 6, 'https://images.unsplash.com/photo-1577234286642-fc512a5f8f11?w=600', 1),
(8, 7, 'https://images.unsplash.com/photo-1537640538966-79f369143f8f?w=600', 1),
(9, 8, 'https://images.unsplash.com/photo-1528825871115-3581a5387919?w=600', 1),
(10, 9, 'https://images.unsplash.com/photo-1610832958506-aa56368176cf?w=600', 1);
SET IDENTITY_INSERT ProductImage OFF;

-- ----------------------------------------------------------------------------------------------------
-- 14. BẢNG PHIẾU NHẬP KHO [ImportNote] & CHI TIẾT [ImportNoteDetail]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [ImportNote] và [ImportNoteDetail]...';
SET IDENTITY_INSERT ImportNote ON;
INSERT INTO ImportNote (ImportId, ShopId, TotalCost, Note) VALUES
(1, 1, 9500000.00, N'Nhập dâu tây giống Nhật đợt 1 từ vườn bán thủy canh'),
(2, 2, 12000000.00, N'Thu mua sầu riêng Ri6 và xoài cát Hòa Lộc Tiền Giang'),
(3, 3, 25000000.00, N'Nhập container đường bay Nho Mẫu Đơn và Cherry Mỹ');
SET IDENTITY_INSERT ImportNote OFF;

SET IDENTITY_INSERT ImportNoteDetail ON;
INSERT INTO ImportNoteDetail (DetailId, ImportId, VariantId, Quantity, ImportPrice) VALUES
-- Phiếu 1 của Shop 1
(1, 1, 1, 60, 95000.00),
(2, 1, 2, 30, 185000.00),
-- Phiếu 2 của Shop 2
(3, 2, 6, 20, 240000.00),
(4, 2, 7, 50, 140000.00),
-- Phiếu 3 của Shop 3
(5, 3, 10, 30, 360000.00),
(6, 3, 12, 60, 180000.00);
SET IDENTITY_INSERT ImportNoteDetail OFF;

-- ----------------------------------------------------------------------------------------------------
-- 15. BẢNG LỊCH SỬ KHO [InventoryTransaction]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [InventoryTransaction]...';
SET IDENTITY_INSERT InventoryTransaction ON;
INSERT INTO InventoryTransaction (TransactionId, VariantId, UserId, TransactionType, QuantityChanged, PreviousStock, NewStock) VALUES
(1, 1, 3, 'RESTOCK', 60, 0, 60),
(2, 2, 3, 'RESTOCK', 30, 0, 30),
(3, 1, 7, 'ORDER_DEDUCT', -2, 60, 58),
(4, 6, 4, 'RESTOCK', 20, 0, 20),
(5, 10, 5, 'RESTOCK', 30, 0, 30);
SET IDENTITY_INSERT InventoryTransaction OFF;

-- ----------------------------------------------------------------------------------------------------
-- 16. BẢNG MÃ KHUYẾN MÃI [Coupon]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [Coupon]...';
SET IDENTITY_INSERT Coupon ON;
INSERT INTO Coupon (CouponId, ShopId, CouponCode, CouponCategory, DiscountType, DiscountValue, MinOrderValue, MaxDiscountAmount, UsageLimit, UsedCount, IsActive, StartDate, EndDate) VALUES
-- Mã toàn sàn của BetaFruit (ShopId = NULL)
(1, NULL, 'FREESHIP50', 'SHIPPING', 'FIXED_AMOUNT', 30000.00, 200000.00, 30000.00, 500, 24, 1, '2026-01-01', '2026-12-31'),
(2, NULL, 'BETAFRUIT10', 'PRODUCT', 'PERCENT', 10.00, 300000.00, 50000.00, 200, 18, 1, '2026-01-01', '2026-12-31'),
-- Mã của riêng Shop Đà Lạt
(3, 1, 'DALAT20K', 'PRODUCT', 'FIXED_AMOUNT', 20000.00, 150000.00, 20000.00, 100, 9, 1, '2026-01-01', '2026-12-31'),
-- Mã của riêng Shop Miền Tây
(4, 2, 'MIENTAY5P', 'PRODUCT', 'PERCENT', 5.00, 250000.00, 30000.00, 100, 5, 1, '2026-01-01', '2026-12-31');
SET IDENTITY_INSERT Coupon OFF;

-- ----------------------------------------------------------------------------------------------------
-- 17. BẢNG GIỎ HÀNG [Cart] & [CartItem]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [Cart] và [CartItem]...';
SET IDENTITY_INSERT Cart ON;
INSERT INTO Cart (CartId, CustomerId) VALUES
(1, 7), -- Phúc
(2, 8), -- Hoa
(3, 9); -- Linh
SET IDENTITY_INSERT Cart OFF;

SET IDENTITY_INSERT CartItem ON;
INSERT INTO CartItem (CartItemId, CartId, VariantId, Quantity) VALUES
(1, 1, 1, 2),  -- Phúc có 2 hộp dâu tây 500g trong giỏ
(2, 1, 3, 1),  -- Phúc có 1 túi bơ sáp trong giỏ
(3, 2, 7, 1),  -- Hoa có 1 khay sầu riêng Ri6 trong giỏ
(4, 3, 10, 1); -- Linh có 1 chùm nho mẫu đơn
SET IDENTITY_INSERT CartItem OFF;

-- ----------------------------------------------------------------------------------------------------
-- 18. BẢNG ĐƠN HÀNG [Order]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [Order]...';

-- Đảm bảo Check Constraints trên bảng [Order] hỗ trợ đúng các Enum trong Java (VNPAY và DELIVERED)
IF EXISTS (SELECT * FROM sys.check_constraints WHERE name = 'CHK_Order_Payment')
BEGIN
    ALTER TABLE [Order] DROP CONSTRAINT CHK_Order_Payment;
    ALTER TABLE [Order] ADD CONSTRAINT CHK_Order_Payment CHECK (PaymentMethod IN ('ONLINE', 'COD', 'VNPAY'));
END

IF EXISTS (SELECT * FROM sys.check_constraints WHERE name = 'CHK_Order_Status')
BEGIN
    ALTER TABLE [Order] DROP CONSTRAINT CHK_Order_Status;
    ALTER TABLE [Order] ADD CONSTRAINT CHK_Order_Status CHECK (OrderStatus IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'PICKED_UP', 'DELIVERING', 'DELIVERED', 'SUCCESS', 'FAILED', 'CANCELLED', 'REJECTED'));
END

SET IDENTITY_INSERT [Order] ON;
INSERT INTO [Order] (OrderId, CustomerId, ShopId, CouponId, SnapshotShippingAddress, TotalAmount, DiscountAmount, ShippingFee, FinalAmount, PlatformFee, ShopNetReceived, PaymentMethod, OrderStatus, IsSettled, IsCODRemitted) VALUES
-- Đơn hàng 1: Khách Phúc mua dâu Đà Lạt (Thanh toán VNPAY, đã hoàn tất thành công)
(1, 7, 1, 3, N'Nguyễn Phúc Hoàng - 0988776655, Số 88 Phố Huế, Phường Hàng Bài, Quận Hai Bà Trưng, Hà Nội', 270000.00, 20000.00, 25000.00, 275000.00, 13500.00, 256500.00, 'VNPAY', 'SUCCESS', 1, 0),

-- Đơn hàng 2: Khách Phúc mua sầu riêng Tiền Giang (COD, đã giao thành công)
(2, 7, 2, NULL, N'Nguyễn Phúc Hoàng - 0988776655, Tòa nhà FPT Tower, Số 10 Phạm Văn Bạch, Phường Dịch Vọng, Quận Cầu Giấy, Hà Nội', 345000.00, 0.00, 30000.00, 375000.00, 17250.00, 327750.00, 'COD', 'DELIVERED', 0, 1),

-- Đơn hàng 3: Khách Hoa mua Nho Mẫu Đơn Shop Import (VNPAY, đang giao)
(3, 8, 3, 2, N'Nguyễn Thị Kim Hoa - 0977889900, 125 Đường Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh', 480000.00, 48000.00, 20000.00, 452000.00, 24000.00, 456000.00, 'VNPAY', 'DELIVERING', 0, 0),

-- Đơn hàng 4: Khách Linh mua bơ sáp Đà Lạt (COD, đang chuẩn bị hàng)
(4, 9, 1, NULL, N'Phạm Thùy Linh - 0966554433, 45 Nguyễn Văn Cừ, Phường Hải Châu 1, Quận Hải Châu, Đà Nẵng', 110000.00, 0.00, 20000.00, 130000.00, 5500.00, 104500.00, 'COD', 'PREPARING', 0, 0),

-- Đơn hàng 5: Khách Phúc mua Cherry đỏ (VNPAY, đã hủy đơn)
(5, 7, 3, NULL, N'Nguyễn Phúc Hoàng - 0988776655, Số 88 Phố Huế, Phường Hàng Bài, Quận Hai Bà Trưng, Hà Nội', 250000.00, 0.00, 25000.00, 275000.00, 12500.00, 237500.00, 'VNPAY', 'CANCELLED', 0, 0);
SET IDENTITY_INSERT [Order] OFF;

-- ----------------------------------------------------------------------------------------------------
-- 19. BẢNG CHI TIẾT ĐƠN HÀNG [OrderItem]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [OrderItem]...';
SET IDENTITY_INSERT OrderItem ON;
INSERT INTO OrderItem (OrderItemId, OrderId, VariantId, Quantity, PriceSnapshot, CostSnapshot, SnapshotProductName, SnapshotVariantName) VALUES
-- Cho Đơn 1
(1, 1, 1, 2, 135000.00, 95000.00, N'Dâu Tây Giống Nhật Cao Cấp Đà Lạt', N'Hộp 500g (Loại 1)'),
-- Cho Đơn 2
(2, 2, 6, 1, 345000.00, 240000.00, N'Sầu Riêng Ri6 Chín Cây Tiền Giang', N'Quả nguyên trái (2.5kg - 3.2kg)'),
-- Cho Đơn 3
(3, 3, 10, 1, 480000.00, 360000.00, N'Nho Mẫu Đơn Shine Muscat Hàn Quốc VIP', N'Chùm 600g - 750g Hộp Nhựa'),
-- Cho Đơn 4
(4, 4, 3, 2, 55000.00, 35000.00, N'Bơ Sáp 034 Lâm Đồng Cơm Vàng Dẻo', N'Túi 1kg (3-4 quả)'),
-- Cho Đơn 5
(5, 5, 12, 1, 250000.00, 180000.00, N'Cherry Đỏ Mỹ Size 9.5 Premium', N'Hộp 500g Size 9.5');
SET IDENTITY_INSERT OrderItem OFF;

-- ----------------------------------------------------------------------------------------------------
-- 20. BẢNG LỊCH SỬ TRẠNG THÁI ĐƠN [OrderStatusHistory]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [OrderStatusHistory]...';
SET IDENTITY_INSERT OrderStatusHistory ON;
INSERT INTO OrderStatusHistory (HistoryId, OrderId, UpdatedBy, StatusFrom, StatusTo, Note) VALUES
(1, 1, 7, NULL, 'PENDING', N'Khách hàng tạo đơn và thanh toán qua VNPAY'),
(2, 1, 3, 'PENDING', 'CONFIRMED', N'Chủ shop đã xác nhận có đủ dâu tươi'),
(3, 1, 3, 'CONFIRMED', 'READY', N'Đã đóng thùng xốp bảo quản lạnh sẵn sàng giao'),
(4, 1, 6, 'READY', 'DELIVERED', N'Shipper giao hàng thành công đến tay khách'),
(5, 1, 1, 'DELIVERED', 'SUCCESS', N'Hệ thống quyết toán đơn hàng hoàn tất'),
(6, 2, 7, NULL, 'PENDING', N'Khách hàng đặt đơn chọn phương thức COD'),
(7, 2, 6, 'DELIVERING', 'DELIVERED', N'Shipper Nam đã thu tiền COD 375.000đ'),
(8, 5, 7, 'PENDING', 'CANCELLED', N'Khách hàng thay đổi ý định muốn đổi size khác');
SET IDENTITY_INSERT OrderStatusHistory OFF;

-- ----------------------------------------------------------------------------------------------------
-- 21. BẢNG GIAO DỊCH THANH TOÁN TRỰC TUYẾN [PaymentTransaction]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [PaymentTransaction]...';
SET IDENTITY_INSERT PaymentTransaction ON;
INSERT INTO PaymentTransaction (TransactionId, OrderId, Gateway, TransactionRef, Amount, Status) VALUES
(1, 1, 'VNPAY', 'TXN2026102001', 275000.00, 'PAID'),
(2, 3, 'VNPAY', 'TXN2026102002', 452000.00, 'PAID'),
(3, 5, 'VNPAY', 'TXN2026102003', 275000.00, 'FAILED');
SET IDENTITY_INSERT PaymentTransaction OFF;

-- ----------------------------------------------------------------------------------------------------
-- 22. BẢNG PHÂN CÔNG GIAO HÀNG [DeliveryAssignment]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [DeliveryAssignment]...';
SET IDENTITY_INSERT DeliveryAssignment ON;
INSERT INTO DeliveryAssignment (AssignmentId, OrderId, ShipperId, AssignedBy) VALUES
(1, 1, 6, 2), -- Đơn 1 giao cho shipper Nam
(2, 2, 6, 2), -- Đơn 2 giao cho shipper Nam
(3, 3, 6, 2); -- Đơn 3 giao cho shipper Nam
SET IDENTITY_INSERT DeliveryAssignment OFF;

-- ----------------------------------------------------------------------------------------------------
-- 23. BẢNG THU HỘ TIỀN COD [CODCollection] & [CODDetail]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [CODCollection] và [CODDetail]...';
SET IDENTITY_INSERT CODCollection ON;
INSERT INTO CODCollection (CollectionId, ShipperId, AmountCollected, CollectedBy) VALUES
(1, 6, 375000.00, 2); -- Staff Minh đối soát nhận nộp tiền COD từ Shipper Nam
SET IDENTITY_INSERT CODCollection OFF;

SET IDENTITY_INSERT CODDetail ON;
INSERT INTO CODDetail (DetailId, CollectionId, OrderId, Amount) VALUES
(1, 1, 2, 375000.00);
SET IDENTITY_INSERT CODDetail OFF;

-- ----------------------------------------------------------------------------------------------------
-- 24. BẢNG ĐÁNH GIÁ SẢN PHẨM [Review] & MEDIA [ReviewMedia]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [Review] và [ReviewMedia]...';
SET IDENTITY_INSERT Review ON;
INSERT INTO Review (ReviewId, OrderItemId, CustomerId, Rating, Comment, ShopReply, VisibilityStatus, IsReported) VALUES
(1, 1, 7, 5, N'Dâu tây tươi rói, quả thơm ngát ngọt lịm không hề bị dập nát quả nào. Shop đóng gói thùng xốp rất cẩn thận!', N'Shop Nông Sản Đà Lạt Xanh cảm ơn anh Phúc rất nhiều ạ! Chúc anh ngon miệng và luôn ủng hộ shop nhé!', 'VISIBLE', 0);
SET IDENTITY_INSERT Review OFF;

SET IDENTITY_INSERT ReviewMedia ON;
INSERT INTO ReviewMedia (MediaId, ReviewId, MediaUrl, MediaType) VALUES
(1, 1, 'https://images.unsplash.com/photo-1543528176-61b239494933?w=500', 'IMAGE');
SET IDENTITY_INSERT ReviewMedia OFF;

-- ----------------------------------------------------------------------------------------------------
-- 25. BẢNG YÊU CẦU RÚT TIỀN [PayoutRequest]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [PayoutRequest]...';
SET IDENTITY_INSERT PayoutRequest ON;
INSERT INTO PayoutRequest (RequestId, ShopId, RequestedAmount, BankName, BankAccountNumber, AccountName, Status, ProcessedBy) VALUES
(1, 1, 5000000.00, N'Ngân hàng Vietcombank', '0071001234567', N'TRAN VAN DA LAT', 'COMPLETED', 1),
(2, 2, 3000000.00, N'Ngân hàng Techcombank', '19034567891011', N'LE THI MIEN TAY', 'PENDING', NULL);
SET IDENTITY_INSERT PayoutRequest OFF;

-- ----------------------------------------------------------------------------------------------------
-- 26. BẢNG THÔNG BÁO [Notification]
-- ----------------------------------------------------------------------------------------------------
PRINT N'==> Đang thêm dữ liệu bảng [Notification]...';
SET IDENTITY_INSERT Notification ON;
INSERT INTO Notification (NotificationId, UserId, Title, Message, NotificationType, ActionUrl, IsRead, IsHidden) VALUES
(1, 7, N'Chào mừng thành viên mới!', N'Chào mừng bạn đến với sàn thương mại điện tử hoa quả sạch BetaFruit!', 'SYSTEM_ALERT', '/', 1, 0),
(2, 7, N'Đơn hàng #DH000001 đã giao thành công', N'Kiện hàng Dâu Tây Đà Lạt của bạn đã được giao thành công. Hãy để lại đánh giá nhé!', 'ORDER_STATUS', '/customer/payment/transaction-list', 0, 0),
(3, 3, N'Đơn hàng mới #DH000001', N'Bạn có 1 đơn hàng mới từ khách hàng Nguyễn Phúc Hoàng.', 'ORDER_STATUS', '/shop/orders', 1, 0),
(4, 1, N'Yêu cầu rút tiền mới', N'Shop Nông Sản Đà Lạt Xanh vừa gửi yêu cầu rút 5.000.000đ về Vietcombank.', 'PAYMENT_UPDATE', '/admin/payouts', 1, 0);
SET IDENTITY_INSERT Notification OFF;

PRINT N'====================================================================================================';
PRINT N'==> TẠO DỮ LIỆU MẪU (SEED DATA) HOÀN TẤT THÀNH CÔNG VỚI 28 BẢNG TRONG HỆ THỐNG BETAFRUIT!';
PRINT N'====================================================================================================';
