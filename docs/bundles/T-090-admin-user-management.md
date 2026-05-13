---
feature: Admin User Management
task: T-090
issue: #10
archived: 2026-05-13
files:
  - src/main/resources/db/migration/V9__add_user_active_flag.sql
  - src/main/java/com/example/eom/exception/SelfDeactivationException.java
  - src/main/java/com/example/eom/dto/user/AdminUserResponse.java
  - src/main/java/com/example/eom/controller/admin/AdminUserController.java
  - src/main/java/com/example/eom/domain/User.java (active field added)
  - src/main/java/com/example/eom/service/UserService.java (listAll, activate, deactivate added)
  - src/main/java/com/example/eom/service/impl/UserServiceImpl.java (new methods implemented)
  - src/main/java/com/example/eom/service/impl/CustomUserDetailsService.java (active flag wired)
  - src/main/java/com/example/eom/config/GlobalExceptionHandler.java (DisabledException + SelfDeactivationException mapped)
  - src/test/java/com/example/eom/service/UserServiceImplTest.java
  - src/test/java/com/example/eom/controller/AdminUserControllerTest.java
---

# Bundle: Admin User Management

## API Surface
| Method | Path | Auth | Handler |
|--------|------|------|---------|
| GET | /api/admin/users | ADMIN JWT | `AdminUserController.listAll()` |
| PUT | /api/admin/users/{id}/activate | ADMIN JWT | `AdminUserController.activate()` |
| PUT | /api/admin/users/{id}/deactivate | ADMIN JWT | `AdminUserController.deactivate()` |

## Logic Map

### UserServiceImpl (file: service/impl/UserServiceImpl.java)
- `listAll(pageable)`: `userRepository.findAll(pageable)` → maps each `User` to `AdminUserResponse` (no passwordHash)
- `activate(userId)`: loads user by id (throws `EntityNotFoundException` if missing) → sets `active = true` → saves → returns `AdminUserResponse`
- `deactivate(adminId, userId)`: throws `SelfDeactivationException` if `adminId.equals(userId)` → loads user → sets `active = false` → saves → returns `AdminUserResponse`
- `toAdminResponse(user)`: private mapper → `AdminUserResponse(id, email, role, active, createdAt)`

### AdminUserController (file: controller/admin/AdminUserController.java)
- `listAll(pageable)`: delegates to `userService.listAll(pageable)` → `200 OK`
- `activate(id)`: delegates to `userService.activate(id)` → `200 OK`
- `deactivate(id, authentication)`: extracts `adminId = Long.parseLong(authentication.getPrincipal())` → delegates to `userService.deactivate(adminId, id)` → `200 OK`

### CustomUserDetailsService (file: service/impl/CustomUserDetailsService.java)
- `loadUserByUsername(email)`: loads `User` by email → builds Spring `UserDetails` using 7-arg constructor with `user.isActive()` as `enabled` flag → Spring Security's `DaoAuthenticationProvider` automatically throws `DisabledException` during `authenticate()` when `enabled = false`

### GlobalExceptionHandler (file: config/GlobalExceptionHandler.java)
- `handleDisabled(DisabledException)`: → `401 Unauthorized` with message "Account is deactivated"
- `handleSelfDeactivation(SelfDeactivationException)`: → `422 Unprocessable Entity` with message "Admin cannot deactivate their own account"

## Business Rules
1. `active` defaults to `true` on registration — `User.builder()` sets `@Builder.Default active = true`; migration sets `DEFAULT TRUE` for existing rows.
2. Deactivated users are rejected at login — Spring Security throws `DisabledException` → 401 before JWT is issued.
3. Admin cannot deactivate their own account — `adminId` checked against `userId` in service before any DB operation; throws `SelfDeactivationException` → 422.
4. `AdminUserResponse` never includes `passwordHash` — only id, email, role, active, createdAt.
5. `activate` is idempotent — calling it on an already-active user sets `active = true` again and returns the user.
6. `deactivate` is idempotent — calling it on an already-inactive user just saves again.

## Key Decisions
- **`DisabledException` handled in service layer via Spring Security**: no explicit check in `login()` — `authenticationManager.authenticate()` calls `CustomUserDetailsService`, which passes `user.isActive()` as `enabled`; Spring throws `DisabledException` automatically; mapped to 401 in `GlobalExceptionHandler`.
- **`SelfDeactivationException` as dedicated exception class**: avoids changing the existing `IllegalArgumentException` → 400 mapping; cleanly maps to 422.
- **`adminId` from JWT in controller** (not service): service `deactivate(adminId, userId)` takes the adminId as a parameter — keeps service testable without a Spring Security context.
- **Soft deactivate only** (no delete endpoint): business requirement is account visibility + toggling, not removal.

## Exception Handling
- `SelfDeactivationException` → 422 — `GlobalExceptionHandler.handleSelfDeactivation`
- `DisabledException` → 401 — `GlobalExceptionHandler.handleDisabled`
- `EntityNotFoundException` (user not found in activate/deactivate) → 404 — `GlobalExceptionHandler.handleNotFound`

## Tests
- `UserServiceImplTest`: deactivate happy path (active=false saved), self-deactivate throws `SelfDeactivationException`, deactivate not found throws `EntityNotFoundException`, activate happy path (active=true saved), activate not found throws — **5 cases**
- `AdminUserControllerTest`: GET no auth → 401, GET admin → 200 with page, PUT activate → 200, PUT activate not found → 404, PUT deactivate other user → 200, PUT deactivate self → 422, PUT deactivate not found → 404 — **7 cases**

## Files Index
**Migration:** src/main/resources/db/migration/V9__add_user_active_flag.sql
**Domain (modified):** src/main/java/com/example/eom/domain/User.java
**Exception:** src/main/java/com/example/eom/exception/SelfDeactivationException.java
**Service Interface:** src/main/java/com/example/eom/service/UserService.java
**Service Impl:** src/main/java/com/example/eom/service/impl/UserServiceImpl.java
**Auth (modified):** src/main/java/com/example/eom/service/impl/CustomUserDetailsService.java
**Controller:** src/main/java/com/example/eom/controller/admin/AdminUserController.java
**DTO:** src/main/java/com/example/eom/dto/user/AdminUserResponse.java
**Config (modified):** src/main/java/com/example/eom/config/GlobalExceptionHandler.java
**Tests:** src/test/java/com/example/eom/service/UserServiceImplTest.java, src/test/java/com/example/eom/controller/AdminUserControllerTest.java
