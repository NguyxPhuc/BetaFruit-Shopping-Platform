# Đối chiếu trước khi dọn thư mục nhánh tính năng

Ngày: 19/10/2026. Mỗi đường dẫn dưới đây tương đối với thư mục nhánh con trước khi xóa.
Đối chiếu dựa trên file thực tế, hash nội dung và các thay đổi đã kiểm tra ở bước 1–6.
Giữ source main khi bản nhánh con chỉ là scaffold hoặc khác thiết kế chính; không chép đè entity/config/auth main.

Tổng 329 file, trong đó 121 file ngoài target được liệt kê bên dưới. Phần target là build/log/report có thể tạo lại.
Bản sao nguyên thư mục, gồm cấu hình local không đưa vào Git: target/legacy-feature-backup.zip (file build cục bộ, có thể mất khi chạy clean).

| File nguồn | Quyết định / nơi giữ |
| --- | --- |
| .env.example | Giữ file/cấu hình/tài nguyên main tại cùng đường dẫn; không thay bằng bản nhánh con. |
| .gitattributes | Đã có bản giống nội dung tại root: .gitattributes |
| .gitignore | Giữ file/cấu hình/tài nguyên main tại cùng đường dẫn; không thay bằng bản nhánh con. |
| .mvn/wrapper/maven-wrapper.properties | Đã có bản giống nội dung tại root: .mvn/wrapper/maven-wrapper.properties |
| docs/delivered-settlement.md | Giữ lịch sử tại docs/legacy-feature-notes/delivered-settlement.md |
| docs/shipper-payment-data.md | Giữ lịch sử tại docs/legacy-feature-notes/shipper-payment-data.md |
| docs/shop-order-detail.md | Giữ lịch sử tại docs/legacy-feature-notes/shop-order-detail.md |
| mvnw | Đã có bản giống nội dung tại root: mvnw |
| mvnw.cmd | Đã có bản giống nội dung tại root: mvnw.cmd |
| pom.xml | Giữ file/cấu hình/tài nguyên main tại cùng đường dẫn; không thay bằng bản nhánh con. |
| src/main/java/com/fu/SWP391_BetaFruit/controller/DelivererOrderController.java | Đã chuyển và sửa thành controller/ShipperOrderController.java; session thật, route shipper. |
| src/main/java/com/fu/SWP391_BetaFruit/controller/OrderDeliveryController.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/main/java/com/fu/SWP391_BetaFruit/controller/ShipperOrderQueryController.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/main/java/com/fu/SWP391_BetaFruit/controller/ShopOrderController.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/main/java/com/fu/SWP391_BetaFruit/controller/UserController.java | Nhánh con là scaffold rỗng/trang hello; giữ chức năng user và home đang có của main. |
| src/main/java/com/fu/SWP391_BetaFruit/dto/request/UserRequest.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/dto/request/UserRequest.java |
| src/main/java/com/fu/SWP391_BetaFruit/dto/response/DelivererOrderDetailResponse.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/dto/response/DelivererOrderDetailResponse.java |
| src/main/java/com/fu/SWP391_BetaFruit/dto/response/ShipperCODSummaryResponse.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/dto/response/ShipperCODSummaryResponse.java |
| src/main/java/com/fu/SWP391_BetaFruit/dto/response/ShipperOrderResponse.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/dto/response/ShipperOrderResponse.java |
| src/main/java/com/fu/SWP391_BetaFruit/dto/response/ShopOrderDetailResponse.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/dto/response/ShopOrderDetailResponse.java |
| src/main/java/com/fu/SWP391_BetaFruit/dto/response/ShopOrderListItemResponse.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/dto/response/ShopOrderListItemResponse.java |
| src/main/java/com/fu/SWP391_BetaFruit/dto/response/UserResponse.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/dto/response/UserResponse.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/Address.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/Cart.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/entity/Cart.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/CartItem.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/entity/CartItem.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/CODCollection.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/CODDetail.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/Coupon.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/entity/Coupon.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/CustomerMembership.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/entity/CustomerMembership.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/DeliveryAssignment.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/entity/DeliveryAssignment.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/ImportNote.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/ImportNoteDetail.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/InventoryTransaction.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/entity/InventoryTransaction.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/MasterCategory.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/MembershipTier.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/entity/MembershipTier.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/Notification.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/entity/Notification.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/Order.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/OrderItem.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/OrderStatusHistory.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/entity/OrderStatusHistory.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/PaymentTransaction.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/PayoutRequest.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/entity/PayoutRequest.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/Product.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/ProductImage.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/entity/ProductImage.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/ProductVariant.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/Review.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/ReviewMedia.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/entity/ReviewMedia.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/Role.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/Shipper.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/entity/Shipper.java |
| src/main/java/com/fu/SWP391_BetaFruit/entity/Shop.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/entity/User.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/enums/AddressType.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/enums/CouponCategory.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/enums/CouponCategory.java |
| src/main/java/com/fu/SWP391_BetaFruit/enums/DiscountType.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/enums/DiscountType.java |
| src/main/java/com/fu/SWP391_BetaFruit/enums/InventoryTransactionType.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/enums/InventoryTransactionType.java |
| src/main/java/com/fu/SWP391_BetaFruit/enums/MediaType.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/enums/MediaType.java |
| src/main/java/com/fu/SWP391_BetaFruit/enums/NotificationType.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/enums/NotificationType.java |
| src/main/java/com/fu/SWP391_BetaFruit/enums/OrderStatus.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/enums/PaymentMethod.java | Giữ chuẩn entity/enum main; các khác biệt và mapping được xử lý ở bước 1–4. |
| src/main/java/com/fu/SWP391_BetaFruit/enums/PaymentStatus.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/enums/PaymentStatus.java |
| src/main/java/com/fu/SWP391_BetaFruit/enums/PayoutStatus.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/enums/PayoutStatus.java |
| src/main/java/com/fu/SWP391_BetaFruit/enums/ProductApprovalStatus.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/enums/ProductApprovalStatus.java |
| src/main/java/com/fu/SWP391_BetaFruit/enums/ProductVisibility.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/enums/ProductVisibility.java |
| src/main/java/com/fu/SWP391_BetaFruit/enums/ReviewVisibility.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/enums/ReviewVisibility.java |
| src/main/java/com/fu/SWP391_BetaFruit/enums/ShopApprovalStatus.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/enums/ShopApprovalStatus.java |
| src/main/java/com/fu/SWP391_BetaFruit/enums/UserStatus.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/enums/UserStatus.java |
| src/main/java/com/fu/SWP391_BetaFruit/repository/CODDetailRepository.java | Giữ/hợp nhất repository main; query tính năng đơn hàng được kiểm tra bước 3. Review/ImportNote dùng nghiệp vụ main, method nhánh con không có caller trong tính năng chuyển. |
| src/main/java/com/fu/SWP391_BetaFruit/repository/DeliveryAssignmentRepository.java | Giữ/hợp nhất repository main; query tính năng đơn hàng được kiểm tra bước 3. Review/ImportNote dùng nghiệp vụ main, method nhánh con không có caller trong tính năng chuyển. |
| src/main/java/com/fu/SWP391_BetaFruit/repository/ImportNoteDetailRepository.java | Giữ/hợp nhất repository main; query tính năng đơn hàng được kiểm tra bước 3. Review/ImportNote dùng nghiệp vụ main, method nhánh con không có caller trong tính năng chuyển. |
| src/main/java/com/fu/SWP391_BetaFruit/repository/ImportNoteRepository.java | Giữ/hợp nhất repository main; query tính năng đơn hàng được kiểm tra bước 3. Review/ImportNote dùng nghiệp vụ main, method nhánh con không có caller trong tính năng chuyển. |
| src/main/java/com/fu/SWP391_BetaFruit/repository/InventoryTransactionRepository.java | Giữ/hợp nhất repository main; query tính năng đơn hàng được kiểm tra bước 3. Review/ImportNote dùng nghiệp vụ main, method nhánh con không có caller trong tính năng chuyển. |
| src/main/java/com/fu/SWP391_BetaFruit/repository/OrderItemRepository.java | Giữ/hợp nhất repository main; query tính năng đơn hàng được kiểm tra bước 3. Review/ImportNote dùng nghiệp vụ main, method nhánh con không có caller trong tính năng chuyển. |
| src/main/java/com/fu/SWP391_BetaFruit/repository/OrderRepository.java | Giữ/hợp nhất repository main; query tính năng đơn hàng được kiểm tra bước 3. Review/ImportNote dùng nghiệp vụ main, method nhánh con không có caller trong tính năng chuyển. |
| src/main/java/com/fu/SWP391_BetaFruit/repository/OrderStatusHistoryRepository.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/repository/OrderStatusHistoryRepository.java |
| src/main/java/com/fu/SWP391_BetaFruit/repository/PaymentTransactionRepository.java | Giữ/hợp nhất repository main; query tính năng đơn hàng được kiểm tra bước 3. Review/ImportNote dùng nghiệp vụ main, method nhánh con không có caller trong tính năng chuyển. |
| src/main/java/com/fu/SWP391_BetaFruit/repository/ProductVariantRepository.java | Giữ/hợp nhất repository main; query tính năng đơn hàng được kiểm tra bước 3. Review/ImportNote dùng nghiệp vụ main, method nhánh con không có caller trong tính năng chuyển. |
| src/main/java/com/fu/SWP391_BetaFruit/repository/ReviewRepository.java | Giữ/hợp nhất repository main; query tính năng đơn hàng được kiểm tra bước 3. Review/ImportNote dùng nghiệp vụ main, method nhánh con không có caller trong tính năng chuyển. |
| src/main/java/com/fu/SWP391_BetaFruit/repository/ShipperRepository.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/repository/ShipperRepository.java |
| src/main/java/com/fu/SWP391_BetaFruit/repository/UserRepository.java | Giữ/hợp nhất repository main; query tính năng đơn hàng được kiểm tra bước 3. Review/ImportNote dùng nghiệp vụ main, method nhánh con không có caller trong tính năng chuyển. |
| src/main/java/com/fu/SWP391_BetaFruit/service/impl/DelivererOrderDetailService.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/service/impl/DelivererOrderDetailService.java |
| src/main/java/com/fu/SWP391_BetaFruit/service/impl/DeliveryAssignmentService.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/service/impl/DeliveryAssignmentService.java |
| src/main/java/com/fu/SWP391_BetaFruit/service/impl/OrderDeliveryService.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/main/java/com/fu/SWP391_BetaFruit/service/impl/OrderPaymentRules.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/main/java/com/fu/SWP391_BetaFruit/service/impl/OrderPricing.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/service/impl/OrderPricing.java |
| src/main/java/com/fu/SWP391_BetaFruit/service/impl/OrderStockService.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/service/impl/OrderStockService.java |
| src/main/java/com/fu/SWP391_BetaFruit/service/impl/ShipperOrderQueryService.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/main/java/com/fu/SWP391_BetaFruit/service/impl/ShopOrderServiceImpl.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/main/java/com/fu/SWP391_BetaFruit/service/impl/UserServiceImpl.java | Nhánh con là scaffold rỗng/trang hello; giữ chức năng user và home đang có của main. |
| src/main/java/com/fu/SWP391_BetaFruit/service/SessionUserService.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/main/java/com/fu/SWP391_BetaFruit/service/ShopOrderService.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/service/ShopOrderService.java |
| src/main/java/com/fu/SWP391_BetaFruit/service/UserService.java | Nhánh con là scaffold rỗng/trang hello; giữ chức năng user và home đang có của main. |
| src/main/java/com/fu/SWP391_BetaFruit/Swp391BetaFruitApplication.java | Đã có bản giống nội dung tại root: src/main/java/com/fu/SWP391_BetaFruit/Swp391BetaFruitApplication.java |
| src/main/resources/application-local.properties | Cấu hình máy cũ: chỉ giữ trong ZIP cục bộ bị Git ignore, không đưa vào runtime root. |
| src/main/resources/application.properties | Giữ file/cấu hình/tài nguyên main tại cùng đường dẫn; không thay bằng bản nhánh con. |
| src/main/resources/static/css/site.css | Giữ file/cấu hình/tài nguyên main tại cùng đường dẫn; không thay bằng bản nhánh con. |
| src/main/resources/static/images/betafruit-logo.png | Đã có bản giống nội dung tại root: src/main/resources/static/images/betafruit-logo.png |
| src/main/resources/templates/deliverer/order-detail.html | Đã chuyển sang templates/shipper/order-detail.html; route/filter/form mới. |
| src/main/resources/templates/deliverer/orders.html | Đã chuyển sang templates/shipper/orders.html; route/filter/form mới. |
| src/main/resources/templates/error/404.html | Đã có bản giống nội dung tại root: src/main/resources/templates/error/404.html |
| src/main/resources/templates/fragments/DelivererSidebar.html | Đã chuyển sang templates/shipper/fragments/sidebar.html. |
| src/main/resources/templates/fragments/footer.html | Đã có bản giống nội dung tại root: src/main/resources/templates/fragments/footer.html |
| src/main/resources/templates/fragments/header.html | Giữ file/cấu hình/tài nguyên main tại cùng đường dẫn; không thay bằng bản nhánh con. |
| src/main/resources/templates/fragments/ShopSidebar.html | Không dùng sidebar nhánh con; dùng templates/shop/layout và shop/fragments/sidebar của main. |
| src/main/resources/templates/index.html | Trang hello scaffold; giữ PageController + public/index.html của main. |
| src/main/resources/templates/shop/order-detail.html | Đã chuyển vào root, dùng layout main, bỏ ownerId và cập nhật form. |
| src/main/resources/templates/shop/orders.html | Đã chuyển vào root, dùng layout main, bỏ ownerId và cập nhật form. |
| src/test/java/com/fu/SWP391_BetaFruit/controller/DelivererOrderDetailPageTests.java | Đã đổi thành controller/ShipperOrderDetailPageTests.java; render Thymeleaf và session thật. |
| src/test/java/com/fu/SWP391_BetaFruit/controller/DelivererOrderPageTests.java | Đã đổi thành controller/ShipperOrderPageTests.java; render Thymeleaf và session thật. |
| src/test/java/com/fu/SWP391_BetaFruit/controller/OrderDeliveryControllerTests.java | Đã hợp nhất coverage vào controller/OrderControllersTests.java với interceptor/session thật và API mới. |
| src/test/java/com/fu/SWP391_BetaFruit/controller/ShipperOrderQueryControllerTests.java | Đã hợp nhất coverage vào controller/OrderControllersTests.java với interceptor/session thật và API mới. |
| src/test/java/com/fu/SWP391_BetaFruit/controller/ShopOrderDetailPageTests.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/test/java/com/fu/SWP391_BetaFruit/service/impl/DelivererOrderDetailServiceTests.java | Đã có bản giống nội dung tại root: src/test/java/com/fu/SWP391_BetaFruit/service/impl/DelivererOrderDetailServiceTests.java |
| src/test/java/com/fu/SWP391_BetaFruit/service/impl/DeliveryAssignmentServiceTests.java | Đã có bản giống nội dung tại root: src/test/java/com/fu/SWP391_BetaFruit/service/impl/DeliveryAssignmentServiceTests.java |
| src/test/java/com/fu/SWP391_BetaFruit/service/impl/OrderDeliveryServiceTests.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/test/java/com/fu/SWP391_BetaFruit/service/impl/OrderPaymentRulesTests.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/test/java/com/fu/SWP391_BetaFruit/service/impl/OrderPricingRepositoryTests.java | Đã có bản giống nội dung tại root: src/test/java/com/fu/SWP391_BetaFruit/service/impl/OrderPricingRepositoryTests.java |
| src/test/java/com/fu/SWP391_BetaFruit/service/impl/OrderPricingTests.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/test/java/com/fu/SWP391_BetaFruit/service/impl/OrderStockServiceTests.java | Đã có bản giống nội dung tại root: src/test/java/com/fu/SWP391_BetaFruit/service/impl/OrderStockServiceTests.java |
| src/test/java/com/fu/SWP391_BetaFruit/service/impl/ShipperOrderRepositoryTests.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/test/java/com/fu/SWP391_BetaFruit/service/impl/ShopOrderDetailServiceTests.java | Đã hợp nhất vào cùng đường dẫn root; session/enum/getter/mapping/test theo bước 4–6. |
| src/test/java/com/fu/SWP391_BetaFruit/service/impl/ShopOrderStockTransitionTests.java | Đã có bản giống nội dung tại root: src/test/java/com/fu/SWP391_BetaFruit/service/impl/ShopOrderStockTransitionTests.java |
| src/test/java/com/fu/SWP391_BetaFruit/Swp391BetaFruitApplicationTests.java | Đã chuyển vào root và cập nhật fixture; test Spring context dùng profile H2 riêng ở bước 7. |
