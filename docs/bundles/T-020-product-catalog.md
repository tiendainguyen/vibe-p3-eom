---
feature: Product Catalog with Search, Filtering & Redis Cache
task: T-020
issue: #3
archived: 2026-05-12
files:
  - src/main/resources/db/migration/V3__create_products.sql
  - src/main/java/com/example/eom/domain/Product.java
  - src/main/java/com/example/eom/dto/product/ProductResponse.java
  - src/main/java/com/example/eom/dto/product/CreateProductRequest.java
  - src/main/java/com/example/eom/dto/product/UpdateProductRequest.java
  - src/main/java/com/example/eom/repository/ProductRepository.java
  - src/main/java/com/example/eom/service/ProductService.java
  - src/main/java/com/example/eom/service/impl/ProductServiceImpl.java
  - src/main/java/com/example/eom/controller/ProductController.java
  - src/main/java/com/example/eom/controller/admin/AdminProductController.java
  - src/main/java/com/example/eom/config/RedisConfig.java
  - src/test/java/com/example/eom/service/ProductServiceImplTest.java
  - src/test/java/com/example/eom/controller/ProductControllerTest.java
---

# Bundle: Product Catalog with Search, Filtering & Redis Cache

## API Surface
| Method | Path | Auth | Handler |
|--------|------|------|---------|
| GET | /api/products | public | ProductController.listProducts() |
| GET | /api/products/{id} | public | ProductController.getById() |
| POST | /api/admin/products | ADMIN JWT | AdminProductController.create() |
| PATCH | /api/admin/products/{id} | ADMIN JWT | AdminProductController.update() |
| DELETE | /api/admin/products/{id} | ADMIN JWT | AdminProductController.delete() |

## Logic Map

### ProductServiceImpl (file: src/main/java/com/example/eom/service/impl/ProductServiceImpl.java)
- `listProducts(keyword, category, minPrice, maxPrice, pageable)`: delegates to `findAllWithFilters`, cached with SpEL key `{#keyword,#category,#minPrice,#maxPrice,#pageable}` — returns Page<ProductResponse>
- `getById(id)`: fetches by ID, filters `.filter(Product::isActive)` — throws EntityNotFoundException if absent or inactive
- `create(request)`: builds entity from DTO fields, saves, evicts all cache entries
- `update(id, request)`: loads entity, applies only non-null fields from request, saves, evicts all cache entries
- `delete(id)`: soft delete — sets `active = false` and saves, evicts all cache entries

### ProductRepository (file: src/main/java/com/example/eom/repository/ProductRepository.java)
- `findAllWithFilters(keyword, category, minPrice, maxPrice, pageable)`: JPQL with `p.active = true` always on; each filter param uses `IS NULL OR` guard so null = no filter; keyword searches LOWER(name) and LOWER(description) via LIKE

### RedisConfig (file: src/main/java/com/example/eom/config/RedisConfig.java)
- `cacheManager(factory)`: `@ConditionalOnBean(RedisConnectionFactory.class)` — skipped in test profile; builds `RedisCacheManager` with 10-min TTL, `StringRedisSerializer` for keys, `GenericJackson2JsonRedisSerializer` (with `NON_FINAL` default typing) for values

## Business Rules
1. Only `active = true` products are returned by list and getById — inactive products yield 404
2. Soft delete: DELETE sets `active = false`, never removes the row
3. Price must be > 0 enforced at both DB level (`CHECK (price > 0)`) and DTO validation (`@Positive`)
4. Update is partial: null fields in `UpdateProductRequest` are ignored, existing values kept
5. Any write (create/update/delete) evicts the entire `products` cache (`allEntries = true`)
6. Cache TTL is 10 minutes in production; disabled (`spring.cache.type=none`) in test profile

## Key Decisions
- `allEntries = true` cache eviction on writes: avoids stale cache across all filter combinations at the cost of a full cache miss on next read
- `@ConditionalOnBean(RedisConnectionFactory.class)` on cacheManager: prevents context failure when Redis is excluded in test profile
- Soft delete over hard delete: preserves order history references (future order items will reference product IDs)
- SpEL key includes `#pageable`: ensures page/sort variations get separate cache entries

## Exception Handling
- `EntityNotFoundException` → 404 — `GlobalExceptionHandler.handleEntityNotFound()`
- `MethodArgumentNotValidException` → 400 — `GlobalExceptionHandler.handleValidation()`
- `AccessDeniedException` → 403 — Spring Security default (non-ADMIN hitting /api/admin/**)
- Unauthenticated on `/api/admin/**` → 401 — custom `AuthenticationEntryPoint` in `SecurityConfig`

## Tests
- `ProductServiceImplTest`: service unit tests — listProducts, getById (active/inactive/missing), create, update (partial/missing), delete (soft/missing) — 9 cases
- `ProductControllerTest`: WebMvcTest — public list/getById, 401 no-auth on admin, 403 USER role on admin, ADMIN create/update/delete happy path + validation + 404 — 11 cases

## Files Index
**Domain:** src/main/java/com/example/eom/domain/Product.java
**Repository:** src/main/java/com/example/eom/repository/ProductRepository.java
**Service:** src/main/java/com/example/eom/service/ProductService.java, src/main/java/com/example/eom/service/impl/ProductServiceImpl.java
**Controller:** src/main/java/com/example/eom/controller/ProductController.java, src/main/java/com/example/eom/controller/admin/AdminProductController.java
**DTO:** src/main/java/com/example/eom/dto/product/ProductResponse.java, src/main/java/com/example/eom/dto/product/CreateProductRequest.java, src/main/java/com/example/eom/dto/product/UpdateProductRequest.java
**Config:** src/main/java/com/example/eom/config/RedisConfig.java
**Migration:** src/main/resources/db/migration/V3__create_products.sql
**Tests:** src/test/java/com/example/eom/service/ProductServiceImplTest.java, src/test/java/com/example/eom/controller/ProductControllerTest.java
