---
task_id: T-020
feature: Product Catalog
status: pending
effort: L
dependencies: [T-001]
---

# URD — Product Catalog

## 1. Business Context
The product catalog is the read-heavy core of the storefront. Users browse and search products; admins manage the catalog. Redis caching reduces database load on frequent list/search calls. Prices captured here are snapshotted at order time to prevent price drift on existing orders.

## 2. User Stories
- As a **shopper**, I want to browse a paginated product list so that I can discover what's available.
- As a **shopper**, I want to search products by name or filter by category so that I can find specific items quickly.
- As a **shopper**, I want to view product details (price, description, stock) so that I can make a purchase decision.
- As an **admin**, I want to create products so that they appear in the catalog.
- As an **admin**, I want to update product details and price so that the catalog stays accurate.
- As an **admin**, I want to deactivate a product so that it's hidden from shoppers without being deleted.

## 3. Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-020-1 | `GET /api/products` returns paginated product list (active only) | Must Have |
| FR-020-2 | Supports query params: `search` (name ILIKE), `category`, `page`, `size`, `sort` | Must Have |
| FR-020-3 | `GET /api/products/{id}` returns single product detail | Must Have |
| FR-020-4 | Product list responses served from Redis cache; invalidated on admin write | Must Have |
| FR-020-5 | `POST /api/admin/products` creates a new product (ADMIN only) | Must Have |
| FR-020-6 | `PUT /api/admin/products/{id}` updates product fields | Must Have |
| FR-020-7 | `DELETE /api/admin/products/{id}` soft-deletes (sets `active=false`) | Must Have |
| FR-020-8 | Product price is always a positive decimal with 2 decimal places | Must Have |

## 4. API Endpoints
| Method | Path | Auth | Request Body | Response | Description |
|--------|------|------|--------------|----------|-------------|
| GET | /api/products | None | — | `Page<ProductResponse>` | List active products with optional search/filter |
| GET | /api/products/{id} | None | — | `ProductResponse` | Get product detail |
| POST | /api/admin/products | Bearer JWT (ADMIN) | `CreateProductRequest` | `ProductResponse` | Create product |
| PUT | /api/admin/products/{id} | Bearer JWT (ADMIN) | `UpdateProductRequest` | `ProductResponse` | Update product |
| DELETE | /api/admin/products/{id} | Bearer JWT (ADMIN) | — | `204 No Content` | Soft-delete product |

## 5. Data Entities
**Product**
- `id` (Long, PK, auto-increment)
- `name` (String, not null, max 255)
- `description` (String, nullable, TEXT)
- `price` (BigDecimal, not null, precision 10 scale 2, positive)
- `category` (String, not null, max 100)
- `imageUrl` (String, nullable)
- `active` (Boolean, not null, default true)
- `createdAt` (Instant, not null)
- `updatedAt` (Instant, not null)

**Flyway migration file:** `V3__create_products.sql` (includes index on `category` and `name`)

## 6. Business Rules
1. Only products with `active=true` are returned to non-admin callers.
2. Price must be positive (`> 0`); zero-price products are not allowed.
3. Cache key format: `products::list::{page}::{size}::{search}::{category}` — evicted on any admin write.
4. Product `id` is never reused after soft-delete.
5. Admin can view inactive products via the admin endpoint (out of scope for this task — T-090).

## 7. Implementation Layers
1. **Domain** — `Product` entity
2. **Migration** — `V3__create_products.sql`
3. **Repository** — `ProductRepository extends JpaRepository<Product, Long>` with `findByActiveTrue(Pageable)` and custom search query
4. **Service**
   - `ProductService` interface: `listProducts(Pageable, String search, String category)`, `getProduct(Long id)`, `createProduct(CreateProductRequest)`, `updateProduct(Long id, UpdateProductRequest)`, `deleteProduct(Long id)`
   - `ProductServiceImpl` — `@Cacheable` on list/get, `@CacheEvict` on write
5. **Controller**
   - `ProductController` — `/api/products/**` (public)
   - `AdminProductController` — `/api/admin/products/**` (ADMIN only)
6. **Config** — ensure `RedisConfig` has a `CacheManager` with TTL (e.g., 10 minutes)
7. **Tests**
   - `ProductServiceImplTest` — list pagination, search filtering, not-found exception, cache eviction logic
   - `ProductControllerTest` — GET list 200, GET single 200/404, pagination params
   - `AdminProductControllerTest` — POST 201, PUT 200, DELETE 204, 403 for non-admin

## 8. Acceptance Criteria
- [ ] `GET /api/products` returns 200 with paginated results (active products only)
- [ ] `GET /api/products?search=shirt&category=apparel` filters correctly
- [ ] `GET /api/products/{id}` for unknown id returns `404`
- [ ] Second identical `GET /api/products` request is served from Redis (no DB query — verify via logs)
- [ ] `POST /api/admin/products` with valid body returns `201` and product payload
- [ ] `POST /api/admin/products` without ADMIN JWT returns `403`
- [ ] `PUT /api/admin/products/{id}` updates and invalidates cache
- [ ] `DELETE /api/admin/products/{id}` sets `active=false` — product disappears from public list
- [ ] Price `0` or negative returns `400` validation error
- [ ] All product tests pass

## 9. Security Checklist
- [ ] Admin endpoints protected with `@PreAuthorize("hasRole('ADMIN')")`
- [ ] `price` validated as positive decimal with Bean Validation (`@DecimalMin("0.01")`)
- [ ] `name` and `description` treated as user-controlled strings — no dynamic SQL
- [ ] Soft delete prevents data loss while removing product from public view

## 10. Non-Functional Notes
- Redis cache TTL: 10 minutes. Evicted immediately on any admin write operation.
- Pagination default: `page=0, size=20, sort=createdAt,desc`.
- Full-text search not required — `ILIKE %search%` on `name` is sufficient for demo scale.
- `@Cacheable` key must include all query params to prevent stale cross-query responses.
