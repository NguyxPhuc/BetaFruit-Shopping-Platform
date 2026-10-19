> Lưu trữ ghi chú nhánh tính năng trước hợp nhất. Các endpoint/session/enum/layout trong tài liệu này có thể đã cũ; xem [lộ trình hiện tại](../REFACTOR_ROADMAP.md) để biết trạng thái và hợp đồng sau hợp nhất.

# Dữ liệu thanh toán cho màn hình shipper

## Ý nghĩa các trường Order

| Trường | Ý nghĩa |
| --- | --- |
| PaymentMethod | COD: shipper thu từ khách khi giao; ONLINE: không thu tiền mặt. Trường này không phải trạng thái giao dịch cổng thanh toán. |
| FinalAmount | Tổng khách phải trả, đã gồm vận chuyển và giảm giá; đây là số COD phải thu, không phải TotalAmount hoặc ShopNetReceived. |
| IsSettled | Tiền của đơn đã thực sự được cộng vào AvailableBalance của shop hay chưa. Giữ entity gốc: giá trị khởi tạo là null; chỉ true mới có nghĩa đã chuyển tiền. Luồng xác nhận giao thành công ghi false. |
| SettledAt | Thời điểm cộng tiền vào số dư shop, không phải thời điểm giao hoặc nộp COD. |
| IsCODRemitted | Hệ thống đã nhận lại COD từ shipper hay chưa. Chỉ áp dụng cho COD; ONLINE không hiển thị cờ này. |

Hai việc nộp COD và chuyển tiền cho shop độc lập. Không suy ra cờ này từ cờ kia. Thông thường shop nhận tiền sau 72 giờ từ giao thành công nếu không có tranh chấp; xác nhận giao hoặc nộp COD không tự cộng tiền cho shop.

## API đã chuẩn bị

Tạm thời khi chưa đăng nhập, các màn hình và thao tác HTML /deliverer/orders mặc định dùng UserId = 3 để kiểm tra giao diện và đơn được gán. Khi có session thì dùng user của session. Các API JSON vẫn yêu cầu đăng nhập; fallback này không ghi user thử nghiệm vào session.

Màn hình danh sách: GET /deliverer/orders. Sidebar DelivererSidebar bên trái; bảng gồm mã đơn, khách hàng, tổng tiền, thanh toán (COD/Online), trạng thái và nút Xem chi tiết. Có tìm mã/tên khách, lọc thanh toán/trạng thái, sắp xếp và phân trang tại database. COD đang chờ giao hiển thị số cần thu; ONLINE hiển thị không thu tiền mặt.

Màn hình chi tiết: GET /deliverer/orders/{orderId}. Thông tin đơn hàng chiếm toàn chiều ngang ở trên, sản phẩm nằm phía dưới; thanh toán/thu hộ và lịch sử nằm ở phần dưới cùng. Không hiển thị giá vốn hay lợi nhuận của shop cho deliverer. Chỉ đơn đã phân công đúng người mới được đọc.

Luồng thao tác HTML: POST /deliverer/orders/{orderId}/picked-up (READY → PICKED_UP), /delivering (PICKED_UP → DELIVERING), /delivered (DELIVERING → DELIVERED). Bước cuối với COD bắt buộc gửi codCollected=true, được xác nhận qua checkbox trên giao diện. Mỗi bước khóa order, kiểm tra lại người được phân công và trạng thái nguồn; ghi lịch sử cùng transaction. Không cho bỏ bước/lùi trạng thái/tự chuyển SUCCESS. Lỗi xung đột hiển thị lại trên chi tiết.

DeliveryAssignmentService.assign là entry point cho luồng phân công của điều phối sau này: chỉ nhận order READY, shipper phải tồn tại và đơn chưa có phân công; khóa order để ngăn gán trùng. Không mở API tự nhận đơn cho shipper. Màn hình đối soát COD sẽ được làm riêng.

Các API dùng user trong session CURRENT_USER_ID, chỉ đọc đơn được phân công cho user đó. Không nhận shipperId từ client để chọn người khác.

- GET /shipper/orders?page=0&size=10: danh sách phân trang, size tối đa 100, phân công mới nhất trước.
- GET /shipper/orders/{orderId}: chi tiết dữ liệu giao và thanh toán; đơn của shipper khác trả 404.
- GET /shipper/orders/cod-summary: pendingOrderCount và amountToRemit, tính toàn bộ COD đã giao nhưng chưa nộp, kể cả ngày trước, đơn SUCCESS và đơn đã chuyển tiền cho shop. Không tính theo riêng trang hiện tại.
- POST /shipper/orders/{orderId}/picked-up và /delivering: API cho hai bước đầu, chỉ cho người được phân công và đúng trạng thái nguồn.
- POST /shipper/orders/{orderId}/delivered: chỉ shipper được phân công xác nhận khi đơn DELIVERING. Với COD bắt buộc codCollected=true (mặc định false). Gọi lại sau DELIVERED không tạo lịch sử lần hai.

## Các trường trả về cho giao diện

- codAmount: tổng COD của đơn để đối chiếu, ONLINE = 0.
- amountToCollect: số cần thu hiện tại; COD ở READY/PICKED_UP/DELIVERING = FinalAmount, còn lại = 0. Không thu lần hai sau DELIVERED/SUCCESS.
- codHeldAmount: COD đã thu nhưng chưa nộp; chỉ DELIVERED/SUCCESS và IsCODRemitted chưa true.
- codRemittanceStatus: NOT_APPLICABLE (online), NOT_COLLECTED (chưa giao), PENDING_REMITTANCE (đã giao chưa nộp), REMITTED (đã nộp).
- isCODRemitted: giá trị Order cho COD; ONLINE trả null vì không áp dụng. COD null cũ được tính là chưa xác nhận nộp.
- isSettled/settledAt: trạng thái và thời gian chuyển tiền cho shop, giữ nguyên giá trị lưu trong Order.
- canMarkDelivered: đơn DELIVERING và chưa chuyển tiền cho shop.
- customerName/customerPhone/shippingAddress/shopName/orderStatus/paymentMethod/finalAmount: dữ liệu nền để làm màn hình giao hàng.

Ví dụ COD FinalAmount = 115000: đang giao thì amountToCollect = 115000, codHeldAmount = 0; đã giao chưa nộp thì amountToCollect = 0, codHeldAmount = 115000; đã nộp thì cả hai = 0. ONLINE luôn có cả hai = 0.

## Chuẩn bị cho nộp COD cuối ngày

Đã có CODCollection (phiếu hệ thống nhận tiền: ShipperId, AmountCollected, CollectedBy, CreatedAt) và CODDetail (các đơn và số tiền trong phiếu, unique OrderId). Các entity này được giữ nguyên. Chỉ khi hệ thống xác nhận thực nhận tiền mới ghi phiếu/chi tiết và đặt IsCODRemitted = true trong cùng transaction. Không tự đặt true khi giao hàng, khi shipper bấm đã nộp, hoặc chỉ vì đã hết ngày. Không thay IsSettled/SettledAt trong nghiệp vụ nộp COD.

Chưa thêm API xác nhận hệ thống nhận tiền hoặc màn hình nộp COD; đây sẽ là nghiệp vụ tiếp nhận tiền với phân quyền người thu. Tổng COD của API hiện tính từ Order và phân công, không sử dụng Shipper.CodDebt để tránh lệch với cờ từng đơn; không tự ghi đè dữ liệu CodDebt cũ.

## Phạm vi triển khai

Không cần thêm cột hoặc chạy SQL cho phần chuẩn bị này. Không sinh dữ liệu giả vào database. Bộ test dùng H2 và mock.

Theo xác nhận của người dùng, chưa triển khai scheduler chuyển tiền sau 72 giờ vì chưa có dữ liệu tranh chấp. Không suy ra thời điểm hết hạn từ CreatedAt của đơn. Thiết kế tranh chấp và cách xác nhận nhận COD sẽ được nối riêng khi làm các luồng đó.
