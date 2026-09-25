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
CODCollection is not used: the current entity has no OrderId relationship.

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
