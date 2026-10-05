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

The customer total (FinalAmount), PlatformFee and ShopNetReceived are stored order values.
Current Shop.CommissionRate and ProductVariant.CostPrice never recalculate historical orders.
The shop card shows the stored platform fee, already deducted from net receipts.
Total cost = sum(OrderItem.CostSnapshot * Quantity). Estimated profit = ShopNetReceived - total cost;
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
