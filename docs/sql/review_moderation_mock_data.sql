-- =========================================================================================
-- SCRIPT GIẢ LẬP DỮ LIỆU ĐÁNH GIÁ (REVIEW DISPUTE & MODERATION TEST DATA)
-- DỰ ÁN: BetaFruit Shopping Platform (Iteration 3 - FE-10.2 & FE-10.3)
-- Tác giả: Đặng Quang Khải (HE186918)
-- =========================================================================================

-- 1. Kiểm tra OrderItem hợp lệ trong hệ thống
-- Lưu ý: Đảm bảo OrderItemId tồn tại trong bảng OrderItem và chưa có review
DECLARE @OrderItemId1 INT = (SELECT TOP 1 OrderItemId FROM OrderItem ORDER BY OrderItemId ASC);
DECLARE @CustomerId1 INT = (SELECT TOP 1 UserId FROM [User] WHERE Username = 'customer' OR Username LIKE '%cust%' ORDER BY UserId ASC);

IF @CustomerId1 IS NULL
BEGIN
    SET @CustomerId1 = (SELECT TOP 1 UserId FROM [User] ORDER BY UserId ASC);
END

PRINT N'Sử dụng OrderItemId = ' + CAST(@OrderItemId1 AS NVARCHAR) + N', CustomerId = ' + CAST(@CustomerId1 AS NVARCHAR);

-- 2. Xóa review mẫu cũ (nếu có) để tránh trùng lặp OrderItemId Unique
DELETE FROM ReviewReport WHERE ReviewId IN (SELECT ReviewId FROM Review WHERE OrderItemId = @OrderItemId1);
DELETE FROM Review WHERE OrderItemId = @OrderItemId1;

-- 3. Tạo 1 Đánh giá 1 sao có dấu hiệu vu khống, xúc phạm (để test kịch bản Shop khiếu nại -> Staff duyệt ẩn)
INSERT INTO Review (OrderItemId, CustomerId, Rating, Comment, VisibilityStatus, IsReported, CreatedAt, UpdatedAt)
VALUES (
    @OrderItemId1, 
    @CustomerId1, 
    1, 
    N'Shop làm ăn lừa đảo gian manh, cân thiếu nửa ký nho mẫu đơn, trái cây dập nát thối rữa như rác rưởi!', 
    'VISIBLE', 
    0, 
    DATEADD(DAY, -2, GETDATE()), 
    DATEADD(DAY, -2, GETDATE())
);

PRINT N'>> ĐÃ NẠP REVIEW MẪU THÀNH CÔNG!';
PRINT N'>> Hướng dẫn test:';
PRINT N'1. Đăng nhập tài khoản Shop -> Vào menu "Đánh Giá & Khiếu Nại" (/shop/reviews)';
PRINT N'2. Bấm nút [Báo cáo vi phạm] tại đánh giá 1 sao vừa tạo, chọn lý do và bấm Gửi.';
PRINT N'3. Đăng nhập tài khoản Staff -> Vào menu "Kiểm duyệt Đánh giá" (/staff/reviews)';
PRINT N'4. Bấm [Thụ lý] -> Chọn [Chấp thuận khiếu nại (Ẩn đánh giá vi phạm)] -> Bấm Xác nhận.';
PRINT N'5. Quan sát: Đánh giá chuyển sang "Đã duyệt ẩn", rating sản phẩm tự động loại bỏ review 1 sao này!';
