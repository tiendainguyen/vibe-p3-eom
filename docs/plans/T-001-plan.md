---
task_id: T-001
feature: Infrastructure Setup
status: ready
dependencies: []
---

# Implementation Plan — T-001 Infrastructure Setup

## 3.1 Overview

| Field | Value |
|-------|-------|
| Task ID | T-001 |
| Feature | Infrastructure Setup |
| Layer | config / security |
| Effort | L |
| Dependencies | none |

Most infrastructure is already scaffolded by `/initial-project`. This task completes the remaining pieces: JWT utility + filter and the exception handler test. Everything else just needs verification.

---

## 3.2 Prerequisites Check

- [x] No dependency tasks — T-001 is the root
- [x] `V1__baseline.sql` exists (empty baseline)
- [x] `spring-dotenv` added to `pom.xml` (done in previous session)
- [x] `docker-compose.yml` created with Postgres, Redis, RabbitMQ
- [ ] `.env` created from `.env.example` with real values
- [ ] Docker services running (`docker compose up -d`)

> **Port note:** `docker-compose.yml` uses non-default host ports:
> - Postgres → `5433` (container 5432)
> - Redis → `6380` (container 6379)
>
> `.env` must reflect this:
> ```
> DB_URL=jdbc:postgresql://localhost:5433/eom
> REDIS_HOST=localhost
> REDIS_PORT=6380
> ```

---

## 3.3 Files to Create / Modify

```
ALREADY COMPLETE — no changes needed:
  src/main/java/com/example/eom/Application.java
  src/main/java/com/example/eom/config/GlobalExceptionHandler.java
  src/main/java/com/example/eom/config/OpenApiConfig.java
  src/main/java/com/example/eom/config/RabbitMQConfig.java       (deferred to T-070)
  src/main/java/com/example/eom/config/RedisConfig.java           (deferred to T-020)
  src/main/java/com/example/eom/dto/ErrorResponseDTO.java
  src/main/resources/application.yml
  src/main/resources/db/migration/V1__baseline.sql
  src/test/java/com/example/eom/ApplicationTests.java
  src/test/resources/application-test.yml

CREATE:
  src/main/java/com/example/eom/config/JwtUtil.java
    Purpose: JWT token generation and validation (stateless, HS256, configurable secret/expiry)

  src/main/java/com/example/eom/config/JwtAuthFilter.java
    Purpose: OncePerRequestFilter — extracts Bearer token, validates, sets SecurityContext

  src/test/java/com/example/eom/config/GlobalExceptionHandlerTest.java
    Purpose: Unit tests verifying each handler method returns correct status + error shape

MODIFY:
  src/main/java/com/example/eom/config/SecurityConfig.java
    Purpose: Inject JwtAuthFilter and add it before UsernamePasswordAuthenticationFilter
```

---

## 3.4 Implementation Steps

### Step 1 — Create `JwtUtil`
**File:** `src/main/java/com/example/eom/config/JwtUtil.java`

- Annotate `@Component`
- Inject `@Value("${jwt.secret}")` and `@Value("${jwt.expiration-ms}")`
- `generateToken(String subject)` → signs with `Keys.hmacShaKeyFor(secret.getBytes())`, algorithm HS256, sets expiration
- `extractSubject(String token)` → parses and returns `sub` claim
- `isTokenValid(String token)` → returns `false` on `JwtException` or expiry (no exceptions propagated)
- Key must be ≥ 256 bits — validated at startup via `@PostConstruct` check (throw `IllegalStateException` if shorter)

### Step 2 — Create `JwtAuthFilter`
**File:** `src/main/java/com/example/eom/config/JwtAuthFilter.java`

- Extend `OncePerRequestFilter`, annotate `@Component`
- Inject `JwtUtil`
- In `doFilterInternal`:
  1. Read `Authorization` header — skip filter if missing or not `Bearer `
  2. Extract token, call `jwtUtil.isTokenValid(token)`
  3. If valid: call `jwtUtil.extractSubject(token)`, build `UsernamePasswordAuthenticationToken` with empty authorities, set as `SecurityContextHolder` principal
  4. If invalid: do nothing (downstream `anyRequest().authenticated()` will reject with 401)
  5. Always call `filterChain.doFilter(request, response)`

### Step 3 — Update `SecurityConfig`
**File:** `src/main/java/com/example/eom/config/SecurityConfig.java`

- Inject `JwtAuthFilter` via constructor
- Add `.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)` to the filter chain
- Add `@Bean AuthenticationManager` for T-010 compatibility (accepts `AuthenticationConfiguration`)
- Remove the comment "JWT filter added in T-010"

### Step 4 — Create `GlobalExceptionHandlerTest`
**File:** `src/test/java/com/example/eom/config/GlobalExceptionHandlerTest.java`

- Plain unit test — instantiate `GlobalExceptionHandler` directly (no Spring context needed)
- Test cases:
  - `handleValidation` → 400, error = "Validation Failed", message contains field errors
  - `handleNotFound` → 404, error = "Not Found"
  - `handleConflict` → 409, error = "Conflict"
  - `handleBadRequest` → 400, error = "Bad Request"
  - `handleGeneral` → 500, error = "Internal Server Error", message = "An unexpected error occurred" (no leak)

---

## 3.5 Security Considerations

- [x] JWT secret injected via `${jwt.secret}` — never hardcoded
- [x] Secret length validated at startup (`@PostConstruct`)
- [x] CSRF disabled with documented reason (stateless REST API)
- [x] `/actuator` restricted — only `/health` in PUBLIC_PATHS
- [x] General exception handler returns opaque message — no stack traces
- [x] `SecurityContext` cleared per-request by `OncePerRequestFilter` lifecycle

---

## 3.6 Testing Strategy

| Test | Type | File |
|------|------|------|
| Context loads without errors | Integration | `ApplicationTests.java` (exists) |
| Each exception handler returns correct HTTP status and body shape | Unit | `GlobalExceptionHandlerTest.java` |

No controller or repository tests needed at this layer — there are no domain endpoints in T-001.

---

## 3.7 Validation Checklist

- [ ] `./mvnw spring-boot:run` starts — logs "Started Application"
- [ ] `GET /actuator/health` → `200 { "status": "UP" }`
- [ ] `GET /swagger-ui.html` → 200
- [ ] `GET /api/orders` (no token) → 401
- [ ] POST with invalid body → 400 `{ "error": "Validation Failed", ... }`
- [x] `./mvnw test` passes (ApplicationTests + GlobalExceptionHandlerTest)
- [x] No secrets in `application.yml`

## Implementation Notes

- `ApplicationTests.java` fixed: removed non-existent `excludeAutoConfiguration` attribute from `@SpringBootTest` — exclusions moved fully into `application-test.yml`
- Added `spring.cache.type: none` to `application-test.yml` to prevent missing `cacheManager` error when `RedisAutoConfiguration` is excluded
- Added `RedisReactiveAutoConfiguration` to test exclusion list
