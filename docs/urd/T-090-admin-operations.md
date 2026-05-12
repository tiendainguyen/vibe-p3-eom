---
task_id: T-090
feature: Admin Operations
status: pending
effort: M
dependencies: [T-010, T-020, T-050]
---

# URD — Admin Operations

## 1. Business Context
Platform operators need a consolidated set of admin tools to manage the storefront: viewing all users, deactivating accounts, auditing all orders, generating a sales summary, and performing bulk product operations. These endpoints are collected into admin controllers and protected by the ADMIN role. This is the "back-office" view of the system.

## 2. User Stories
- As an **admin**, I want to list all registered users so that I can audit accounts and contact customers.
- As an **admin**, I want to deactivate a user account so that a bad actor cannot place further orders.
- As an **admin**, I want to view a sales summary (total revenue, order counts by status) so that I can assess business performance.
- As an **admin**, I want to bulk-update product category so that catalog reorganization is efficient.
- As an **admin**, I want to view an order that belongs to any user so that I can resolve support tickets.

## 3. Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-090-1 | `GET /api/admin/users` returns paginated list of all users | Must Have |
| FR-090-2 | `PUT /api/admin/users/{id}/deactivate` sets user `active=false` | Must Have |
| FR-090-3 | Deactivated users cannot log in (login returns `403`) | Must Have |
| FR-090-4 | `GET /api/admin/dashboard` returns summary: total revenue, orders by status, active products count | Must Have |
| FR-090-5 | `GET /api/admin/orders` returns all orders (already defined in T-050 — consolidate here) | Must Have |
| FR-090-6 | `GET /api/admin/orders/{id}` returns any order regardless of owner | Must Have |
| FR-090-7 | All admin endpoints require ADMIN role — non-admin returns `403` | Must Have |

## 4. API Endpoints
| Method | Path | Auth | Request Body | Response | Description |
|--------|------|------|--------------|----------|-------------|
| GET | /api/admin/users | Bearer JWT (ADMIN) | — | `Page<AdminUserResponse>` | List all users |
| PUT | /api/admin/users/{id}/deactivate | Bearer JWT (ADMIN) | — | `AdminUserResponse` | Deactivate user account |
| GET | /api/admin/dashboard | Bearer JWT (ADMIN) | — | `DashboardResponse` | Sales summary |
| GET | /api/admin/orders | Bearer JWT (ADMIN) | — | `Page<OrderSummaryResponse>` | All orders (cross-user) |
| GET | /api/admin/orders/{id} | Bearer JWT (ADMIN) | — | `OrderResponse` | Any order by ID |

## 5. Data Entities
**User** (existing — add `active` field)
- `active` (Boolean, not null, default true)

**Flyway migration file:** `V9__add_user_active_flag.sql`

**DashboardResponse** (DTO, not an entity):
- `totalRevenue` (BigDecimal) — sum of `totalAmount` for PAID/PROCESSING/SHIPPED/DELIVERED orders
- `orderCountByStatus` (Map<String, Long>) — count per status
- `activeProductCount` (Long)
- `totalUserCount` (Long)

## 6. Business Rules
1. Admin cannot deactivate their own account.
2. Deactivated user attempting to log in receives `403 Forbidden` with message "Account is disabled".
3. Dashboard totals are computed live from DB aggregations (no caching — accuracy preferred).
4. `totalRevenue` includes only orders in status: PAID, PROCESSING, SHIPPED, DELIVERED — excludes CANCELLED and REFUNDED.
5. All admin endpoints are accessible only to users with role ADMIN (`@PreAuthorize("hasRole('ADMIN')")`).

## 7. Implementation Layers
1. **Domain** — `User` entity updated with `active` boolean
2. **Migration** — `V9__add_user_active_flag.sql` (adds `active` column, backfills to `true`)
3. **Repository**
   - `UserRepository` — add `findAllByOrderByCreatedAtDesc(Pageable)`, `findByIdAndActiveTrue(Long)`
   - `OrderRepository` — add `findTotalRevenueByStatusIn(List<OrderStatus>)`, `countByStatus(OrderStatus)`
   - `ProductRepository` — add `countByActiveTrue()`
4. **Service**
   - `AdminUserService` interface: `listAllUsers(Pageable)`, `deactivateUser(Long adminId, Long targetUserId)`
   - `AdminDashboardService` interface: `getDashboard()`
   - Implementations for both
   - Update `CustomUserDetailsService` to check `user.isActive()` — throw `DisabledException` if false
5. **Controller**
   - `AdminUserController` — `/api/admin/users/**`
   - `AdminDashboardController` — `/api/admin/dashboard`
6. **Config** — none additional
7. **Tests**
   - `AdminUserServiceImplTest` — list users, deactivate valid user, deactivate self (throws), deactivate non-existent (throws)
   - `AdminDashboardServiceImplTest` — revenue calculation excludes CANCELLED/REFUNDED orders
   - `AdminUserControllerTest` — GET 200, PUT deactivate 200/400 (self), 403 for non-admin

## 8. Acceptance Criteria
- [ ] `GET /api/admin/users` returns paginated user list with role and active status
- [ ] `PUT /api/admin/users/{id}/deactivate` sets `active=false` in DB
- [ ] Deactivated user attempting `POST /api/auth/login` receives `403`
- [ ] Admin cannot deactivate their own account — returns `400`
- [ ] `GET /api/admin/dashboard` returns correct revenue (excludes CANCELLED/REFUNDED)
- [ ] All admin endpoints return `403` when called by a USER role JWT
- [ ] All admin tests pass

## 9. Security Checklist
- [ ] All admin endpoints annotated with `@PreAuthorize("hasRole('ADMIN')")`
- [ ] Admin cannot deactivate themselves (business rule enforced in service layer)
- [ ] Dashboard aggregation queries are read-only `@Transactional(readOnly = true)`
- [ ] `AdminUserResponse` never exposes `passwordHash`
- [ ] Pagination prevents full table scans on large user/order sets

## 10. Non-Functional Notes
- Dashboard is computed on-demand; for high traffic this would be a scheduled job writing to a summary table, but live queries are fine for demo scale.
- User deactivation is soft — account data is preserved for audit purposes.
- Admin role is only granted via `DataInitializer` seed or a direct DB update — there is no "promote to admin" API endpoint.
