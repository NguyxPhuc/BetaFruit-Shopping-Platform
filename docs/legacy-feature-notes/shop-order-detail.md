> Lưu trữ ghi chú nhánh tính năng trước hợp nhất. Các endpoint/session/enum/layout trong tài liệu này có thể đã cũ; xem [lộ trình hiện tại](../REFACTOR_ROADMAP.md) để biết trạng thái và hợp đồng sau hợp nhất.

# Shop owner order detail

Backend endpoint: `GET /shop/orders/api/{orderId}?ownerId={ownerId}`.
The owner parameter follows the existing temporary list-page convention. Replace it
with the authenticated session owner when login is connected. A missing order or
an order outside the supplied owner's shop returns 404 before loading related data.

## Data contract

- `summary`: existing list-row fields including order ID/code, customer name,
  final amount, shopNetReceived, status/label, action flags and creation time.
- `shopId`, `shopName`, `customerId`, `customerPhone`: current Shop/User data.
  The customer phone is the current account phone, not a recipient-phone snapshot.
- `snapshotShippingAddress`: address stored in Order at checkout.
- `totalAmount`, `discountAmount`, `shippingFee`, `paymentMethod`,
  `isSettled`, `settledAt`: stored Order values; no recalculation of settlement.
- `couponCode`: current linked Coupon code, or null.
- `items`: OrderItem product/variant name and price snapshots, quantity,
  and lineTotal = snapshot price × quantity, sorted by item ID.
- `statusHistory`: OrderStatusHistory with updater name from User, oldest first
  (creation time then history ID).
- `delivery`: DeliveryAssignment → Shipper → User contact details, or null.
- `payments`: PaymentTransaction records, oldest first (creation time then ID).
  An empty list does not imply that a COD order is unpaid.

Lists are empty when no records exist. DTOs do not expose whole JPA entities.
Product/ProductVariant and Address are not needed for purchased-item names/prices
or shipping address because order-time snapshots are available.
COD remittance is shown separately from shop settlement using Order.IsCODRemitted.
CODDetail now links individual orders to CODCollection; this page reads the stored order flag.

## Layout reference for the next UI step

Use `src/main/resources/templates/shop/orders.html` and
`fragments/ShopSidebar` as the source of truth:

- Shared shop sidebar with the orders section active.
- Flex layout, pale blue background #eef8ff, dark text #18334c.
- White cards, #d6e8f5 borders, 18px corner radius and subtle shadows.
- Primary buttons #3671ff; outlined secondary buttons and red cancel actions.
- Pill status badges: yellow pending, teal confirmed, red failed/cancelled,
  blue for other statuses.
- Match heading typography, content spacing and mobile breakpoints at 1000/700px.

The detail page is available at GET /shop/orders/{orderId}?ownerId={ownerId} and is linked from the order list. Its status forms reuse the list endpoints with returnToDetail=true so successful updates return to the detail page.

## Money breakdown

The customer total (FinalAmount) remains the stored checkout amount. Shop receipts on
the list, detail and API are calculated on read using the following rules:

- When Shop.CommissionRate is non-null (including 0%), fee amount =
  round(TotalAmount * Shop.CommissionRate / 100, 2), using HALF_UP, before vouchers
  and excluding shipping.
- Otherwise use Order.PlatformFee as the stored **currency amount**. Existing SQL
  Server data stores amounts such as 20,000 for a product total of 400,000.
  The displayed equivalent percentage is fee * 100 / TotalAmount (0 for a zero total
  and zero fee). This is display-only and is never multiplied back to recalculate the fee.
  Even a fee below 100 is currency; never infer its unit from its magnitude.
- Coupon.ShopId = 0 identifies an admin coupon covering the platform. Legacy null
  ShopId is treated the same way. Its DiscountAmount does not reduce shop receipts.
- A positive Coupon.ShopId must match Order.ShopId. That shop bears DiscountAmount.
  Mismatched or negative coupon ownership is rejected with HTTP 409; confirmation
  validates ownership before stock changes. Existing stock validation also requires
  all ordered variants to belong to the order's shop.
- Net receipts = TotalAmount - shop-funded DiscountAmount - fee amount.
  No coupon means no shop-funded voucher deduction.

For products costing 100,000, a 10,000 voucher and a 5% rate, admin coupons yield
95,000 for the shop; shop coupons yield 85,000. Shipping never enters these calculations.
The detail DTO's `platformFee` remains a **calculated currency amount**, with separate
`commissionRate`, `commissionSource` (SHOP/PLATFORM), and `shopDiscountAmount` fields.
The summary/list adds `couponCode` and `couponSource` (NONE/SHOP/PLATFORM).
`receivedAmount` and `shopNetReceived` both contain the calculated net receipts.
Sorting by net receipts uses the same formula in the database before pagination.

Reads do not overwrite Order.ShopNetReceived or change balances, settlement or payments.
Changing the shop's current rate therefore changes displayed receipts for existing orders.
This repository has no checkout/coupon-application endpoint; future checkout code must
reuse these ownership rules and verify product ownership before accepting a coupon.

No fee data conversion is needed: Order.PlatformFee retains its existing currency unit.
There is no separate platform percentage configuration table in this codebase.
The original `Coupon.shop` ManyToOne mapping is preserved. Pricing reads only that
association's identifier; the sort expression uses Hibernate `fk(c.shop)` to read
the stored ShopId without joining a nonexistent Shop 0. A legacy null association
still identifies a platform coupon. If Coupon.ShopId has a database foreign key to
Shop, that existing constraint must already permit any 0 sentinel stored in the data.
This implementation does not change the schema, create a Shop 0 or disable live constraints.
`Shop` retains its original 5% initialization default; an explicitly null rate loaded
from the database still uses the stored monetary PlatformFee fallback.
No migration or live data changes are performed by the order read path.

The list controller logs failures and distinguishes data validation, database connection,
query and unexpected processing errors. A pricing validation failure must not be reported
as a lost SQL Server connection. Hibernate SELECT and count_big output are normal SQL logs.

Total cost = sum(OrderItem.CostSnapshot * Quantity). Estimated profit = calculated net receipts - total cost;
shipping and platform fees are not deducted again. A loss remains negative.
No items or missing cost snapshots produce an unknown cost/profit rather than zero.
Explicit zero cost snapshots remain valid stored values (including migrated default values).
Profit is indicative and excludes other expenses; COD remittance and settlement do not alter it.
Money formatting preserves the two decimal places when the stored/calculated amount has a fractional part.

## Stock on shop status changes

PENDING -> CONFIRMED deducts the sum of ordered quantities per variant. Confirmation fails
with HTTP 409 if any line is invalid or stock is insufficient. PENDING -> CANCELLED leaves
stock unchanged. Cancelling CONFIRMED, PREPARING or READY returns the same quantities.
Advancing to PREPARING/READY does not deduct again. Other cancellation states are rejected.
The order is write-locked before checking its status; variants are write-locked in ID order.
Stock, InventoryTransaction (ORDER_DEDUCT/CANCEL_REFUND) and status history share one transaction.
Repeated confirmation/cancellation is rejected by the locked order status.

Deployment: confirmed/preparing/ready orders created before this feature did not automatically
deduct stock. Reconcile those existing orders with actual stock before using cancellation
under this workflow; InventoryTransaction currently has no order reference to identify old reservations.
