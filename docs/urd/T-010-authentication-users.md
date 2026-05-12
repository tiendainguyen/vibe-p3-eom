---
task_id: T-010
feature: Authentication & Users
status: pending
effort: L
dependencies: [T-001]
---

# URD — Authentication & Users

## 1. Business Context
Every other feature requires knowing who the caller is. This task implements user registration, login, and JWT issuance so that downstream features can trust the identity extracted from the token. Role-based access (USER vs ADMIN) is also established here to support admin-only endpoints in later tasks.

## 2. User Stories
- As a **visitor**, I want to register with email and password so that I can create an account.
- As a **registered user**, I want to log in and receive a JWT so that I can call protected endpoints.
- As a **user**, I want my password stored securely (hashed) so that a data breach doesn't expose my credentials.
- As an **admin**, I want a seeded admin account on startup so that admin endpoints are accessible from day one.
- As a **developer**, I want the `userId` extracted from the JWT principal so that features never trust user-supplied IDs.

## 3. Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-010-1 | `POST /api/auth/register` creates a new user with hashed password | Must Have |
| FR-010-2 | Duplicate email registration returns `409 Conflict` | Must Have |
| FR-010-3 | `POST /api/auth/login` returns a signed JWT on valid credentials | Must Have |
| FR-010-4 | Invalid credentials return `401 Unauthorized` | Must Have |
| FR-010-5 | JWT payload contains `sub` (userId), `email`, `role`, `iat`, `exp` | Must Have |
| FR-010-6 | JWT expiry is configurable via `app.jwt.expiration-ms` env var | Should Have |
| FR-010-7 | `GET /api/users/me` returns the authenticated user's profile | Must Have |
| FR-010-8 | Admin role seeded via `DataInitializer` on startup if no admin exists | Must Have |

## 4. API Endpoints
| Method | Path | Auth | Request Body | Response | Description |
|--------|------|------|--------------|----------|-------------|
| POST | /api/auth/register | None | `RegisterRequest` | `AuthResponse` | Register new user |
| POST | /api/auth/login | None | `LoginRequest` | `AuthResponse` | Login, receive JWT |
| GET | /api/users/me | Bearer JWT | — | `UserResponse` | Get own profile |

## 5. Data Entities
**User**
- `id` (Long, PK, auto-increment)
- `email` (String, unique, not null)
- `passwordHash` (String, not null)
- `role` (Enum: USER / ADMIN, not null, default USER)
- `createdAt` (Instant, not null)

**Flyway migration file:** `V2__create_users.sql`

## 6. Business Rules
1. Passwords must be at least 8 characters long (validated at API layer).
2. Passwords are hashed with BCrypt (strength 12) before storage.
3. Plain-text passwords are NEVER logged or returned in any response.
4. JWT secret is read from `${JWT_SECRET}` environment variable — never hardcoded.
5. `userId` is always extracted from `Authentication.getPrincipal()` — never from request body.
6. Only one ADMIN role account is seeded; the seeder is idempotent (no duplicate admin on restart).

## 7. Implementation Layers
1. **Domain** — `User` entity, `Role` enum (USER, ADMIN)
2. **Migration** — `V2__create_users.sql`
3. **Repository** — `UserRepository extends JpaRepository<User, Long>` with `findByEmail(String email)`
4. **Service**
   - `AuthService` interface: `register(RegisterRequest)`, `login(LoginRequest)`
   - `UserService` interface: `getCurrentUser(Long userId)`
   - `AuthServiceImpl`, `UserServiceImpl`
   - `CustomUserDetailsService implements UserDetailsService`
   - `DataInitializer implements ApplicationRunner` — seeds admin
5. **Controller**
   - `AuthController` — `/api/auth/**`
   - `UserController` — `/api/users/me`
6. **Config** — `JwtUtil` updated with claims from User entity; `PasswordEncoder` bean (BCrypt)
7. **Tests**
   - `AuthServiceImplTest` — register happy path, duplicate email, login valid/invalid
   - `AuthControllerTest` — POST /register 201, POST /login 200/401, GET /me 200/401

## 8. Acceptance Criteria
- [ ] `POST /api/auth/register` with valid body returns `201` and a JWT
- [ ] `POST /api/auth/register` with existing email returns `409`
- [ ] `POST /api/auth/register` with password < 8 chars returns `400`
- [ ] `POST /api/auth/login` with correct credentials returns `200` and a valid JWT
- [ ] `POST /api/auth/login` with wrong password returns `401`
- [ ] `GET /api/users/me` with valid JWT returns user profile (no `passwordHash` field)
- [ ] `GET /api/users/me` without JWT returns `401`
- [ ] User table has BCrypt hash in `password_hash` column (not plain text)
- [ ] All `AuthServiceImplTest` and `AuthControllerTest` tests pass

## 9. Security Checklist
- [ ] Passwords hashed with BCrypt before persistence
- [ ] `passwordHash` never serialized into any DTO or response
- [ ] JWT secret loaded from `${JWT_SECRET}` env var — not in application.yml
- [ ] Login failure returns generic message (no "user not found" vs "wrong password" distinction)
- [ ] Token expiry validated on every protected request by `JwtAuthFilter`
- [ ] `userId` extracted from JWT principal only — never trusted from request body

## 10. Non-Functional Notes
- BCrypt strength 12 adds ~250ms per hash — acceptable for auth endpoints, not for hot paths.
- JWT expiry default: 24 hours. Configurable via `app.jwt.expiration-ms`.
- No refresh token in scope for this project — stateless JWTs only.
