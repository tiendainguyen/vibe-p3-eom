---
feature: User Authentication (JWT)
task: T-010
issue: #2
archived: 2026-05-12
files:
  - src/main/java/com/example/eom/domain/User.java
  - src/main/java/com/example/eom/domain/enums/Role.java
  - src/main/java/com/example/eom/repository/UserRepository.java
  - src/main/java/com/example/eom/service/AuthService.java
  - src/main/java/com/example/eom/service/UserService.java
  - src/main/java/com/example/eom/service/impl/AuthServiceImpl.java
  - src/main/java/com/example/eom/service/impl/UserServiceImpl.java
  - src/main/java/com/example/eom/service/impl/CustomUserDetailsService.java
  - src/main/java/com/example/eom/service/impl/DataInitializer.java
  - src/main/java/com/example/eom/controller/AuthController.java
  - src/main/java/com/example/eom/controller/UserController.java
  - src/main/java/com/example/eom/dto/auth/RegisterRequest.java
  - src/main/java/com/example/eom/dto/auth/LoginRequest.java
  - src/main/java/com/example/eom/dto/auth/AuthResponse.java
  - src/main/java/com/example/eom/dto/user/UserResponse.java
  - src/main/resources/db/migration/V2__create_users.sql
  - src/test/java/com/example/eom/controller/AuthControllerTest.java
  - src/test/java/com/example/eom/service/AuthServiceImplTest.java
---

# Bundle: User Authentication (JWT)

## API Surface
| Method | Path | Auth | Handler |
|--------|------|------|---------|
| POST | /api/auth/register | public | `AuthController.register()` |
| POST | /api/auth/login | public | `AuthController.login()` |
| GET | /api/users/me | JWT | `UserController.getMe()` |

## Logic Map

### AuthServiceImpl (file: src/main/java/com/example/eom/service/impl/AuthServiceImpl.java)
- `register(RegisterRequest)`: checks email uniqueness (throws `IllegalStateException` if dup), hashes password via `PasswordEncoder`, saves `User` with role `USER`, generates JWT → returns `AuthResponse`
- `login(LoginRequest)`: delegates credential check to `AuthenticationManager.authenticate()` (throws `BadCredentialsException` on failure), loads user by email, generates JWT → returns `AuthResponse`

### UserServiceImpl (file: src/main/java/com/example/eom/service/impl/UserServiceImpl.java)
- `getCurrentUser(Long userId)`: loads user by id (throws `EntityNotFoundException` if not found), maps to `UserResponse` record (id, email, role name, createdAt)

### CustomUserDetailsService (file: src/main/java/com/example/eom/service/impl/CustomUserDetailsService.java)
- `loadUserByUsername(String email)`: finds user by email (throws `UsernameNotFoundException` if absent), returns Spring `User` with `passwordHash` and `ROLE_<role>` authority — used by `DaoAuthenticationProvider` at login

### DataInitializer (file: src/main/java/com/example/eom/service/impl/DataInitializer.java)
- `run(ApplicationArguments)`: on startup, checks if any `ADMIN` role user exists via `userRepository.existsByRole(ADMIN)`; if not, creates one using `app.admin.email` / `app.admin.password` env vars (defaults: `admin@example.com` / `Admin@1234`)

### AuthController (file: src/main/java/com/example/eom/controller/AuthController.java)
- `register(@Valid RegisterRequest)`: delegates to `authService.register()`, returns 201 + `AuthResponse`
- `login(@Valid LoginRequest)`: delegates to `authService.login()`, returns 200 + `AuthResponse`

### UserController (file: src/main/java/com/example/eom/controller/UserController.java)
- `getMe(Authentication)`: extracts `userId = Long.parseLong(authentication.getPrincipal())` from JWT principal, delegates to `userService.getCurrentUser(userId)`, returns `UserResponse`

## Business Rules
1. Email must be unique — `register()` checks `existsByEmail()` before saving; duplicate throws `IllegalStateException` → 409
2. Passwords stored as BCrypt hash (strength 12) — plain text never persisted or returned
3. `userId` always extracted from JWT principal — never accepted from request body
4. Role defaults to `USER` on registration — `ADMIN` role only seeded via `DataInitializer` at startup
5. `AuthResponse` never includes `passwordHash` — only token, userId, email, role name
6. `LoginRequest` validates `@NotBlank` for email and password — missing fields return 400
7. `RegisterRequest` validates `@Email`, `@NotBlank` for email and `@Size(min=8)` for password

## Key Decisions
- `AuthenticationManager` used in `AuthServiceImpl.login()` (not manual password check): delegates to `DaoAuthenticationProvider` → `CustomUserDetailsService` → BCrypt verify, consistent with Spring Security contract
- `userId` stored as JWT `subject` (not email): stable identifier even if email changes in future
- `DataInitializer implements ApplicationRunner`: admin seed runs after full context is ready, including DB migrations
- `Role` enum in JWT claims as `.name()` string: avoids serialization issues with enum ordinals across versions
- `UserController` casts principal to `String` then `Long.parseLong()`: principal is set as `userId.toString()` in `JwtAuthFilter`

## Exception Handling
- `IllegalStateException("Email already registered")` → 409 "Conflict" — `GlobalExceptionHandler.handleConflict()`
- `BadCredentialsException` → 401 "Unauthorized" (opaque message) — `GlobalExceptionHandler.handleBadCredentials()`
- `EntityNotFoundException("User not found")` → 404 "Not Found" — `GlobalExceptionHandler.handleNotFound()`
- `UsernameNotFoundException` → caught by Spring Security, surfaced as `BadCredentialsException` → 401
- `MethodArgumentNotValidException` → 400 "Validation Failed" — `GlobalExceptionHandler.handleValidation()`

## Tests
- `AuthServiceImplTest` (4 tests): pure Mockito unit tests — register success (token returned, user saved), register duplicate email (throws IllegalStateException), login valid credentials (token returned, AuthenticationManager called), login bad credentials (BadCredentialsException propagated)
- `AuthControllerTest` (8 tests): `@WebMvcTest` + mocked `AuthService`/`UserService`/`JwtUtil` — register 201, register short password 400, register invalid email 400, register duplicate 409, login 200 with token, login invalid credentials 401, GET /me without JWT 401, GET /me with valid JWT 200

## Files Index
**Domain:** `src/main/java/com/example/eom/domain/User.java`, `src/main/java/com/example/eom/domain/enums/Role.java`
**Repository:** `src/main/java/com/example/eom/repository/UserRepository.java`
**Service:** `src/main/java/com/example/eom/service/AuthService.java`, `src/main/java/com/example/eom/service/UserService.java`, `src/main/java/com/example/eom/service/impl/AuthServiceImpl.java`, `src/main/java/com/example/eom/service/impl/UserServiceImpl.java`, `src/main/java/com/example/eom/service/impl/CustomUserDetailsService.java`, `src/main/java/com/example/eom/service/impl/DataInitializer.java`
**Controller:** `src/main/java/com/example/eom/controller/AuthController.java`, `src/main/java/com/example/eom/controller/UserController.java`
**DTO:** `src/main/java/com/example/eom/dto/auth/RegisterRequest.java`, `src/main/java/com/example/eom/dto/auth/LoginRequest.java`, `src/main/java/com/example/eom/dto/auth/AuthResponse.java`, `src/main/java/com/example/eom/dto/user/UserResponse.java`
**Migration:** `src/main/resources/db/migration/V2__create_users.sql`
**Tests:** `src/test/java/com/example/eom/controller/AuthControllerTest.java`, `src/test/java/com/example/eom/service/AuthServiceImplTest.java`
