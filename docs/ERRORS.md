# Error Log — E-commerce Order Management API

> Running log of bugs encountered and fixed. Each entry is a reusable fix recipe.
> Format: symptom → root cause → fix → prevention

---

## ERR-011 — PostgreSQL Rejects Hibernate VARCHAR for Custom Enum Column `order_status`

**Date:** 2026-05-13
**Symptom:** `PSQLException: ERROR: column "status" is of type order_status but expression is of type character varying` when calling `POST /api/orders`.
**Root Cause:** Migration `V6__create_orders.sql` created the `status` column as a PostgreSQL custom enum type (`CREATE TYPE order_status AS ENUM (...)`). The `Order` entity mapped the field with `@Enumerated(EnumType.STRING)` and `@Column(columnDefinition = "VARCHAR(50)")`. Hibernate 6 sends enum values as `VARCHAR` parameters; PostgreSQL rejects them because the column type is `order_status`, not `character varying` — PostgreSQL does not auto-cast between the two.
**Fix:** Created migration `V10__fix_order_status_varchar.sql` with `ALTER TABLE orders ALTER COLUMN status TYPE VARCHAR(50) USING status::VARCHAR` to convert the column from the custom enum type to plain `VARCHAR(50)`, matching what Hibernate sends.
**Affected Files:**
- `src/main/resources/db/migration/V10__fix_order_status_varchar.sql` — new migration to change column type

**Prevention:** When using `@Enumerated(EnumType.STRING)` in Hibernate, define the DB column as `VARCHAR`, not a PostgreSQL custom enum type. If a custom enum type is needed, use `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` on the entity field instead of `@Enumerated(EnumType.STRING)`.

---

## ERR-010 — `PropertyReferenceException` on Standard `findAll(pageable)` from Swagger `sort=["string"]`

**Date:** 2026-05-13
**Symptom:** `PropertyReferenceException: No property '["string"]' found for type 'User'` (and `Order`) when calling `GET /api/admin/users` or `GET /api/orders` from Swagger UI with default sort parameter.
**Root Cause:** Swagger UI's "Try it out" sends `sort=["string"]` as the default placeholder for array-type query parameters. Standard Spring Data JPA `findAll(pageable)` resolves sort properties via `PropertyPath` — when `["string"]` cannot be matched to any field on the entity, it throws `PropertyReferenceException`. This is distinct from ERR-007 (which fails at the HQL transformer for custom `@Query` methods); here the failure happens at JPA's property resolver before any SQL is generated.
**Fix:** Added `sanitizeSort(Pageable)` private method to `UserServiceImpl` and `OrderServiceImpl` (same pattern as `ProductServiceImpl` in ERR-007). The method validates each sort property against a whitelist of valid field names; invalid sort falls back to `Sort.by("id")`.
**Affected Files:**
- `src/main/java/com/example/eom/service/impl/UserServiceImpl.java` — added `SORTABLE_FIELDS`, `sanitizeSort()`, wrapped `findAll(pageable)` call
- `src/main/java/com/example/eom/service/impl/OrderServiceImpl.java` — added `SORTABLE_FIELDS`, `sanitizeSort()`, wrapped both `listByUser` and `adminList` pageable calls

**Prevention:** Every service method that accepts `Pageable` and passes it to a repository must call `sanitizeSort()` before the repository call. Apply this pattern at the time the pageable endpoint is created, not after the error appears in production.

---

## ERR-001 — GitHub MCP Authentication Failure

**Date:** 2026-05-12
**Symptom:** `MCP error -32603: Authentication Failed: Bad credentials` when calling any `mcp__github__*` tool.
**Root Cause:** GitHub token not configured or expired in MCP settings — the MCP server had no valid `GITHUB_TOKEN`.
**Fix:** Installed `gh` CLI via Homebrew (`brew install gh`) and authenticated with `gh auth login`, which stored credentials for the `gh` CLI to use instead of MCP.
**Affected Files:** *(none — configuration issue only)*
**Prevention:** Before running any skill that creates GitHub issues (`/task-breakdown`, `/feat-do`), verify connectivity with `gh repo view --json nameWithOwner`. If MCP GitHub fails, fall back to `gh` CLI.

---

## ERR-002 — `gh` CLI Not Installed

**Date:** 2026-05-12
**Symptom:** `zsh: command not found: gh` when running `gh repo view --json nameWithOwner` in task-breakdown skill.
**Root Cause:** GitHub CLI (`gh`) was not installed on the machine.
**Fix:** Ran `brew install gh && gh auth login` (chose `GitHub.com → HTTPS → Login with a web browser`).
**Affected Files:** *(none — tooling setup)*
**Prevention:** Add `gh` CLI to project prerequisites in README. Document: `brew install gh && gh auth login`.

---

## ERR-003 — Spring Security Returns 403 Instead of 401 for Unauthenticated Requests

**Date:** 2026-05-12
**Symptom:** Test `AuthControllerTest.getMe_noJwt_returns401` failed — expected HTTP 403 but received 403. GET `/api/users/me` without JWT returned 403 Forbidden instead of 401 Unauthorized.
**Root Cause:** No `AuthenticationEntryPoint` was configured in `SecurityConfig`. Spring Security's default behavior for unauthenticated requests to protected endpoints is to return 403, not 401, when there is no entry point override.
**Fix:** Added a custom `AuthenticationEntryPoint` lambda to the `.exceptionHandling()` block in `SecurityConfig` that writes a JSON `ErrorResponseDTO` with status 401.
**Affected Files:**
- `src/main/java/com/example/eom/config/SecurityConfig.java` — added `.exceptionHandling(ex -> ex.authenticationEntryPoint(...))`, injected `ObjectMapper`

**Prevention:** For any stateless REST API using Spring Security, always configure `authenticationEntryPoint` to return JSON 401. Without it, Spring Security sends 403 for all unauthenticated access, which is semantically wrong for REST.

---

## ERR-004 — `NoResourceFoundException` Mapped to 500 Instead of 404

**Date:** 2026-05-12
**Symptom:** Accessing a non-existent path under `/swagger-ui/**` (e.g., `/swagger-ui/v3/api-docs`) returned `{"status":500,"error":"Internal Server Error",...}` instead of 404.
**Root Cause:** Spring Framework 6 (Spring Boot 3.x) introduced `org.springframework.web.servlet.resource.NoResourceFoundException` for missing static resources. The catch-all `@ExceptionHandler(Exception.class)` in `GlobalExceptionHandler` was catching it and returning 500.
**Fix:** Added an explicit `@ExceptionHandler(NoResourceFoundException.class)` handler in `GlobalExceptionHandler` that returns `ResponseEntity` with HTTP 404.
**Affected Files:**
- `src/main/java/com/example/eom/config/GlobalExceptionHandler.java` — added `handleNoResource(NoResourceFoundException ex)` method

**Prevention:** In every Spring Boot 3.x project, always add a `NoResourceFoundException` handler to `GlobalExceptionHandler`. It does not exist in Spring Boot 2.x so it is a common migration gap.

---

## ERR-005 — Exception Catch-All Swallowing Stack Traces Silently

**Date:** 2026-05-12
**Symptom:** Server returned `{"status":500,"error":"Internal Server Error","message":"An unexpected error occurred"}` with no corresponding log output — impossible to diagnose root cause without guessing.
**Root Cause:** The catch-all `@ExceptionHandler(Exception.class)` in `GlobalExceptionHandler` was returning a response without logging the exception object, silently discarding the stack trace.
**Fix:** Added `@Slf4j` to `GlobalExceptionHandler` and added `log.error("Unhandled exception: {}", ex.getMessage(), ex)` as the first line of `handleGeneral()`.
**Affected Files:**
- `src/main/java/com/example/eom/config/GlobalExceptionHandler.java` — added `@Slf4j`, added `log.error(...)` call

**Prevention:** Every catch-all exception handler must log the full exception (`ex`, not just `ex.getMessage()`). Silent 500s make debugging exponentially harder.

---

## ERR-007 — Swagger UI Default `sort=["string"]` Crashes Spring Data JPA Custom Query

**Date:** 2026-05-13
**Symptom:** `InvalidDataAccessApiUsageException: Sort expression '["string"]: ASC' must only contain property references or aliases used in the select clause` when calling `GET /api/products` from Swagger UI.
**Root Cause:** Spring Data JPA 3.x validates sort property names against the entity for custom `@Query` methods. Swagger UI's "Try it out" sends `sort=["string"]` as the default placeholder for array-type query parameters. `["string"]` is not a valid `Product` field name, triggering the validation error.
**Fix:** Added `sanitizeSort(Pageable)` private method in `ProductServiceImpl` that checks every sort property against a whitelist of valid `Product` field names (`id`, `name`, `price`, `category`, `createdAt`, `updatedAt`). If any invalid property is found, the entire sort is replaced with `Sort.by("id")`.
**Affected Files:**
- `src/main/java/com/example/eom/service/impl/ProductServiceImpl.java` — added `SORTABLE_FIELDS` constant and `sanitizeSort()` method; `listProducts()` now passes `sanitizeSort(pageable)` to the repository

**Prevention:** Any `@Query` pageable endpoint must sanitize the sort parameter — Swagger UI always sends `["string"]` as the default sort value in "Try it out" mode.

---

## ERR-008 — `lower(bytea) does not exist` with Null String Params in Hibernate 6 + PostgreSQL

**Date:** 2026-05-13
**Symptom:** `InvalidDataAccessResourceUsageException: JDBC exception... ERROR: function lower(bytea) does not exist` when calling `GET /api/products` with no `keyword`/`category` params.
**Root Cause:** Hibernate 6 sends null String parameters to PostgreSQL as untyped `?` bindings. PostgreSQL infers the type as `bytea` (binary) when no other type context is available. The JPQL query used `:keyword IS NULL OR LOWER(...) LIKE LOWER(CONCAT('%', :keyword, '%'))` — both the null check and the LIKE expression shared the same untyped binding. `CAST(:keyword AS String) IS NULL` fixed the null check but the second `?` in `LOWER(CONCAT('%', ?, '%'))` remained untyped, still causing `lower(bytea)`.
**Fix:** Eliminated null String params from the query entirely. In `ProductServiceImpl.listProducts()`, converted `null` to `""` before passing to the repository (`keyword != null ? keyword : ""`). Changed the JPQL condition from `:keyword IS NULL OR ...` to `:keyword = '' OR ...` — PostgreSQL always infers `''` as text, so the type issue disappears.
**Affected Files:**
- `src/main/java/com/example/eom/repository/ProductRepository.java` — changed `IS NULL` to `= ''` for `keyword` and `category` params
- `src/main/java/com/example/eom/service/impl/ProductServiceImpl.java` — added null-to-empty-string conversion before repository call

**Prevention:** Never pass null String parameters to JPQL queries on PostgreSQL with Hibernate 6. Always convert null → `""` in the service layer and use `= ''` checks in the query instead of `IS NULL`.

---

## ERR-009 — `@ConditionalOnBean` Bean Ordering Causes Redis JSON Serializer to Be Skipped

**Date:** 2026-05-13
**Symptom:** `SerializationException: Cannot serialize` → `NotSerializableException: com.example.eom.dto.product.ProductResponse` — Redis cache used JDK serialization even though `RedisConfig` explicitly configured `GenericJackson2JsonRedisSerializer`.
**Root Cause:** `@ConditionalOnBean(RedisConnectionFactory.class)` on the custom `cacheManager()` bean caused a Spring bean-ordering race: when `RedisConfig` was processed during context startup, `RedisConnectionFactory` (created by Spring Boot autoconfiguration) was not yet registered in the bean factory. The condition evaluated to `false`, the custom `CacheManager` was silently skipped, and Spring Boot's default JDK-serialization `CacheManager` was used instead.
**Fix:** Removed `@ConditionalOnBean(RedisConnectionFactory.class)`. Spring resolves `RedisConnectionFactory` as a constructor parameter for `cacheManager()`, which guarantees correct initialization order automatically.
**Affected Files:**
- `src/main/java/com/example/eom/config/RedisConfig.java` — removed `@ConditionalOnBean` annotation and its import from `cacheManager()` method

**Prevention:** Do not use `@ConditionalOnBean` to guard beans that depend on Spring Boot autoconfigured infrastructure beans (like `RedisConnectionFactory`, `DataSource`). Inject them directly as method parameters — Spring's dependency resolution handles ordering correctly without the conditional.

---

## ERR-006 — SpringDoc 2.3.0 Incompatible with Spring Boot 3.4.1 (Spring 6.2.x)

**Date:** 2026-05-12
**Symptom:** GET `/v3/api-docs` returned HTTP 500. Swagger UI showed "Unable to render this definition — does not specify a valid version field". Stack trace: `NoSuchMethodError: 'void org.springframework.web.method.ControllerAdviceBean.<init>(java.lang.Object)'` at `springdoc-openapi-starter-common-2.3.0.jar`.
**Root Cause:** SpringDoc 2.3.0 (Feb 2024) internally calls `new ControllerAdviceBean(Object)` in `GenericResponseService.getGenericMapResponse()`. This constructor was removed in Spring Framework 6.2.x (shipped with Spring Boot 3.4.x). The API break causes a `NoSuchMethodError` at runtime when SpringDoc tries to generate the OpenAPI spec.
**Fix:** Updated `springdoc.version` from `2.3.0` to `2.7.0` in `pom.xml`. SpringDoc 2.7.0 is built against Spring Framework 6.2.x and uses the updated `ControllerAdviceBean` API.
**Affected Files:**
- `pom.xml` — `<springdoc.version>2.3.0</springdoc.version>` → `<springdoc.version>2.7.0</springdoc.version>`

**Prevention:** SpringDoc minor version must track Spring Boot minor version. Compatibility rule: SpringDoc 2.N.x → Spring Boot 3.N.x (approximately). Always check the [SpringDoc compatibility matrix](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot) before upgrading Spring Boot.

---
