---
task_id: T-001
feature: Infrastructure Setup
status: pending
effort: L
dependencies: []
---

# URD — Infrastructure Setup

## 1. Business Context
Before any business feature can be built, the project needs a working skeleton: database connectivity, migrations baseline, Redis and RabbitMQ connections, JWT security filter wiring, global error handling, and SpringDoc OpenAPI configured. This task produces no user-visible endpoints but unblocks every other task.

## 2. User Stories
- As a **developer**, I want a runnable Spring Boot app with DB connected so that I can start building features immediately.
- As a **developer**, I want Flyway baseline migration in place so that schema changes are version-controlled from day one.
- As a **developer**, I want the JWT security filter skeleton configured so that auth can be layered in cleanly.
- As a **developer**, I want global exception handling so that all features return consistent error shapes.
- As a **developer**, I want Swagger UI available so that every endpoint is explorable without Postman setup.

## 3. Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-001-1 | Application starts and connects to PostgreSQL (datasource health check passes) | Must Have |
| FR-001-2 | Flyway runs V1 baseline migration on startup without errors | Must Have |
| FR-001-3 | Redis connection established; `@Cacheable` annotation usable | Must Have |
| FR-001-4 | RabbitMQ connection established; exchange and queue declared | Must Have |
| FR-001-5 | JWT filter registered in security filter chain (permits `/api/auth/**` and `/swagger-ui/**`) | Must Have |
| FR-001-6 | `GlobalExceptionHandler` returns `{ "error": "...", "message": "..." }` for validation, not-found, and runtime errors | Must Have |
| FR-001-7 | Swagger UI accessible at `/swagger-ui.html` with OpenAPI spec at `/v3/api-docs` | Must Have |
| FR-001-8 | `application.yml` reads all secrets from environment variables (no hardcoded values) | Must Have |

## 4. API Endpoints
| Method | Path | Auth | Request Body | Response | Description |
|--------|------|------|--------------|----------|-------------|
| GET | /actuator/health | None | — | `{ "status": "UP" }` | Health check (infra only) |
| GET | /swagger-ui.html | None | — | HTML | Swagger UI |
| GET | /v3/api-docs | None | — | JSON | OpenAPI spec |

## 5. Data Entities
No domain entities in this task. Flyway baseline creates the schema only (empty tables will be added per feature).

**Flyway migration file:** `V1__baseline.sql` — creates schema version record; subsequent tasks add their tables.

## 6. Business Rules
1. All secrets (DB password, JWT secret, Redis password, RabbitMQ credentials, Stripe key) MUST come from environment variables.
2. The JWT filter must allow unauthenticated access to `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, and `/actuator/health`.
3. All other paths require a valid JWT Bearer token by default.

## 7. Implementation Layers
1. **Domain** — no entities; create `com.example.eom` package structure
2. **Migration** — `V1__baseline.sql` (empty baseline, no tables yet)
3. **Repository** — none
4. **Service** — none
5. **Controller** — none (only actuator health)
6. **Config**
   - `SecurityConfig` — JWT filter chain, permit rules
   - `JwtUtil` — token generation/validation utility
   - `JwtAuthFilter` — `OncePerRequestFilter` implementation
   - `RabbitMQConfig` — exchange, queue, binding beans
   - `RedisConfig` — `RedisTemplate` / `CacheManager` beans
   - `OpenApiConfig` — `OpenAPI` bean with title, version, JWT Bearer scheme
   - `GlobalExceptionHandler` — `@RestControllerAdvice`
7. **Tests**
   - `ApplicationContextTest` — verifies context loads without errors
   - `GlobalExceptionHandlerTest` — unit tests for error response shapes

## 8. Acceptance Criteria
- [ ] `./mvnw spring-boot:run` starts with exit code 0 and logs "Started Application"
- [ ] `GET /actuator/health` returns `200 { "status": "UP" }`
- [ ] Flyway migration log shows "Successfully applied 1 migration"
- [ ] `GET /swagger-ui.html` returns 200 HTML page
- [ ] A request to a protected endpoint without JWT returns `401`
- [ ] A malformed request body returns `400` with `{ "error": "Validation failed", "message": "..." }`
- [ ] No secrets appear in `application.yml` (only `${ENV_VAR}` placeholders)
- [ ] `ApplicationContextTest` passes

## 9. Security Checklist
- [ ] All credentials injected via `${ENV_VAR}` in `application.yml`
- [ ] JWT secret is at least 256 bits (32 characters minimum)
- [ ] CSRF disabled only because this is a stateless REST API (document the reason in `SecurityConfig`)
- [ ] `/actuator` endpoints restricted — only `/health` exposed publicly
- [ ] No stack traces leaked in error responses

## 10. Non-Functional Notes
- Redis and RabbitMQ should fail fast on startup if unreachable (set `spring.rabbitmq.connection-timeout` and `spring.data.redis.timeout`).
- Use `spring.jpa.open-in-view=false` to avoid lazy-load pitfalls.
- Flyway `out-of-order=false` to enforce migration ordering.
