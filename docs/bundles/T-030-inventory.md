---
feature: Inventory Management with Pessimistic Locking
task: T-030
issue: #4
archived: 2026-05-12
files:
  - src/main/resources/db/migration/V4__create_inventory.sql
  - src/main/java/com/example/eom/domain/Inventory.java
  - src/main/java/com/example/eom/dto/inventory/InventoryResponse.java
  - src/main/java/com/example/eom/dto/inventory/UpdateInventoryRequest.java
  - src/main/java/com/example/eom/exception/InsufficientStockException.java
  - src/main/java/com/example/eom/repository/InventoryRepository.java
  - src/main/java/com/example/eom/service/InventoryService.java
  - src/main/java/com/example/eom/service/impl/InventoryServiceImpl.java
  - src/main/java/com/example/eom/controller/admin/AdminInventoryController.java
  - src/main/java/com/example/eom/config/GlobalExceptionHandler.java
  - src/test/java/com/example/eom/service/InventoryServiceImplTest.java
  - src/test/java/com/example/eom/controller/AdminInventoryControllerTest.java
---

# Bundle: Inventory Management with Pessimistic Locking

## API Surface
| Method | Path | Auth | Handler |
|--------|------|------|---------|
| GET | /api/admin/inventory | ADMIN | AdminInventoryController.list() |
| PUT | /api/admin/inventory/{productId} | ADMIN | AdminInventoryController.updateStock() |

> `reserve`, `deduct`, `release` are internal service methods — no HTTP exposure. Called by OrderService (T-050).

## Logic Map

### InventoryServiceImpl (file: src/main/java/com/example/eom/service/impl/InventoryServiceImpl.java)
- `list(productId)`: if productId non-null → findByProductId wrapped in List; else findAll — returns List<InventoryResponse>
- `getByProduct(productId)`: non-locking read via findByProductId → throws EntityNotFoundException if absent
- `updateStock(productId, request)`: pessimistic-lock fetch (creates record if missing) → validates new onHand ≥ current reserved → sets quantityOnHand → saves
- `reserve(productId, qty)`: pessimistic-lock fetch → checks availableQty ≥ qty else throws InsufficientStockException → increments quantityReserved
- `deduct(productId, qty)`: pessimistic-lock fetch → decrements both quantityOnHand and quantityReserved by qty (finalises reservation post-payment)
- `release(productId, qty)`: pessimistic-lock fetch → decrements quantityReserved only (restores on cancellation, onHand unchanged)

### Inventory (file: src/main/java/com/example/eom/domain/Inventory.java)
- `getAvailableQuantity()`: computed helper — returns `quantityOnHand - quantityReserved`
- `preUpdate()`: @PreUpdate — sets updatedAt = Instant.now()

### InventoryRepository (file: src/main/java/com/example/eom/repository/InventoryRepository.java)
- `findByProductIdWithLock(productId)`: `@Lock(PESSIMISTIC_WRITE)` JPQL query — used by all stock-mutating methods
- `findByProductId(productId)`: plain read — used by list/getByProduct

## Business Rules
1. One inventory record per product — `product_id` column has UNIQUE constraint
2. `quantityReserved` must never exceed `quantityOnHand` — enforced by DB CHECK and validated in `updateStock`
3. `reserve()` checks `availableQuantity = onHand − reserved` before incrementing — throws 409 if insufficient
4. `deduct()` decrements both onHand and reserved — used only after payment confirmation
5. `release()` decrements reserved only — used on order cancellation, onHand stays the same
6. `updateStock` is idempotent — creates the inventory record if one doesn't exist yet
7. Admin cannot set `quantityOnHand` below current `quantityReserved` — throws IllegalArgumentException → 400

## Key Decisions
- **Long id** (not UUID from issue spec): consistent with User/Product BIGSERIAL pattern
- **`availableQuantity` computed in Java** (`onHand - reserved`): avoids denormalization, stays consistent without extra column
- **Pessimistic write lock on all mutations**: prevents overselling under concurrent requests; read-only queries (list/getByProduct) use no lock
- **`updateStock` creates record if missing**: admin can set stock before any reservation exists without needing a separate "create" endpoint
- **reserve/deduct/release not exposed via HTTP**: called internally by OrderService only — prevents clients from bypassing business rules

## Exception Handling
- `InsufficientStockException` → 409 — `GlobalExceptionHandler.handleInsufficientStock()`
- `EntityNotFoundException` → 404 — `GlobalExceptionHandler.handleNotFound()`
- `IllegalArgumentException` (below-reserved set) → 400 — `GlobalExceptionHandler.handleBadRequest()`
- `MethodArgumentNotValidException` (@Min violation) → 400 — `GlobalExceptionHandler.handleValidation()`
- Unauthenticated on `/api/admin/**` → 401 — custom `AuthenticationEntryPoint` in SecurityConfig
- Non-ADMIN on `/api/admin/**` → 403 — Spring Security `@PreAuthorize`

## Tests
- `InventoryServiceImplTest`: list (no filter / with filter), updateStock (success / below-reserved), reserve (success / insufficient / not-found), deduct, release — 9 cases
- `AdminInventoryControllerTest`: GET no-auth → 401, GET USER role → 403, GET admin all → 200, GET admin with filter → 200, PUT valid → 200, PUT negative qty → 400, PUT below-reserved → 400 — 7 cases

## Files Index
**Domain:** src/main/java/com/example/eom/domain/Inventory.java
**Repository:** src/main/java/com/example/eom/repository/InventoryRepository.java
**Service:** src/main/java/com/example/eom/service/InventoryService.java, src/main/java/com/example/eom/service/impl/InventoryServiceImpl.java
**Controller:** src/main/java/com/example/eom/controller/admin/AdminInventoryController.java
**DTO:** src/main/java/com/example/eom/dto/inventory/InventoryResponse.java, src/main/java/com/example/eom/dto/inventory/UpdateInventoryRequest.java
**Exception:** src/main/java/com/example/eom/exception/InsufficientStockException.java
**Config:** src/main/java/com/example/eom/config/GlobalExceptionHandler.java (added InsufficientStockException handler)
**Migration:** src/main/resources/db/migration/V4__create_inventory.sql
**Tests:** src/test/java/com/example/eom/service/InventoryServiceImplTest.java, src/test/java/com/example/eom/controller/AdminInventoryControllerTest.java
