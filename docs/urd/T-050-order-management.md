---
task_id: T-050
feature: Order Management
status: pending
effort: L
dependencies: [T-040, T-030]
---

# URD — Order Management

## 1. Business Context
Order management is the central business process of the system. It converts a user's cart into a confirmed order, snapshots product prices at checkout time, coordinates inventory reservation, and drives the full lifecycle from PENDING through PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED, and REFUNDED.

## 2. User Stories
- As a **shopper**, I want to create an order from my cart so that I can proceed to payment.
- As a **shopper**, I want to view my order history so that I can track past purchases.
- As a **shopper**, I want to view a specific order with full line items so that I see exactly what I ordered.
- As a **shopper**, I want to cancel a pending order so that I release my commitment before paying.
- As the **system**, I want prices snapshotted at order time so that future price changes don't affect existing orders.
- As the **system**, I want inventory reserved on order creation so that stock is held while the user pays.
- As an **admin**, I want to advance an order's status (PROCESSING → SHIPPED → DELIVERED) so that fulfilment is tracked.

## 3. Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-050-1 | `POST /api/orders` creates order from cart, snapshots prices, reserves inventory, clears cart | Must Have |
| FR-050-2 | Empty cart creation attempt returns `400` | Must Have |
| FR-050-3 | `GET /api/orders` returns paginated order list for authenticated user | Must Have |
| FR-050-4 | `GET /api/orders/{id}` returns full order with line items | Must Have |
| FR-050-5 | `POST /api/orders/{id}/cancel` cancels a PENDING order, releases inventory | Must Have |
| FR-050-6 | Cancelling a non-PENDING order returns `409 Conflict` | Must Have |
| FR-050-7 | Order status follows the defined lifecycle — no backward transitions | Must Have |
| FR-050-8 | Admin can update order status via `PUT /api/admin/orders/{id}/status` | Must Have |
| FR-050-9 | `GET /api/admin/orders` returns all orders across all users (paginated) | Must Have |

## 4. API Endpoints
| Method | Path | Auth | Request Body | Response | Description |
|--------|------|------|--------------|----------|-------------|
| POST | /api/orders | Bearer JWT | — | `OrderResponse` | Create order from cart |
| GET | /api/orders | Bearer JWT | — | `Page<OrderSummaryResponse>` | List user's orders |
| GET | /api/orders/{id} | Bearer JWT | — | `OrderResponse` | Get order detail |
| POST | /api/orders/{id}/cancel | Bearer JWT | — | `OrderResponse` | Cancel a PENDING order |
| GET | /api/admin/orders | Bearer JWT (ADMIN) | — | `Page<OrderSummaryResponse>` | List all orders |
| PUT | /api/admin/orders/{id}/status | Bearer JWT (ADMIN) | `UpdateOrderStatusRequest` | `OrderResponse` | Advance order status |

## 5. Data Entities
**Order**
- `id` (Long, PK, auto-increment)
- `user` → User (ManyToOne, not null)
- `status` (Enum: PENDING / PAID / PROCESSING / SHIPPED / DELIVERED / CANCELLED / REFUNDED)
- `totalAmount` (BigDecimal, not null, precision 12 scale 2)
- `stripePaymentIntentId` (String, nullable) — set after payment step
- `createdAt` (Instant, not null)
- `updatedAt` (Instant, not null)

**OrderItem**
- `id` (Long, PK, auto-increment)
- `order` → Order (ManyToOne, not null)
- `product` → Product (ManyToOne, not null)
- `productName` (String, not null) — snapshot
- `unitPrice` (BigDecimal, not null) — snapshot at order time
- `quantity` (Integer, not null, min 1)

**Flyway migration files:** `V6__create_orders.sql`, `V7__create_order_items.sql`

## 6. Business Rules
1. Order created only if cart has ≥ 1 active item.
2. `unitPrice` on `OrderItem` is snapshotted from `Product.price` at creation time — never recalculated.
3. `Order.totalAmount` = sum of `(unitPrice × quantity)` across all items.
4. Inventory is reserved atomically during order creation; if any item fails reservation, the entire order creation rolls back.
5. Valid forward-only status transitions:
   - PENDING → PAID (via payment webhook)
   - PAID → PROCESSING → SHIPPED → DELIVERED (admin only)
   - PENDING → CANCELLED (user or admin)
   - PAID → REFUNDED (via Stripe refund webhook — T-060)
6. A user may only view their own orders; admin can view all.
7. `userId` extracted from JWT — never from request body.

## 7. Implementation Layers
1. **Domain** — `Order` entity, `OrderItem` entity, `OrderStatus` enum
2. **Migration** — `V6__create_orders.sql`, `V7__create_order_items.sql`
3. **Repository** — `OrderRepository`, `OrderItemRepository` with user-scoped queries
4. **Service**
   - `OrderService` interface: `createFromCart(Long userId)`, `getOrder(Long userId, Long orderId)`, `listOrders(Long userId, Pageable)`, `cancelOrder(Long userId, Long orderId)`, `updateStatus(Long orderId, OrderStatus newStatus)`
   - `OrderServiceImpl` — `@Transactional` on `createFromCart` and `cancelOrder`; calls `CartService`, `InventoryService`
5. **Controller**
   - `OrderController` — `/api/orders/**`
   - `AdminOrderController` — `/api/admin/orders/**` (ADMIN only)
6. **Config** — none additional
7. **Tests**
   - `OrderServiceImplTest` — create from cart (happy path), create empty cart (throws), cancel PENDING (succeeds), cancel PAID (throws conflict), status transition validation
   - `OrderControllerTest` — POST 201, GET list 200, GET single 200/404, cancel 200/409
   - `AdminOrderControllerTest` — GET all 200, PUT status 200, 403 for non-admin

## 8. Acceptance Criteria
- [ ] `POST /api/orders` with items in cart returns `201` with order payload and PENDING status
- [ ] Cart is empty after successful order creation
- [ ] `OrderItem.unitPrice` reflects price at time of order (not current product price after update)
- [ ] `POST /api/orders` with empty cart returns `400`
- [ ] `POST /api/orders/{id}/cancel` on PENDING order returns `200` and releases inventory
- [ ] `POST /api/orders/{id}/cancel` on PAID order returns `409`
- [ ] User A cannot view or cancel User B's order (`404` for wrong owner)
- [ ] `GET /api/admin/orders` returns orders from all users
- [ ] `PUT /api/admin/orders/{id}/status` with invalid transition returns `409`
- [ ] All order tests pass

## 9. Security Checklist
- [ ] `userId` extracted from JWT principal only — never from request body
- [ ] User-scoped queries always include `userId` filter
- [ ] Admin endpoints protected with `@PreAuthorize("hasRole('ADMIN')")`
- [ ] `@Transactional` on order creation to ensure atomicity with inventory reservation
- [ ] Order total computed server-side — never trusted from client

## 10. Non-Functional Notes
- `createFromCart` is the most critical transaction: it must atomically create the order, reserve inventory, and clear the cart. If any step fails, the whole operation rolls back.
- Order history pagination default: `page=0, size=10, sort=createdAt,desc`.
- `stripePaymentIntentId` will be populated by T-060 (Payment Processing).
