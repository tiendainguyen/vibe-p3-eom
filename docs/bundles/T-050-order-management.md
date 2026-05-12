---
feature: Order Management & Lifecycle
task: T-050
issue: #6
archived: 2026-05-12
files:
  - src/main/resources/db/migration/V6__create_orders.sql
  - src/main/resources/db/migration/V7__create_order_items.sql
  - src/main/java/com/example/eom/domain/enums/OrderStatus.java
  - src/main/java/com/example/eom/domain/Order.java
  - src/main/java/com/example/eom/domain/OrderItem.java
  - src/main/java/com/example/eom/dto/order/OrderItemResponse.java
  - src/main/java/com/example/eom/dto/order/OrderResponse.java
  - src/main/java/com/example/eom/dto/order/UpdateOrderStatusRequest.java
  - src/main/java/com/example/eom/repository/OrderRepository.java
  - src/main/java/com/example/eom/repository/OrderItemRepository.java
  - src/main/java/com/example/eom/service/OrderService.java
  - src/main/java/com/example/eom/service/impl/OrderServiceImpl.java
  - src/main/java/com/example/eom/controller/OrderController.java
  - src/main/java/com/example/eom/controller/admin/AdminOrderController.java
  - src/main/java/com/example/eom/config/GlobalExceptionHandler.java
  - src/test/java/com/example/eom/service/OrderServiceImplTest.java
  - src/test/java/com/example/eom/controller/OrderControllerTest.java
---

# Bundle: Order Management & Lifecycle

## API Surface
| Method | Path | Auth | Handler |
|--------|------|------|---------|
| POST | /api/orders | JWT | OrderController.createFromCart() |
| GET | /api/orders | JWT | OrderController.listMyOrders() |
| GET | /api/orders/{id} | JWT | OrderController.getById() |
| POST | /api/orders/{id}/cancel | JWT | OrderController.cancel() |
| GET | /api/admin/orders | ADMIN | AdminOrderController.listAll() |
| PUT | /api/admin/orders/{id}/status | ADMIN | AdminOrderController.updateStatus() |

## Logic Map

### OrderServiceImpl (file: src/main/java/com/example/eom/service/impl/OrderServiceImpl.java)
- `createFromCart(userId)`: gets cart via CartService → throws 400 if empty → reserves inventory for each item (rollback on failure) → saves Order + OrderItems with snapshotted prices → clears cart → returns OrderResponse
- `getById(userId, orderId)`: finds order or throws 404 → throws AccessDeniedException if `order.userId != userId` → returns response with items
- `listByUser(userId, pageable)`: `findByUserIdOrderByCreatedAtDesc` → maps each order to response (loads items per order)
- `cancel(userId, orderId)`: finds order, checks ownership → throws 409 if status != PENDING → releases inventory for all items → sets CANCELLED
- `adminList(status, userId, from, to, pageable)`: JPQL filter query with all-nullable params → maps to responses
- `updateStatus(orderId, request)`: finds order → validates against `VALID_ADMIN_TRANSITIONS` map → throws 409 if invalid → sets new status
- `toResponse(order, items)`: private — maps OrderItem list to OrderItemResponse (computes subtotal = unitPrice × qty) → builds OrderResponse

### VALID_ADMIN_TRANSITIONS (static EnumMap in OrderServiceImpl)
- `PAID → PROCESSING`, `PROCESSING → SHIPPED`, `SHIPPED → DELIVERED` — any other transition throws IllegalStateException → 409

## Business Rules
1. Order is created only from a non-empty cart — throws 400 if cart has no items
2. All inventory reservations happen atomically — `@Transactional` rolls back all reserves if any item fails
3. Prices and product names are snapshotted at checkout from `CartItemResponse` (live product data) — stored in `order_items.unit_price` and `product_name`
4. Cart is cleared after successful order creation
5. Only PENDING orders can be cancelled by the customer — 409 for any other status
6. Cancellation releases inventory (`InventoryService.release()`) for all order items
7. Customer can only view/cancel their own orders — AccessDeniedException → 403 for ownership mismatch
8. Admin status transitions are strictly linear: PAID→PROCESSING→SHIPPED→DELIVERED only

## Key Decisions
- **Long id** (not UUID per spec): consistent with all other entities in this project
- **`stripePaymentIntentId` nullable**: set by T-060 (Payment) — order creation does not touch Stripe
- **CartService injected** (not CartItemRepository directly): respects service boundary; `getCart()` fetches live prices for snapshotting and `clearCart()` cleans up
- **`toResponse()` loads items via parameter** in `createFromCart` (uses saved list, not a re-query): avoids extra DB call; all other methods load via `orderItemRepository.findByOrderId()`
- **AccessDeniedException → 403** added to GlobalExceptionHandler: Spring MVC's `@ControllerAdvice` catches it before filter-level handling when thrown from service layer

## Exception Handling
- `IllegalArgumentException` (empty cart) → 400 — `GlobalExceptionHandler.handleBadRequest()`
- `EntityNotFoundException` (order not found) → 404 — `GlobalExceptionHandler.handleNotFound()`
- `AccessDeniedException` (wrong user) → 403 — `GlobalExceptionHandler.handleAccessDenied()` ← added in T-050
- `IllegalStateException` (cancel non-PENDING / invalid admin transition) → 409 — `GlobalExceptionHandler.handleConflict()`
- `InsufficientStockException` (stock check during reserve) → 409 — `GlobalExceptionHandler.handleInsufficientStock()`
- Unauthenticated → 401 — custom `AuthenticationEntryPoint` in SecurityConfig

## Tests
- `OrderServiceImplTest`: createFromCart (happy path — verifies reserve + clearCart / empty cart → 400), getById (own / not found → 404 / wrong user → 403), cancel (PENDING / non-PENDING → 409 / wrong user → 403), updateStatus (valid PAID→PROCESSING / invalid PENDING→DELIVERED → 409) — 10 cases
- `OrderControllerTest`: no-auth 401 (POST + GET), createOrder 201, empty cart 400, listOrders 200, getById 200, getById wrong-user 403, getById 404, cancel 200, cancel non-PENDING 409, adminList 200, adminList user-role 403, updateStatus 200, updateStatus null 400, updateStatus invalid 409 — 15 cases; uses `asUser()` / `asAdmin()` helpers with String principal

## Files Index
**Domain:** src/main/java/com/example/eom/domain/Order.java, src/main/java/com/example/eom/domain/OrderItem.java, src/main/java/com/example/eom/domain/enums/OrderStatus.java
**Repository:** src/main/java/com/example/eom/repository/OrderRepository.java, src/main/java/com/example/eom/repository/OrderItemRepository.java
**Service:** src/main/java/com/example/eom/service/OrderService.java, src/main/java/com/example/eom/service/impl/OrderServiceImpl.java
**Controller:** src/main/java/com/example/eom/controller/OrderController.java, src/main/java/com/example/eom/controller/admin/AdminOrderController.java
**DTO:** src/main/java/com/example/eom/dto/order/OrderResponse.java, src/main/java/com/example/eom/dto/order/OrderItemResponse.java, src/main/java/com/example/eom/dto/order/UpdateOrderStatusRequest.java
**Config:** src/main/java/com/example/eom/config/GlobalExceptionHandler.java (added AccessDeniedException → 403)
**Migration:** src/main/resources/db/migration/V6__create_orders.sql, src/main/resources/db/migration/V7__create_order_items.sql
**Tests:** src/test/java/com/example/eom/service/OrderServiceImplTest.java, src/test/java/com/example/eom/controller/OrderControllerTest.java
