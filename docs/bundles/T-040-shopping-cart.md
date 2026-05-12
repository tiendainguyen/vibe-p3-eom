---
feature: Shopping Cart
task: T-040
issue: #5
archived: 2026-05-12
files:
  - src/main/resources/db/migration/V5__create_cart_items.sql
  - src/main/java/com/example/eom/domain/CartItem.java
  - src/main/java/com/example/eom/dto/cart/AddToCartRequest.java
  - src/main/java/com/example/eom/dto/cart/UpdateCartItemRequest.java
  - src/main/java/com/example/eom/dto/cart/CartItemResponse.java
  - src/main/java/com/example/eom/dto/cart/CartResponse.java
  - src/main/java/com/example/eom/repository/CartItemRepository.java
  - src/main/java/com/example/eom/service/CartService.java
  - src/main/java/com/example/eom/service/impl/CartServiceImpl.java
  - src/main/java/com/example/eom/controller/CartController.java
  - src/test/java/com/example/eom/service/CartServiceImplTest.java
  - src/test/java/com/example/eom/controller/CartControllerTest.java
---

# Bundle: Shopping Cart

## API Surface
| Method | Path | Auth | Handler |
|--------|------|------|---------|
| GET | /api/cart | JWT (any role) | CartController.getCart() |
| POST | /api/cart/items | JWT (any role) | CartController.addItem() |
| PUT | /api/cart/items/{productId} | JWT (any role) | CartController.updateItem() |
| DELETE | /api/cart/items/{productId} | JWT (any role) | CartController.removeItem() |
| DELETE | /api/cart | JWT (any role) | CartController.clearCart() |

## Logic Map

### CartServiceImpl (file: src/main/java/com/example/eom/service/impl/CartServiceImpl.java)
- `getCart(userId)`: loads all CartItems by userId → builds CartResponse via `buildCartResponse()`
- `addItem(userId, request)`: checks product active via ProductRepository filter → checks `availableQuantity >= request.quantity()` via InventoryService → upserts CartItem (finds existing or creates new with qty=0, then increments) → returns refreshed cart
- `updateItem(userId, productId, request)`: finds item or throws 404 → if qty=0: delete; else: set qty → returns refreshed cart
- `removeItem(userId, productId)`: finds item or throws 404 → deletes → returns refreshed cart
- `clearCart(userId)`: `deleteByUserId(userId)` — no return value
- `buildCartResponse(userId, items)`: private — for each CartItem fetches product (name, price) from ProductRepository, computes subtotal = price × qty; sums to grand total → returns CartResponse

### CartController (file: src/main/java/com/example/eom/controller/CartController.java)
- `userId(auth)`: private — `Long.parseLong((String) authentication.getPrincipal())` — matches String principal set by JwtAuthFilter

## Business Rules
1. Cart is per-user — all queries filter by `userId` extracted from JWT; no cross-user access possible
2. `addItem` upserts: existing product+user row has quantity incremented, not replaced
3. `updateItem` with `quantity=0` removes the item (not a validation error)
4. Stock availability checked at `addItem` time only: `availableQuantity >= requested quantity` → 409 if insufficient
5. Inventory is NOT reserved when adding to cart — reservation happens at order creation (T-050)
6. Cart total = Σ(product.price × cartItem.quantity) — price is live from Product entity, not snapshotted
7. Cart has no separate Cart entity — it is the set of CartItems for a userId

## Key Decisions
- **ProductRepository injected directly** (not via ProductService): `ProductService.getById()` filters active=true and throws 404 for inactive — that's the desired behavior for addItem. For `buildCartResponse`, inactive products in existing cart items would throw 404; this is acceptable for now (future: filter gracefully)
- **No inventory reservation at cart time**: matches issue spec "not reserved yet" — reservation is OrderService's responsibility
- **`updateItem(qty=0)` removes item**: cleaner than requiring a separate DELETE call for the same intent
- **`asUser()` test helper**: `@WithMockUser` sets `UserDetails` principal — incompatible with `(String) auth.getPrincipal()`. Tests use `SecurityMockMvcRequestPostProcessors.authentication()` with `UsernamePasswordAuthenticationToken(userId, null, roles)` instead

## Exception Handling
- `EntityNotFoundException` (inactive/missing product, item not in cart) → 404 — `GlobalExceptionHandler.handleNotFound()`
- `InsufficientStockException` (availableQty < requested) → 409 — `GlobalExceptionHandler.handleInsufficientStock()`
- `MethodArgumentNotValidException` (@Min violations) → 400 — `GlobalExceptionHandler.handleValidation()`
- Unauthenticated → 401 — custom `AuthenticationEntryPoint` in SecurityConfig

## Tests
- `CartServiceImplTest`: getCart (empty / with items + total calc), addItem (new item / existing→increment / inactive→404 / out-of-stock→409), updateItem (update qty / qty=0→remove / not-in-cart→404), removeItem (found / not-found), clearCart — 12 cases
- `CartControllerTest`: no-auth 401 (GET + POST), getCart 200, addItem (200 / 400 bad qty / 404 inactive / 409 stock), updateItem (200 / 404), removeItem 200, clearCart 204 — 11 cases; uses `asUser()` helper with String principal

## Files Index
**Domain:** src/main/java/com/example/eom/domain/CartItem.java
**Repository:** src/main/java/com/example/eom/repository/CartItemRepository.java
**Service:** src/main/java/com/example/eom/service/CartService.java, src/main/java/com/example/eom/service/impl/CartServiceImpl.java
**Controller:** src/main/java/com/example/eom/controller/CartController.java
**DTO:** src/main/java/com/example/eom/dto/cart/AddToCartRequest.java, src/main/java/com/example/eom/dto/cart/UpdateCartItemRequest.java, src/main/java/com/example/eom/dto/cart/CartItemResponse.java, src/main/java/com/example/eom/dto/cart/CartResponse.java
**Migration:** src/main/resources/db/migration/V5__create_cart_items.sql
**Tests:** src/test/java/com/example/eom/service/CartServiceImplTest.java, src/test/java/com/example/eom/controller/CartControllerTest.java
