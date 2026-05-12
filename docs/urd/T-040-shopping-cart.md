---
task_id: T-040
feature: Shopping Cart
status: pending
effort: M
dependencies: [T-010, T-020, T-030]
---

# URD — Shopping Cart

## 1. Business Context
The shopping cart is a transient collection of items a user intends to purchase. It bridges product browsing and order creation. The cart checks real-time inventory availability before allowing items to be added, so checkout failures due to stock issues are surfaced early.

## 2. User Stories
- As a **shopper**, I want to add a product to my cart so that I can proceed to checkout later.
- As a **shopper**, I want to update the quantity of an item in my cart so that I can buy more or fewer units.
- As a **shopper**, I want to remove an item from my cart so that I can change my mind.
- As a **shopper**, I want to view my cart with current prices and subtotal so that I know what I'll pay.
- As a **shopper**, I want my cart rejected if I add more items than are in stock so that I'm not surprised at checkout.
- As the **system**, I want to clear the cart after a successful order is created so that users start fresh.

## 3. Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-040-1 | `POST /api/cart/items` adds a product to the authenticated user's cart | Must Have |
| FR-040-2 | Adding the same product again increments quantity | Must Have |
| FR-040-3 | Quantity exceeding available inventory returns `400` with descriptive error | Must Have |
| FR-040-4 | `PUT /api/cart/items/{productId}` updates quantity; quantity=0 removes the item | Must Have |
| FR-040-5 | `DELETE /api/cart/items/{productId}` removes a specific item | Must Have |
| FR-040-6 | `GET /api/cart` returns all cart items with current price and computed subtotal | Must Have |
| FR-040-7 | `DELETE /api/cart` clears all items (also called internally after order creation) | Must Have |
| FR-040-8 | Cart is user-scoped — a user can only see and modify their own cart | Must Have |

## 4. API Endpoints
| Method | Path | Auth | Request Body | Response | Description |
|--------|------|------|--------------|----------|-------------|
| GET | /api/cart | Bearer JWT | — | `CartResponse` | Get current user's cart |
| POST | /api/cart/items | Bearer JWT | `AddCartItemRequest` | `CartResponse` | Add item to cart |
| PUT | /api/cart/items/{productId} | Bearer JWT | `UpdateCartItemRequest` | `CartResponse` | Update item quantity |
| DELETE | /api/cart/items/{productId} | Bearer JWT | — | `CartResponse` | Remove item from cart |
| DELETE | /api/cart | Bearer JWT | — | `204 No Content` | Clear entire cart |

## 5. Data Entities
**CartItem**
- `id` (Long, PK, auto-increment)
- `user` → User (ManyToOne, not null)
- `product` → Product (ManyToOne, not null)
- `quantity` (Integer, not null, min 1)
- `addedAt` (Instant, not null)
- Unique constraint: `(user_id, product_id)`

**Flyway migration file:** `V5__create_cart_items.sql`

## 6. Business Rules
1. Cart items are user-scoped: a user may never read or modify another user's cart.
2. Adding a product checks current available inventory (`InventoryService.getInventory`) — request rejected if requested quantity exceeds available stock.
3. If a product becomes inactive after being added to cart, it is excluded from the cart response (filtered out).
4. Quantity must be ≥ 1; updating to 0 is equivalent to removing the item.
5. Cart is cleared atomically by `CartService.clearCart(userId)` after a successful `OrderService.createFromCart()`.

## 7. Implementation Layers
1. **Domain** — `CartItem` entity
2. **Migration** — `V5__create_cart_items.sql`
3. **Repository** — `CartItemRepository extends JpaRepository<CartItem, Long>` with `findByUserId(Long userId)`, `findByUserIdAndProductId(Long userId, Long productId)`, `deleteByUserId(Long userId)`
4. **Service**
   - `CartService` interface: `getCart(Long userId)`, `addItem(Long userId, AddCartItemRequest)`, `updateItem(Long userId, Long productId, int quantity)`, `removeItem(Long userId, Long productId)`, `clearCart(Long userId)`
   - `CartServiceImpl` — calls `InventoryService` for stock check
5. **Controller**
   - `CartController` — `/api/cart/**`, extracts `userId` from JWT principal
6. **Config** — none additional
7. **Tests**
   - `CartServiceImplTest` — add item, add duplicate (increment), add over-stock (throws), update quantity, remove item, clear cart
   - `CartControllerTest` — GET /cart 200, POST /api/cart/items 200/400, DELETE 204, 401 without JWT

## 8. Acceptance Criteria
- [ ] `POST /api/cart/items` with valid product and quantity returns updated cart
- [ ] Adding same product twice accumulates quantity
- [ ] Adding quantity > available stock returns `400` with stock error message
- [ ] `PUT /api/cart/items/{productId}` with `quantity=0` removes the item
- [ ] `GET /api/cart` returns correct subtotal (sum of price × quantity per item)
- [ ] `DELETE /api/cart` returns `204` and cart is empty on next GET
- [ ] User A cannot see User B's cart (different JWT → different cart)
- [ ] Inactive product is excluded from cart response
- [ ] All cart tests pass

## 9. Security Checklist
- [ ] `userId` extracted from JWT principal — never from request body or path variable
- [ ] Cart queries always filter by `userId` to prevent cross-user data access
- [ ] `quantity` validated as positive integer (`@Min(1)`)
- [ ] `productId` validated to exist (404 if not found) before adding to cart

## 10. Non-Functional Notes
- Cart is DB-backed (not Redis) — simplifies consistency guarantees with inventory checks.
- No cart TTL / expiry in scope — items persist until user checks out or removes them.
- `CartResponse` includes a computed `totalAmount` field (server-side calculation).
