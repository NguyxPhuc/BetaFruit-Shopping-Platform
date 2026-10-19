> Lưu trữ ghi chú nhánh tính năng trước hợp nhất. Các endpoint/session/enum/layout trong tài liệu này có thể đã cũ; xem [lộ trình hiện tại](../REFACTOR_ROADMAP.md) để biết trạng thái và hợp đồng sau hợp nhất.

# Luồng DELIVERED và đề xuất chốt đơn

## Đã triển khai
- POST /shipper/orders/{orderId}/delivered lấy UserId từ session CURRENT_USER_ID.
- Chỉ shipper được phân công được chuyển DELIVERING sang DELIVERED.
- Khóa order khi chuyển trạng thái; gọi lại không tạo lịch sử mới hoặc đặt lại mốc giao.
- Ghi lịch sử DELIVERING -> DELIVERED. CreatedAt của lịch sử dùng default hiện có của database.
- Lưu PlatformFee và ShopNetReceived tại thời điểm xác nhận giao; không cộng AvailableBalance hoặc ghi SettledAt.
- Bộ lọc và nhãn shop có DELIVERED; SUCCESS hiển thị là Đơn hoàn tất.

## Thiết kế dữ liệu được làm rõ ngày 15/10/2026

- PaymentMethod quyết định thu COD từ khách hay không. COD thu FinalAmount; ONLINE không thu tiền mặt.
- IsSettled/SettledAt ghi nhận việc thực sự chuyển tiền của đơn vào số dư shop, thường sau 72 giờ không có tranh chấp.
- IsCODRemitted ghi nhận hệ thống thực nhận lại tiền COD từ shipper. Đây là nghiệp vụ nộp COD cuối ngày, độc lập với chuyển tiền cho shop.
- Không dùng IsSettled để đánh dấu đã nộp COD, không dùng IsCODRemitted để đánh dấu đã chuyển tiền cho shop.

Chi tiết dữ liệu/API chuẩn bị cho màn hình shipper nằm trong [shipper-payment-data.md](shipper-payment-data.md). Phần này tận dụng cột hiện có, không thêm schema hoặc chạy SQL. Đề xuất thêm cột tranh chấp trước đây chưa được triển khai; thiết kế tranh chấp và scheduler sẽ được xử lý ở phạm vi riêng.

## Điểm cần lưu ý
- API shipper cần login ghi CURRENT_USER_ID vào session. Hiện source chưa có giao diện shipper/login hoàn chỉnh.
- Phần chốt sau 72 giờ và API tranh chấp CHƯA được áp dụng.
- Không thay đổi chính sách COD chưa nộp/online chưa thanh toán trong task này; nếu muốn chặn chốt theo thanh toán cần quy định thêm.
