# Lộ trình hợp nhất Shop Orders và Shipper

Ngày lập: 19/10/2026. Nguồn yêu cầu: tài liệu người dùng cung cấp trong chat.

## Phạm vi và trạng thái

Tiến độ hiện tại: hoàn tất phần enum gốc của bước 1 và đối chiếu entity theo main ở bước 2.
Bước 1–7 đã hoàn tất trong workspace: source thống nhất ở root, thư mục con đã dọn.
Sau cập nhật note 2.1, toàn bộ 95 test đạt.
Đã khởi động Spring đầy đủ và smoke test COD/VNPay trên H2. Quyết định VNPay/tồn kho sau hợp nhất đã thay đổi theo note 2.1 hiện tại; giới hạn SQL Server/VNPay thật vẫn giữ.
Nguồn chạy hiện tại là `src/` ở gốc. Nguồn tính năng trước hợp nhất là `BetaFruit-Shopping-Platform-main/src/` (đã dọn ở bước 7).
Hợp nhất theo từng file/method, giữ nghiệp vụ hiện có của nhóm; không chép đè toàn bộ source gốc.
Sau mỗi bước cập nhật trạng thái, kết quả kiểm tra và vấn đề còn mở vào tài liệu này.

## Khảo sát ban đầu

- Git working tree sạch trước khi sửa; chưa thấy file AGENTS.md trong project.
- Enum gốc thiếu DELIVERED; PaymentMethod đã là COD/VNPAY; AddressType đã là SHIPPING/PICKUP/RETURN.
- Entity gốc đã có Order.isCodRemitted, CODDetail.codCollection và converter RoleName của Role.
- Nhánh con còn ONLINE, CustomerOrder, isCODRemitted, session CURRENT_USER_ID và ID test hardcode.
- Repository gốc chưa có OrderStatusHistoryRepository, ShipperRepository, DeliveryAssignmentRepository, CODDetailRepository.
- Phát hiện bổ sung ngoài danh sách gốc: PaymentTransactionRepository của nhánh con có
  findByOrderOrderIdOrderByCreatedAtAscTransactionIdAsc, service chi tiết đơn cần method này;
  phải hợp nhất cùng findByTransactionRef của main ở bước 3.
- POM gốc dùng Java 21; chưa có H2 test. Test gốc contextLoads cần Spring context và cấu hình DB phù hợp.

## Trình tự triển khai và tiêu chí hoàn tất

### Bước 1 — Enum chung

- [x] Thêm DELIVERED giữa DELIVERING và SUCCESS; phân biệt giao thành công với đơn hoàn tất.
- [x] Giữ PaymentMethod chỉ COD, VNPAY.
- [x] Giữ AddressType chỉ SHIPPING, PICKUP, RETURN.
- [x] Khi chuyển code ở bước 3–6: đổi PaymentMethod.ONLINE, literal "ONLINE" và 'ONLINE'
  trong Java, test, query, HTML thành VNPAY; không tạo enum ONLINE ở source gốc.

Tiêu chí: ba enum đúng danh sách/thứ tự; không còn token ONLINE trong source gốc;
biên dịch kiểm tra enum và kiểm tra build Maven nếu môi trường cho phép.
Bước 3–6 đã chuyển code cần thiết và chuẩn hóa enum trong source gốc; bản nguồn nhánh con giữ đến bước 7.

### Bước 2 — Entity theo main

- [x] Xác nhận Order giữ @Entity mặc định, isCodRemitted/getIsCodRemitted().
- [x] Xác nhận CODDetail dùng codCollection/getCodCollection().
- [x] Xác nhận Role.roleName là RoleName, dùng RoleNameConverter.
- [x] Đối chiếu các getter/setter, quan hệ và kiểu dữ liệu mà service nhánh con cần;
  chỉ bổ sung phần thiếu, không thay entity main bằng bản nhánh con.

Tiêu chí: tên property dùng trong code/query khớp entity; giữ tên JPQL Order.
Order.orderStatus lưu EnumType.STRING: kiểm tra constraint/dữ liệu DB có cho phép DELIVERED trước triển khai.

Kết quả bước 2 (19/10/2026): entity gốc đã đúng chuẩn nên giữ nguyên source entity.
Đã đối chiếu hai thư mục entity và các chỗ sử dụng trong service/test nhánh con;
không phát hiện field/quan hệ cần bổ sung cho tính năng đơn hàng/giao hàng đã khảo sát.

| Entity gốc | Hợp đồng đã xác nhận | Quyết định tương thích |
| --- | --- | --- |
| Order | @Entity mặc định; bảng [Order]; Boolean isCodRemitted, cột IsCODRemitted | JPQL dùng Order/isCodRemitted; getter/setter getIsCodRemitted/setIsCodRemitted |
| CODDetail | CODCollection codCollection; CollectionId; quan hệ OrderId unique | Dùng getCodCollection/setCodCollection; giữ unique=true ở JoinColumn của main |
| Role | RoleName roleName; @Convert(RoleNameConverter.class) | Không đổi thành String; converter giữ hỗ trợ SELLER -> SHOP_OWNER và null |

DeliveryAssignment, Shipper, OrderStatusHistory và các field của OrderItem, ProductVariant,
Shop, Coupon cần cho stock/pricing/detail đã có trong main. Getter/setter của ba entity trọng tâm
được xác nhận bằng javap trên class đã biên dịch, gồm cả kiểu Boolean và RoleName.

Các khác biệt phải xử lý ở bước chuyển code, không thay entity main để chiều theo nhánh con:

- Bước 3: sửa JPQL CustomerOrder/isCODRemitted và derived method collection theo checklist repository.
- Bước 4: đổi getIsCODRemitted/setIsCODRemitted trong OrderDeliveryService, OrderPaymentRules,
  ShipperOrderQueryService, ShopOrderServiceImpl thành getIsCodRemitted/setIsCodRemitted;
  cập nhật test fixture/assert tương ứng. DTO isCODRemitted có thể giữ để bảo toàn hợp đồng giao diện.
- Giữ main không có mặc định BigDecimal.ZERO cho Order.platformFee/shopNetReceived,
  OrderItem.costSnapshot và ProductVariant.costPrice; không tự gán Shop.commissionRate = 5.00.
  Khi chuyển test repository phải đặt rõ field NOT NULL (đặc biệt platformFee/shopNetReceived/costSnapshot/costPrice),
  không dựa vào giá trị mặc định của entity nhánh con. Giữ Boolean isCodRemitted có thể null cho dữ liệu cũ.
- Không copy constraint AddressType dạng Shipping/Pickup/Return của nhánh con: main dùng SHIPPING/PICKUP/RETURN.
- Chưa xác minh constraint/dữ liệu trên DB đang triển khai; không có schema SQL/migration trong các file đã liệt kê.
  Kiểm tra DB cho DELIVERED vẫn là việc trước triển khai, không phải kết quả đã đạt ở bước 2.

### Bước 3 — Repository

- [x] Chuyển OrderStatusHistoryRepository, ShipperRepository, DeliveryAssignmentRepository, CODDetailRepository.
- [x] Sửa JPQL a.order.isCODRemitted thành a.order.isCodRemitted.
- [x] CODDetail: findByCodCollection_CollectionIdOrderByDetailIdAsc(Integer collectionId).
- [x] OrderRepository: thêm findLockedById với PESSIMISTIC_WRITE và query theo orderId;
  findByOrderIdAndShopOwnerUserId; findDetailForShopOwner với EntityGraph customer/shop/coupon;
  searchOrdersForShopOwner(ownerId, keyword, status, pageable), đối chiếu chữ ký từ nhánh con.
- [x] Đổi JPQL CustomerOrder thành Order, giữ các method main đang dùng.
- [x] ProductVariantRepository: thêm findForStockUpdate(variantId), PESSIMISTIC_WRITE và query theo variantId.
- [x] OrderItemRepository: thêm findByOrderOrderIdOrderByOrderItemIdAsc.
- [x] PaymentTransactionRepository: hợp nhất method lịch sử thanh toán đã nêu ở khảo sát.

Tiêu chí: compile; kiểm tra khởi tạo repository và chạy test query/derived method bằng DB test phù hợp.
Nếu cần H2 cho kiểm tra bước này thì bổ sung dependency scope test sớm và ghi lại, không đợi bước 7.

### Bước 4 — DTO và service

- [x] Chuyển DTO: ShopOrderListItemResponse, ShopOrderDetailResponse, ShipperOrderResponse,
  ShipperCODSummaryResponse, DelivererOrderDetailResponse (có thể đổi tên đồng bộ Shipper).
- [x] Chuyển ShopOrderService và ShopOrderServiceImpl; OrderStockService, OrderPricing, OrderPaymentRules;
  DeliveryAssignmentService, DelivererOrderDetailService, OrderDeliveryService, ShipperOrderQueryService,
  SessionUserService vào package service/impl phù hợp với thiết kế hiện có.
- [x] SessionUserService.CURRENT_USER_ID = "LOGGED_IN_USER_ID"; đọc Integer từ session.
- [x] Chuẩn hóa ONLINE thành VNPAY; sửa getter entity và method repository theo bước 2–3.
- [x] Giữ nguyên logic nghiệp vụ note 2.1 và mang theo TODO khi chuyển ShopOrderServiceImpl.
- [x] Chuyển các test service liên quan, cập nhật fixture theo entity/enum main.

Tiêu chí: compile và test pricing, payment rules, stock, chuyển trạng thái, phân công giao hàng,
chi tiết đơn. Phân biệt DELIVERED/SUCCESS; không vô tình thu COD cho đơn VNPAY.

### Bước 5 — Auth và controller

- [x] ShopOrderController: bỏ TEST_OWNER_ID, ownerId từ RequestParam trên cả GET/POST;
  lấy Integer LOGGED_IN_USER_ID, thiếu session thì redirect:/auth/login.
- [x] Tên chủ shop từ LOGGED_IN_FULLNAME, fallback "Chủ cửa hàng".
- [x] AuthController: DELIVERY_STAFF redirect:/shipper/orders.
- [x] Đổi DelivererOrderController thành ShipperOrderController; mapping /shipper/orders;
  bỏ TEST_DELIVERER_ID; dùng ID từ session; view shipper/orders và shipper/order-detail.
- [x] Giữ/hợp nhất OrderDeliveryController và ShipperOrderQueryController khi còn caller API;
  dùng cùng session key; kiểm tra không trùng mapping.
- [x] Cập nhật test controller: chưa đăng nhập, sai chủ shop/shipper, ID giả trên request,
  GET danh sách/chi tiết và các POST confirm/preparing/ready/cancel/giao hàng.

Tiêu chí: không hardcode danh tính; không lấy ownerId do client gửi làm căn cứ phân quyền;
người dùng chỉ thao tác đơn thuộc quyền của mình; API có phản hồi thiếu session phù hợp.

### Bước 6 — Templates và tài nguyên

- [x] Chuyển shop/orders.html và shop/order-detail.html; dùng layout/fragments shop gốc,
  đối chiếu chữ ký layout và trạng thái sidebar orders.
- [x] Chuyển deliverer/orders.html và order-detail.html sang templates/shipper/.
- [x] Đổi action/link /deliverer/orders/... thành /shipper/orders/...; bỏ tham số ownerId test.
- [x] Chuẩn hóa ONLINE thành VNPAY trong value, filter và điều kiện hiển thị;
  đối chiếu DTO isCODRemitted với entity isCodRemitted, không thay mù toàn bộ tên DTO.
- [x] Kiểm tra CSS/JS/ảnh, fragment, form CSRF nếu dự án có cấu hình và các URL AJAX thực tế.

Tiêu chí: render được danh sách, chi tiết, bộ lọc và form thao tác; menu/auth dẫn đúng trang;
kiểm tra giao diện shop và shipper trên desktop/mobile.

### Bước 7 — Kiểm chứng tích hợp và dọn dẹp

- [x] Đối chiếu toàn bộ file nguồn/test/tài nguyên nhánh con; ghi rõ file đã hợp nhất hoặc không cần.
- [x] Đảm bảo H2 scope test nếu bộ test cần; giữ nguyên dependency phục vụ main.
- [x] Chạy test tại root bằng mvnw.cmd test; cấu hình test độc lập DB thật.
- [x] Smoke test COD và VNPAY, session/ownership, lịch sử đơn và COD summary;
  ghi nhận giới hạn note 2.1, không tuyên bố đã sửa tồn kho VNPay.
- [x] Quét root không còn CustomerOrder, PaymentMethod.ONLINE, literal ONLINE, đường dẫn deliverer,
  hardcode ID; phân loại đúng trường hợp tên DTO/comment trước khi sửa.
- [x] Chỉ sau khi xác minh đủ code/test/tài nguyên mới xóa BetaFruit-Shopping-Platform-main/.
- [x] Kiểm tra git diff, không đưa secret, file build, upload hay cấu hình DB cá nhân vào thay đổi.

Tiêu chí: build/test root đạt, ghi lại smoke test và các tồn đọng; project không còn phụ thuộc thư mục con.

## Các note bắt buộc giữ cho team

### Note 2.1 — Quyết định tạm thời: VNPay thành công trừ kho ngay

Theo yêu cầu mới sau bước 7: khi xác thực VNPay thành công và số tiền khớp, trừ tồn kho ngay
trong cùng transaction với ghi nhận PAID, chuyển PENDING -> CONFIRMED và lưu lịch sử trạng thái.
IPN và return dùng chung VnpayPaymentCompletionService; khóa đơn trước giao dịch và biến thể.
Không thêm PAID_PENDING_CONFIRMATION hoặc thay schema.

- Callback lặp hoặc IPN/return đến đồng thời không trừ kho lần nữa.
- Nếu shop đã xác nhận và trừ kho trước callback, chỉ ghi nhận thanh toán;
  không trừ lại hoặc đưa PREPARING/READY về CONFIRMED.
- Shop xác nhận lại đơn CONFIRMED có giao dịch VNPAY/PAID là no-op, không lỗi 409;
  không bỏ kiểm tra chủ shop và không mở lại đơn ở trạng thái khác.
- Hủy đơn VNPay đã xác nhận hoàn đúng lượng tồn đã trừ; hủy lại vẫn bị từ chối.
- Chữ ký/số tiền không hợp lệ hoặc thanh toán thất bại không trừ tồn kho.
- Thiếu kho/lỗi transaction: rollback thay đổi kho, trạng thái đơn, lịch sử và thanh toán;
  IPN trả 99 để retry/đối soát, return hiển thị chưa xử lý được đơn, không báo thành công giả.
  Nếu tiền đã bị gateway thu mà chưa thể trừ kho, team cần đối soát/hoàn tiền; chưa tự triển khai hoàn tiền.
- Đơn đã hủy không bị callback muộn xác nhận lại. Dữ liệu PAID/CONFIRMED cũ trước thay đổi
  không được tự động trừ bù vì chưa biết lịch sử kho; cần đối soát riêng.

Các đoạn nhật ký bước 1–7 bên dưới mô tả trạng thái tại thời điểm hợp nhất, trước quyết định mới này.
Kết quả kiểm tra cho thay đổi mới được ghi ở cuối tài liệu.
### Note 2.5–2.6 — Tương thích entity

Dùng tên JPQL Order, không dùng CustomerOrder. Dùng Order.isCodRemitted/getIsCodRemitted()
và CODDetail.codCollection/getCodCollection(). Role giữ RoleName và converter.
Giữ tên field DTO nếu đó là hợp đồng giao diện; mapping từ entity phải đúng getter main.

## Nhật ký kiểm tra

- Bước 1: đối chiếu tự động danh sách và thứ tự cả ba enum — PASS.
- Quét token ONLINE trong src/ gốc — không có kết quả.
- Biên dịch riêng ba enum bằng JDK 21 (`javac -encoding UTF-8`, đầu ra target/enum-verification) — PASS.
- git diff --check — PASS.
- Maven compile tại root với JAVA_HOME trỏ JDK 21: `mvnw.cmd -o -DskipTests compile` — BUILD SUCCESS, 176 file nguồn.
  Lệnh Java mặc định ban đầu không phản hồi nên đã dừng và dùng JDK 21 trực tiếp.
  Đây là kiểm tra compile, chưa chạy test tích hợp/Spring context/DB.
- Thêm ngoại lệ Git riêng cho tài liệu này vì .gitignore hiện bỏ qua *.md, để lộ trình và note có thể được lưu cùng code.

### Bước 2

- Đối chiếu source entity main/nhánh con, quan hệ và nơi sử dụng service/test — hoàn tất.
- javap trên Order, CODDetail, Role, RoleNameConverter — getter/setter và kiểu dữ liệu khớp chuẩn main.
- Maven compile tại root với JDK 21 (`mvnw.cmd -o -DskipTests compile`) — BUILD SUCCESS.
- git diff --check — PASS.
- Chỉ cập nhật lộ trình trong lượt bước 2; entity gốc đã đúng nên không cần sửa code.
- Chưa chạy Spring context/test DB; việc kiểm tra query sau hợp nhất thuộc bước 3.

### Bước 3

- Đã đưa 4 repository còn thiếu vào root: OrderStatusHistoryRepository, ShipperRepository,
  DeliveryAssignmentRepository, CODDetailRepository. Giữ bản nguồn nhánh con đến bước 7.
- Hợp nhất OrderRepository, ProductVariantRepository, OrderItemRepository và PaymentTransactionRepository;
  giữ các method main, thêm query/EntityGraph và PESSIMISTIC_WRITE cho cập nhật đơn/tồn kho.
- Chuẩn hóa JPQL Order, isCodRemitted và derived method codCollection_CollectionId theo entity main.
- Thêm H2 scope test sớm theo tiêu chí bước 3; không thay datasource runtime.
- Thêm OrderRepositoryIntegrationTests dùng Hibernate + JpaRepositoryFactory trên H2 MSSQLServer mode,
  transaction rollback sau mỗi test, không tải cấu hình DB của ứng dụng.
- `JAVA_HOME` = JDK 21; `mvnw.cmd -o -Dtest=OrderRepositoryIntegrationTests test` — BUILD SUCCESS.
  Biên dịch 180 file nguồn; 3 test chạy, 0 failure, 0 error, 0 skipped.
- Đã kiểm tra: shop/shipper ownership; tìm tên/mã đơn, status/payment filter và phân trang;
  EntityGraph; lock mode của đơn và biến thể; COD false/null so với đã nộp/VNPAY/sai trạng thái;
  COD count/sum rỗng; codCollection derived method; thứ tự dòng hàng, lịch sử và thanh toán;
  query coupon cũ và findByTransactionRef vẫn hoạt động.
- Quét repository root không còn CustomerOrder, isCODRemitted, findByCollection hoặc ONLINE;
  git diff --check đạt. Chưa chạy toàn bộ Spring context/test suite hoặc SQL Server thực tế.

### Bước 4

- Đã chuyển 5 response DTO và 10 file service vào source gốc, chỉ copy các file tính năng còn thiếu;
  không ghi đè UserService/UserServiceImpl hay các DTO main.
- Giữ tên DelivererOrderDetailService/DelivererOrderDetailResponse theo lựa chọn được phép ở bước 4;
  việc đổi controller/mapping/view sang Shipper thuộc bước 5–6.
- Chuyển 10 file test service/pricing/repository của tính năng; chuẩn hóa PaymentMethod.ONLINE và literal ONLINE
  thành VNPAY, đổi getter/setter COD của entity sang getIsCodRemitted/setIsCodRemitted.
  Giữ field record DTO isCODRemitted để tương thích template/API.
- SessionUserService.CURRENT_USER_ID mang giá trị LOGGED_IN_USER_ID; chỉ chấp nhận Integer.
  Session null/thiếu/sai kiểu trả Optional.empty(), không ép Number/String sang ID.
  Thêm SessionUserServiceTests cho session đúng, key cũ, kiểu sai, session thiếu và user không tồn tại.
- Fixture ShipperOrderRepositoryTests đặt rõ platformFee/shopNetReceived theo NOT NULL của main;
  giữ nguyên entity/default/constraint của main.
- TODO note 2.1 đã có trong ShopOrderServiceImpl gốc, giữ PaymentServiceImpl và luồng VNPay/tồn kho hiện tại.
  Không xử lý trước lỗi nghiệp vụ được lưu cho team.
- Kiểm tra source service/DTO đã chuyển: không còn ONLINE, CustomerOrder hoặc getter/setter COD cũ.
  Literal CURRENT_USER_ID chỉ còn trong test phủ định để xác nhận không đọc key session cũ.
- Lần chạy đầu bị kẹt khi Mockito tự attach: JVM báo không mở được Windows pipe (error 5).
  Đã cấu hình maven-surefire-plugin nạp mockito-core bằng -javaagent từ đầu tiến trình test;
  dùng settings.localRepository và mockito.version được quản lý bởi Spring Boot, không hardcode đường dẫn máy.
- Chạy lại với JDK 21:
  `mvnw.cmd -o -Dtest=SessionUserServiceTests,*ServiceTests,OrderPaymentRulesTests,OrderPricingTests,OrderPricingRepositoryTests,ShopOrderStockTransitionTests,ShipperOrderRepositoryTests,OrderRepositoryIntegrationTests test`
  — BUILD SUCCESS; 50 test, 0 failure, 0 error, 0 skipped (12 lớp test).
- Nhóm kiểm tra phủ pricing/coupon, phân trang theo tiền shop nhận, tồn kho và transition,
  phân công, giao hàng COD/VNPAY, phân quyền đọc chi tiết, tổng COD, session; gồm test repository bước 3.
- Source root đã biên dịch 195 file; test source gồm 13 file biên dịch thành công.
  git diff --check đạt. Không chạy contextLoads/Spring context với datasource thật;
  controller/template và smoke test toàn ứng dụng tiếp tục ở bước 5–7.

### Bước 5

- Chuyển ShopOrderController và hai REST controller vào root; chuyển DelivererOrderController thành
  ShipperOrderController. Bản nguồn nhánh con giữ đến bước 7.
- Shop: GET danh sách/chi tiết, cả hai API và 4 POST chỉ dùng Integer LOGGED_IN_USER_ID từ session;
  bỏ TEST_OWNER_ID, RequestParam ownerId, preview test và ownerId trên redirect/model.
  Tên chủ shop đọc LOGGED_IN_FULLNAME, fallback "Chủ cửa hàng".
- Shipper: bỏ TEST_DELIVERER_ID và user giả; SessionUserService đọc key đăng nhập chuẩn;
  session/user thiếu thì trang/form redirect /auth/login. View trả shipper/orders và shipper/order-detail.
- AuthController: DELIVERY_STAFF đăng nhập chuyển tới /shipper/orders.
- Hai REST controller dùng /shipper/orders/api để không trùng mapping trang/form.
  Không tìm thấy caller AJAX đang dùng các endpoint cũ trong source/tài nguyên đã khảo sát;
  client ngoài repository nếu có phải chuyển URL theo bảng bên dưới.

| Chức năng | Endpoint gốc sau hợp nhất |
| --- | --- |
| Trang shop | GET /shop/orders; GET /shop/orders/{orderId} (giữ alias /shop/orders.html) |
| Thao tác shop | POST /shop/orders/{orderId}/confirm, /preparing, /ready, /cancel |
| JSON shop | GET /shop/orders/api; GET /shop/orders/api/{orderId} |
| Trang shipper | GET /shipper/orders; GET /shipper/orders/{orderId} |
| Thao tác form shipper | POST /shipper/orders/{orderId}/picked-up, /delivering, /delivered |
| JSON shipper | GET /shipper/orders/api; GET /shipper/orders/api/{orderId}; GET /shipper/orders/api/cod-summary |
| Thao tác API shipper | POST /shipper/orders/api/{orderId}/picked-up, /delivering, /delivered |

- ShopOwnerInterceptor và ShipperInterceptor giữ kiểm tra role theo enum của session;
  từ chối ID sai kiểu, không tạo session khi kiểm tra. API thiếu đăng nhập trả 401, sai role trả 403;
  trang/form vẫn redirect login hoặc /403. WebConfig hiện có đã phủ /shop/** và /shipper/**.
- GlobalExceptionHandler thêm handler ResponseStatusException để giữ status và ProblemDetail;
  tránh biến 401/403/404/409 từ service/controller thành redirect chung.
- OrderControllersTests đăng ký cả bốn controller cùng interceptor thật và GlobalExceptionHandler;
  SessionUserService thật dùng UserRepository mock. Kiểm tra mapping, role/session/ID giả,
  GET/POST shop và shipper, VNPAY filter, COD confirmation, flash conflict và lỗi phân quyền.
  AuthRoutingTests kiểm tra redirect sau login và session được lưu.
- Chỉ kiểm tra tên view/model bằng view giả trong bước này. Chưa render Thymeleaf;
  chuyển template/tài nguyên và cập nhật các test render trang nhánh con ở bước 6.
- Kết quả cuối với JDK 21: Maven BUILD SUCCESS; 64 test, 0 failure, 0 error, 0 skipped.
  Chạy nhóm test bước 4 cộng OrderControllersTests (13 test) và AuthRoutingTests (1 test).
  Biên dịch 199 file nguồn, 15 file test. Log cục bộ: target/step5-test.log.
- Fixture danh sách API dùng PageImpl có Pageable như repository thật, kiểm tra JSON content;
  MockMvc standalone không nạp các module JSON tự cấu hình của Spring Boot.
- git diff --check đạt; quét controller root không còn ID test, /deliverer/orders,
  tên chủ shop test, RequestParam ownerId hay redirect shipper/dashboard.
- Chưa chạy Spring context đầy đủ/SQL Server thật. Các view mới sẽ render được sau khi chuyển template ở bước 6.

### Bước 6

- Chuyển shop/orders.html, shop/order-detail.html và hai template deliverer sang shipper/orders.html,
  shipper/order-detail.html. Shop dùng shop/layout của main với activeMenu='orders', sidebar/topbar chính.
- CSS shop được giới hạn trong .shop-orders-page để không ghi đè style menu/layout của main.
  Giữ script hộp xác nhận và dialog trong fragment nội dung, không bị mất khi thay layout.
- Bỏ toàn bộ hidden input và URL query ownerId; các form submit tới controller dùng session ở bước 5.
  Link/action/form/pagination shipper đều là /shipper/orders, không còn /deliverer/orders.
- Chuyển sidebar shipper cần thiết vào shipper/fragments/sidebar.html, không copy đè fragments chung.
  Link COD trước đây trỏ trang chưa tồn tại được đổi thành bộ lọc "Đơn COD đã giao"
  (/shipper/orders?paymentMethod=COD&status=DELIVERED), không tạo màn hình đối soát mới.
- Filter/điều kiện thanh toán dùng VNPAY; giữ field DTO isCODRemitted theo hợp đồng giao diện.
  Asset logo/CSS shop dùng bản main; sidebar shipper dùng SVG/CSS có sẵn, không cần copy ảnh/CSS trùng.
  Form Thymeleaf dùng th:action. POM hiện không có Spring Security/CSRF filter để cấu hình thêm.
- Layout shop thêm class riêng cho trang orders; mobile menu cuộn ngang, nội dung cuộn dọc,
  bảng dài cuộn trong card. CSS responsive chỉ áp dụng các trang orders, không đổi giao diện trang shop khác.
- Chuyển/cập nhật 3 lớp test render từ nhánh con thành ShopOrderDetailPageTests,
  ShipperOrderPageTests, ShipperOrderDetailPageTests; dùng OrderPageTestSupport với Thymeleaf thật,
  session/role và interceptor main, dữ liệu service mock, không tải datasource thật.
- Bộ test tổng hợp bước 3–6: Maven BUILD SUCCESS, 83 test, 0 failure/error/skipped.
  Sau chỉnh responsive cuối, chạy lại 19 test render: BUILD SUCCESS, không có lỗi.
  Log: target/step6-test.log và target/step6-render-test.log.
- Đã xem cả 4 trang bằng trình duyệt ở desktop và mobile 390x844, kiểm tra hộp xác nhận shop;
  đo lại trang shop mobile: viewport/content/body đều 375px sau trừ scrollbar, không tràn ngang toàn trang.
  Bảng/menu có vùng cuộn riêng. Preview dùng HTML render từ fixture và asset main qua server localhost chỉ đọc;
  không kết nối hoặc thao tác DB thật. Ảnh: target/ui-preview/shop-orders-desktop.png.
- Quét 4 template và fragment shipper không còn ownerId, ONLINE hay fragment/route deliverer cũ;
  git diff --check đạt. Chưa làm bước 7: chưa xóa nhánh con, chưa smoke test SQL Server thật.

### Bước 7

- Kiểm kê 329 file trước dọn; 121 file ngoài target có quyết định giữ/hợp nhất/thay thế cụ thể
  trong [REFACTOR_FILE_AUDIT.md](REFACTOR_FILE_AUDIT.md). Không chép đè chức năng user/home/entity/config main
  bằng scaffold của nhánh con; không phát hiện dependency runtime còn trỏ thư mục con.
- Giữ 3 ghi chú gốc tại docs/legacy-feature-notes/, có banner chỉ rõ đây là bản lịch sử;
  tài liệu này là hợp đồng hiện tại. Các đề xuất COD/settlement/scheduler chưa có vẫn là ghi chú, không tự triển khai.
- Trước xóa đã lưu toàn thư mục tại target/legacy-feature-backup.zip và kiểm SHA-256 khớp đủ 121 file ngoài build.
  ZIP có cấu hình local cũ nên chỉ giữ cục bộ trong target bị Git ignore; không commit/chia sẻ ZIP.
  Đây là bản sao tạm, có thể mất nếu chạy Maven clean. Các source đã theo dõi còn có thể tra bằng lịch sử Git.
- Đã xác minh đường dẫn tuyệt đối đúng D:\ChucMungThangLon\BetaFruit-Shopping-Platform-main,
  nằm ngay dưới workspace, không có reparse point, rồi xóa thư mục này.
- H2 đã được thêm scope test từ bước 3. Bổ sung application-test.properties và chỉ định rõ spring.config.location
  cho Swp391BetaFruitApplicationTests, để không nạp application.properties/local datasource runtime.
- Khởi động Spring Boot đầy đủ với 25 repository, controller, interceptor, service và Thymeleaf thật trên H2;
  transaction test rollback. Luồng smoke gồm shop confirm/preparing/ready, phân công, shipper picked-up/delivering/delivered,
  render chi tiết, COD summary, session ownerId giả và kiểm tra không cộng số dư/SettledAt sớm.
- Smoke VNPay dùng callback IPN ký bằng khóa test và service thật, không gọi gateway bên ngoài:
  giao dịch PAID, đơn CONFIRMED, callback lặp trả 02, shop duyệt lại trả 409;
  tồn kho chưa bị trừ theo đúng note 2.1. Chỉ đơn COD deduct trong smoke này.
  Không hủy đơn VNPay để che lấp vấn đề hoàn kho đã ghi cho team; chưa sửa logic VNPay/tồn kho.
- Lượt test đầu gặp class build cũ không hợp lệ (ClassFormatError ở Shop). Đã xóa riêng target/classes,
  target/test-classes, target/maven-status sau kiểm tra đường dẫn rồi biên dịch lại;
  giữ nguyên preview/log/ZIP cục bộ, không đổi source entity.
- Trước xóa thư mục con: `mvnw.cmd -o test` với JDK 21 — BUILD SUCCESS, 85 test.
- Lượt test cuối sau xóa thư mục con và bổ sung callback IPN: `mvnw.cmd -o test` với JDK 21
  — BUILD SUCCESS, 85 test, 0 failure, 0 error, 0 skipped. Log: target/step7-final-test.log.
- Quét src/pom/.mvn không còn CustomerOrder, PaymentMethod.ONLINE/literal ONLINE,
  /deliverer/orders, ID test hay tham chiếu thư mục con. isCODRemitted trong DTO/template được giữ theo hợp đồng;
  entity/query dùng isCodRemitted. git diff --check đạt.
- Giới hạn: chưa kết nối SQL Server thật, chưa xác nhận constraint/dữ liệu cũ chấp nhận DELIVERED,
  chưa gọi VNPay thật. Các kiểm tra DB/gateway triển khai này và note 2.1 vẫn cần team xử lý trước phát hành.

### Cập nhật sau bước 7 — Trừ kho ngay khi VNPay thành công (19/10/2026)

- Đã thực hiện quyết định mới ở note 2.1. PaymentServiceImpl IPN/return xác thực chữ ký,
  gọi chung VnpayPaymentCompletionService. Service này khóa Order trước PaymentTransaction và tồn kho,
  kiểm số tiền chính xác, deduct, lưu lịch sử, CONFIRMED/PAID trong một transaction.
- Bỏ transaction bọc ngoài IPN/return (kể cả annotation IPN trên interface PaymentService),
  để khi completion thất bại, transaction đã rollback trước khi wrapper trả mã lỗi/thông báo cho khách.
- Callback lặp hoặc đồng thời không deduct lần nữa. Đơn đã được shop confirm/deduct trước callback
  không bị trừ lại hoặc đưa lùi trạng thái. Shop confirm lại đơn VNPay CONFIRMED có giao dịch PAID
  trả thành công mà không tạo thêm lịch sử/trừ kho; các chặn ownership/trạng thái khác vẫn giữ.
- Cập nhật smoke cũ từ việc xác nhận lỗi 409/thiếu deduct sang kiểm tra trừ kho và confirm không lỗi.
- Thêm 10 integration test chạy với transaction thật (không nằm trong transaction test bọc ngoài):
  IPN trước/return trước, callback lặp, sai chữ ký/số tiền, gateway thất bại, thiếu kho và retry sau bổ sung kho,
  hủy/hoàn đúng một lần, shop chuẩn bị trước callback, callback muộn sau hủy, IPN/return đồng thời,
  hai reference cho cùng đơn. Không gọi gateway thật hay sửa DB runtime.
- `mvnw.cmd -o test` với JDK 21 — BUILD SUCCESS; 95 test, 0 failure, 0 error, 0 skipped.
  Log: target/vnpay-stock-test.log. git diff --check đạt.
- Dữ liệu PAID/CONFIRMED trước bản sửa không tự backfill kho. Hoàn tiền và đối soát khi gateway đã thu tiền
  nhưng thiếu kho/lỗi xử lý vẫn cần xử lý riêng; không thêm schema hay trạng thái PAID_PENDING_CONFIRMATION.

### Khôi phục cấu hình database local (19/10/2026)

- Đã khôi phục nguyên byte src/main/resources/application-local.properties từ ZIP trước dọn thư mục con;
  SHA-256 khớp bản lưu. Không đưa giá trị tài khoản/mật khẩu vào tài liệu hoặc chat.
- application.properties nạp optional:classpath:application-local.properties để cấu hình máy cá nhân được dùng tại root.
- .gitignore bỏ qua application-local.properties; đã kiểm tra file không được Git theo dõi.
- File local hiện có độc lập với ZIP trong target; Maven clean không xóa bản src/main/resources này.
  Chưa thử kết nối SQL Server bằng thông tin đã khôi phục.

### Sửa đường dẫn project IntelliJ sau hợp nhất (19/10/2026)

- Log chạy báo ClassNotFoundException cho Swp391BetaFruitApplication trước khi Spring khởi động;
  classpath trong lệnh Java chỉ có dependency, thiếu thư mục class ứng dụng ở root.
- .idea/misc.xml còn đăng ký Maven bằng POM trong thư mục nhánh con đã xóa.
  Đã sửa về $PROJECT_DIR$/pom.xml; .idea/encodings.xml chuyển source/resource UTF-8 về root.
- Main class vẫn tồn tại ở source và target/classes; javap đọc được main(String[]).
  `mvnw.cmd -o -DskipTests compile` với JDK 21 — BUILD SUCCESS.
- IntelliJ đang mở cần reload/import Maven từ pom.xml root để cập nhật module/classpath trong bộ nhớ,
  sau đó chạy lại Swp391BetaFruitApplication. Hai file .idea nói trên đã được repository theo dõi từ trước.
- Đây là lỗi module/classpath trước kết nối DB; không thay đổi tài khoản/mật khẩu database.

### Sửa lỗi tải Shop Orders với database local (19/10/2026)

- Log thực tế báo `No enum constant ...PaymentMethod.ONLINE` khi tải đơn của ownerId=1.
  Query ownership đúng; Hibernate không hydrate được giá trị legacy ONLINE nên controller trả trang kèm lỗi tải dữ liệu.
- Database local có 10 đơn COD, 6 đơn ONLINE; CHECK `CHK_Order_Payment` chỉ cho phép Online/COD.
  `ddl-auto=none` không tự đồng bộ dữ liệu/ràng buộc này theo enum COD/VNPAY.
- Đã chạy `docs/sql/normalize-order-payment-method.sql` trên database local trong transaction:
  đổi 6 giá trị ONLINE thành VNPAY, tạo lại CHECK cho đúng COD/VNPAY (phân biệt hoa/thường).
  Script có thể chạy lại, chặn giá trị lạ trước khi thay đổi và rollback khi lỗi.
- Giá trị gốc theo OrderId và định nghĩa CHECK cũ được lưu tại
  `target/db-repair/order-payment-original-1792391622924.sql` trước khi thay đổi.
  Đây là bản lưu tạm, Maven clean sẽ xóa; khi phục hồi phải điều chỉnh CHECK tương ứng trước khi trả giá trị cũ.
- Đã kiểm tra bằng chính `ShopOrderService.getOrdersForShopOwner` kết nối SQL Server local:
  ownerId=1 đọc đủ 16 đơn qua 2 trang, trang đầu 10 đơn, không còn lỗi hydrate/mapping/pricing.
  Log kiểm tra: `target/db-repair/shop-orders-verification.log`.
- Việc chuẩn hóa phương thức không đánh dấu đã thanh toán, đổi trạng thái đơn hay trừ tồn kho.
  Không cần khởi động lại ứng dụng; tải lại trang Shop Orders để đọc dữ liệu mới.
