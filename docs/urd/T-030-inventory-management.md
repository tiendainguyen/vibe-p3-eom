---
task_id: T-030
feature: Inventory Management
status: pending
effort: M
dependencies: [T-020]
---

# URD — Inventory Management

## 1. Business Context
Inventory prevents overselling. Each product has a stock level that must be atomically reserved when an order is placed and deducted when payment is confirmed. Pessimistic locking is used during reserve/deduct operations to handle concurrent checkout flows safely.

## 2. User Stories
- As a **shopper**, I want to see current stock availability on product detail so that I know what I can buy.
- As the **system**, I want to reserve stock when an order is created so that two users cannot buy the same last item.
- As the **system**, I want to deduct reserved stock when payment is confirmed so that inventory is permanently reduced.
- As the **system**, I want to release reserved stock when an order is cancelled so that items return to available stock.
- As an **admin**, I want to set and adjust stock levels so that inventory stays accurate.

## 3. Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-030-1 | Each product has a corresponding `Inventory` record with `quantity` and `reserved` fields | Must Have |
| FR-030-2 | `reserve(productId, quantity)` decrements `quantity`, increments `reserved` atomically | Must Have |
| FR-030-3 | Reserve fails with `InsufficientStockException` if `quantity < requested` | Must Have |
| FR-030-4 | `deduct(productId, quantity)` decrements `reserved` (called on payment confirmed) | Must Have |
| FR-030-5 | `release(productId, quantity)` increments `quantity`, decrements `reserved` (called on cancel) | Must Have |
| FR-030-6 | All reserve/deduct/release operations use pessimistic write lock (`PESSIMISTIC_WRITE`) | Must Have |
| FR-030-7 | `PUT /api/admin/inventory/{productId}` sets stock level (ADMIN only) | Must Have |
| FR-030-8 | `GET /api/products/{id}` response includes available quantity (from Inventory) | Should Have |

## 4. API Endpoints
| Method | Path | Auth | Request Body | Response | Description |
|--------|------|------|--------------|----------|-------------|
| PUT | /api/admin/inventory/{productId} | Bearer JWT (ADMIN) | `SetInventoryRequest` | `InventoryResponse` | Set stock quantity for a product |
| GET | /api/admin/inventory/{productId} | Bearer JWT (ADMIN) | — | `InventoryResponse` | View stock levels |

(Reserve/deduct/release are internal service calls — not exposed as REST endpoints)

## 5. Data Entities
**Inventory**
- `id` (Long, PK, auto-increment)
- `product` → Product (OneToOne, not null)
- `quantity` (Integer, not null, min 0) — available stock
- `reserved` (Integer, not null, default 0) — held by pending orders
- `updatedAt` (Instant, not null)

**Flyway migration file:** `V4__create_inventory.sql`

## 6. Business Rules
1. `quantity` can never go below 0 — any attempt raises `InsufficientStockException`.
2. `reserved` can never exceed `quantity + reserved` (i.e., you can't reserve more than exists).
3. Reserve and deduct MUST use `@Lock(LockModeType.PESSIMISTIC_WRITE)` to prevent race conditions.
4. When an admin sets stock to a value lower than current `reserved`, the operation is rejected.
5. Inventory record is created automatically when a new product is created (via `ProductService` → `InventoryService.initialize(productId, 0)`).

## 7. Implementation Layers
1. **Domain** — `Inventory` entity
2. **Migration** — `V4__create_inventory.sql`
3. **Repository** — `InventoryRepository extends JpaRepository<Inventory, Long>` with `findByProductIdWithLock(@Lock PESSIMISTIC_WRITE)`
4. **Service**
   - `InventoryService` interface: `reserve(Long productId, int qty)`, `deduct(Long productId, int qty)`, `release(Long productId, int qty)`, `setStock(Long productId, int qty)`, `getInventory(Long productId)`
   - `InventoryServiceImpl` — all mutating methods `@Transactional`
5. **Controller**
   - `AdminInventoryController` — `/api/admin/inventory/**` (ADMIN only)
6. **Config** — none additional
7. **Tests**
   - `InventoryServiceImplTest` — reserve success, reserve insufficient stock throws, deduct, release, set stock
   - `AdminInventoryControllerTest` — PUT 200, GET 200, 403 for non-admin

## 8. Acceptance Criteria
- [ ] `PUT /api/admin/inventory/{productId}` sets quantity and returns updated record
- [ ] `reserve(productId, qty)` when qty available: `quantity` decreases, `reserved` increases
- [ ] `reserve(productId, qty)` when qty insufficient: throws `InsufficientStockException` with no DB change
- [ ] `deduct(productId, qty)` decreases `reserved` correctly
- [ ] `release(productId, qty)` restores `quantity` and decreases `reserved`
- [ ] Concurrent reserve calls for last unit: only one succeeds (pessimistic lock prevents oversell)
- [ ] Setting stock below current `reserved` returns `400`
- [ ] All inventory tests pass

## 9. Security Checklist
- [ ] Admin inventory endpoints protected with `@PreAuthorize("hasRole('ADMIN')")`
- [ ] Quantity validated as non-negative (`@Min(0)`) in request DTOs
- [ ] No raw SQL — all queries via Spring Data with `@Lock` annotation
- [ ] `@Transactional` on all mutating service methods to ensure atomicity

## 10. Non-Functional Notes
- Pessimistic locking serializes concurrent access to the same product's inventory row — acceptable for expected order volumes.
- If the product table has high contention, consider optimistic locking with retry as a future improvement.
- `@Transactional(isolation = SERIALIZABLE)` is NOT needed — pessimistic lock at row level is sufficient.
