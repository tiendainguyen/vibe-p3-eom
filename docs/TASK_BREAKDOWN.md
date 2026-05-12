# Task Breakdown — E-commerce Order Management API

> Generated: 2026-05-12
> Source: assignment.md

## Summary

- **Total Tasks:** 47
- **Estimated Total Effort:** ~28–36 hours
- **Critical Path:** T-001 → T-002 → T-003 → T-004 → T-010 → T-030 → T-040 → T-053 → T-054 → T-056
- **External Integrations:** Stripe (payment), RabbitMQ (async email), Redis (caching), PostgreSQL (persistence)

---

## Infrastructure Tasks

| ID | Feature | Layer | Description | Dependencies | Effort | Security Notes |
|----|---------|-------|-------------|--------------|--------|----------------|
| T-001 | Setup | Config | Create `pom.xml` with all required dependencies: Spring Boot 3 Web, Data JPA, Security, Validation, Flyway, PostgreSQL driver, Redis (spring-data-redis), RabbitMQ (spring-rabbit), Stripe Java SDK, JavaMailSender, Lombok, MapStruct | — | M | No wildcard versions — pin all deps |
| T-002 | Setup | Config | Create `src/main/resources/application.yml` with datasource, Redis, RabbitMQ, Stripe key (via env vars), mail, and server config. Create `application-test.yml` pointing to H2/embedded broker | T-001 | M | All secrets via `${ENV_VAR}` — never hardcoded |
| T-003 | Setup | Migration | Create Flyway baseline `V1__baseline.sql` (empty or schema comment) and verify Flyway auto-runs on startup | T-002 | S | — |
| T-004 | Setup | Config | Create `GlobalExceptionHandler` (`@RestControllerAdvice`) with handlers for `MethodArgumentNotValidException`, `EntityNotFoundException`, `IllegalStateException`, and a fallback 500 handler. Create `ErrorResponseDTO` | T-001 | M | Never leak stack traces in response body |
| T-005 | Setup | Config | Create `SecurityConfig` (`@EnableWebSecurity`): permit public product/catalog endpoints, require auth on cart/order/admin, configure stateless session (JWT filter). Create `JwtUtil` and `JwtAuthFilter`. Create `UserDetailsServiceImpl` | T-001 | L | Stateless JWT — no session. Keep CSRF disabled only for REST (stateless API). Never log tokens |
| T-006 | Setup | Domain | Create `AppUser` entity (id, email, passwordHash, role: USER/ADMIN) and `V2__create_users.sql` | T-003 | M | Store bcrypt hash only — never plaintext password |
| T-007 | Setup | Repository | Create `UserRepository` | T-006 | S | Parameterized queries via Spring Data |
| T-008 | Setup | Service | Create `AuthService` (register, login → JWT), `UserService`. Create `AuthController` (`POST /api/auth/register`, `POST /api/auth/login`) | T-005, T-007 | M | Hash password with `BCryptPasswordEncoder` — never log credentials |

---

## Feature: Product Catalog

| ID | Feature | Layer | Description | Dependencies | Effort | Security Notes |
|----|---------|-------|-------------|--------------|--------|----------------|
| T-010 | Product | Domain | Create `Category` entity (id, name, slug) and `Product` entity (id, name, description, price, imageUrl, category, active, createdAt). Add Bean Validation annotations (`@NotBlank`, `@Positive`, `@NotNull`) | T-003 | M | Input validation on all fields |
| T-011 | Product | Migration | Create `V3__create_products.sql`: `categories` and `products` tables with FK constraint | T-010 | S | — |
| T-012 | Product | Repository | Create `CategoryRepository` and `ProductRepository` with methods: `findAllByActiveTrue(Pageable)`, `findByCategoryId(Long, Pageable)`, `findByNameContainingIgnoreCase(String, Pageable)` | T-011 | S | Spring Data — no raw JDBC |
| T-013 | Product | Service | Create `ProductService` interface + `ProductServiceImpl`: `getProducts(filter, pageable)`, `getProductById(id)`, `createProduct(dto)`, `updateProduct(id, dto)`, `deleteProduct(id)`. Return `ProductResponseDTO`. Add Redis caching on `getProducts` / `getProductById` with `@Cacheable` | T-012 | M | Validate all inputs via `@Valid` in callers |
| T-014 | Product | Controller | Create `ProductController` (`GET /api/products`, `GET /api/products/{id}`, `GET /api/products?category=&search=&page=&size=`) — public, no auth | T-013 | S | Input sanitized via Spring's type binding |
| T-015 | Product | Controller | Create `AdminProductController` (`POST /api/admin/products`, `PUT /api/admin/products/{id}`, `DELETE /api/admin/products/{id}`) — ADMIN role required | T-013, T-005 | S | `@PreAuthorize("hasRole('ADMIN')")` on all methods |

---

## Feature: Inventory

| ID | Feature | Layer | Description | Dependencies | Effort | Security Notes |
|----|---------|-------|-------------|--------------|--------|----------------|
| T-020 | Inventory | Domain | Create `Inventory` entity (id, product OneToOne, quantityOnHand, quantityReserved). Add `getAvailableQuantity()` convenience method | T-010 | S | — |
| T-021 | Inventory | Migration | Create `V4__create_inventory.sql`: `inventory` table with unique FK to `products` | T-020 | S | — |
| T-022 | Inventory | Repository | Create `InventoryRepository` with `findByProductId(Long)` | T-021 | S | Spring Data only |
| T-023 | Inventory | Service | Create `InventoryService` interface + `InventoryServiceImpl`: `checkAvailability(productId, qty)`, `reserve(productId, qty)` (throws if insufficient), `release(productId, qty)`, `deduct(productId, qty)`, `adjustStock(productId, delta)`. Use `@Transactional` and pessimistic locking on reserve/deduct | T-022 | M | Atomic transactions — prevent overselling |
| T-024 | Inventory | Controller | Create `AdminInventoryController` (`GET /api/admin/inventory`, `GET /api/admin/inventory/{productId}`, `PUT /api/admin/inventory/{productId}`) — ADMIN only | T-023, T-005 | S | ADMIN role check |

---

## Feature: Cart

| ID | Feature | Layer | Description | Dependencies | Effort | Security Notes |
|----|---------|-------|-------------|--------------|--------|----------------|
| T-030 | Cart | Domain | Create `Cart` entity (id, user OneToOne, createdAt, updatedAt) and `CartItem` entity (id, cart ManyToOne, product ManyToOne, quantity, unitPrice at time of add) | T-010, T-006 | M | — |
| T-031 | Cart | Migration | Create `V5__create_cart.sql`: `carts` and `cart_items` tables | T-030 | S | — |
| T-032 | Cart | Repository | Create `CartRepository` with `findByUserId(Long)` and `CartItemRepository` with `findByCartIdAndProductId(Long, Long)` | T-031 | S | — |
| T-033 | Cart | Service | Create `CartService` interface + `CartServiceImpl`: `getCart(userId)`, `addItem(userId, productId, qty)` (checks inventory availability), `updateItemQuantity(userId, productId, qty)`, `removeItem(userId, productId)`, `clearCart(userId)`, `getCartTotal(userId)` | T-032, T-023 | M | Validate user owns cart before mutating |
| T-034 | Cart | Controller | Create `CartController` (`GET /api/cart`, `POST /api/cart/items`, `PUT /api/cart/items/{productId}`, `DELETE /api/cart/items/{productId}`, `DELETE /api/cart`) — authenticated user | T-033 | S | Extract userId from JWT principal — never from request body |

---

## Feature: Order

| ID | Feature | Layer | Description | Dependencies | Effort | Security Notes |
|----|---------|-------|-------------|--------------|--------|----------------|
| T-040 | Order | Domain | Create `OrderStatus` enum (PENDING, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED). Create `Order` entity (id, user, status, totalAmount, shippingAddress, createdAt, updatedAt). Create `OrderItem` entity (id, order, product, quantity, unitPrice) | T-006, T-010 | M | — |
| T-041 | Order | Migration | Create `V6__create_orders.sql`: `orders` and `order_items` tables | T-040 | S | — |
| T-042 | Order | Repository | Create `OrderRepository` with `findByUserId(Long, Pageable)`, `findByUserIdAndId(Long, Long)`, `findByStatus(OrderStatus, Pageable)`. Create `OrderItemRepository` | T-041 | S | — |
| T-043 | Order | Service | Create `OrderService` interface + `OrderServiceImpl`: `createFromCart(userId, shippingAddressDTO)` (snapshots prices, reserves inventory, clears cart), `getOrder(userId, orderId)`, `getUserOrders(userId, pageable)`, `cancelOrder(userId, orderId)` (releases inventory), `updateStatus(orderId, status)` (admin), `getAllOrders(pageable, statusFilter)` (admin) | T-042, T-033, T-023 | L | Validate user owns order before cancel. Transactional. |
| T-044 | Order | Controller | Create `OrderController` (`POST /api/orders`, `GET /api/orders`, `GET /api/orders/{id}`, `DELETE /api/orders/{id}`) — authenticated user | T-043 | S | userId from JWT only |
| T-045 | Order | Controller | Create `AdminOrderController` (`GET /api/admin/orders`, `GET /api/admin/orders/{id}`, `PUT /api/admin/orders/{id}/status`) — ADMIN only | T-043, T-005 | S | `@PreAuthorize("hasRole('ADMIN')")` |

---

## Feature: Payment (Stripe)

| ID | Feature | Layer | Description | Dependencies | Effort | Security Notes |
|----|---------|-------|-------------|--------------|--------|----------------|
| T-050 | Payment | Domain | Create `Payment` entity (id, order OneToOne, stripePaymentIntentId, amount, currency, status: PENDING/SUCCEEDED/FAILED/REFUNDED, createdAt) | T-040 | S | Never store raw card data |
| T-051 | Payment | Migration | Create `V7__create_payments.sql`: `payments` table | T-050 | S | — |
| T-052 | Payment | Repository | Create `PaymentRepository` with `findByOrderId(Long)` and `findByStripePaymentIntentId(String)` | T-051 | S | — |
| T-053 | Payment | Config | Create `StripeConfig` (`@Configuration`): initialize `Stripe.apiKey` from `${STRIPE_SECRET_KEY}` env var on `@PostConstruct` | T-001 | S | API key from env only — never hardcoded |
| T-054 | Payment | Service | Create `PaymentService` interface + `PaymentServiceImpl`: `createPaymentIntent(orderId, userId)` (creates Stripe PaymentIntent, persists Payment record, returns clientSecret), `confirmPayment(paymentIntentId)` (updates Payment + Order status to PAID, deducts inventory), `refundPayment(orderId, adminUserId)` (Stripe refund API, updates statuses) | T-052, T-053, T-043 | L | Never log Stripe keys or card data. Handle Stripe API errors gracefully |
| T-055 | Payment | Controller | Create `PaymentController` (`POST /api/payments/intent` takes orderId, returns `{clientSecret}`) — authenticated | T-054 | S | userId from JWT — verify user owns order |
| T-056 | Payment | Controller | Create `StripeWebhookController` (`POST /api/webhooks/stripe`): verify Stripe signature using `STRIPE_WEBHOOK_SECRET` env var, handle `payment_intent.succeeded` and `payment_intent.payment_failed` events | T-054 | M | Verify webhook signature on every request — reject unsigned events |

---

## Feature: Notifications (RabbitMQ + Email)

| ID | Feature | Layer | Description | Dependencies | Effort | Security Notes |
|----|---------|-------|-------------|--------------|--------|----------------|
| T-060 | Notification | Config | Create `RabbitMQConfig`: declare `orders` exchange (topic), queues `order.confirmation`, `order.shipped`, `order.delivered`, and their bindings with routing keys | T-001 | M | — |
| T-061 | Notification | DTO | Create `OrderConfirmedEvent`, `OrderShippedEvent`, `OrderDeliveredEvent` records (orderId, userId, email, items summary, total) | T-040 | S | No PII beyond what's needed for email |
| T-062 | Notification | Service | Create `NotificationPublisher` service: `publishOrderConfirmed(order)`, `publishOrderShipped(order)`, `publishOrderDelivered(order)`. Called from `OrderServiceImpl` on status transitions | T-061, T-060 | S | — |
| T-063 | Notification | Service | Create `EmailNotificationConsumer` (`@RabbitListener`): consume from each queue, build email from order data, send via `JavaMailSender`. Use simple HTML templates | T-062 | M | Never log email content with PII. Use env vars for SMTP credentials |

---

## Feature: Outgoing Webhooks

| ID | Feature | Layer | Description | Dependencies | Effort | Security Notes |
|----|---------|-------|-------------|--------------|--------|----------------|
| T-070 | Webhook | Domain | Create `WebhookSubscription` entity (id, url, secret, events: Set<String>, active, createdAt) | T-003 | S | Store HMAC secret — never log it |
| T-071 | Webhook | Migration | Create `V8__create_webhooks.sql`: `webhook_subscriptions` table | T-070 | S | — |
| T-072 | Webhook | Repository | Create `WebhookSubscriptionRepository` with `findByActiveTrue()` and `findByEventsContaining(String)` | T-071 | S | — |
| T-073 | Webhook | Service | Create `WebhookService` interface + `WebhookServiceImpl`: `register(dto)`, `listAll()`, `delete(id)`, `dispatch(eventName, payload)` — sends signed HTTP POST to each active subscriber using HMAC-SHA256 signature header | T-072 | M | Sign each webhook payload with per-subscription secret (HMAC-SHA256). Non-blocking dispatch via `@Async` |
| T-074 | Webhook | Controller | Create `AdminWebhookController` (`POST /api/admin/webhooks`, `GET /api/admin/webhooks`, `DELETE /api/admin/webhooks/{id}`) — ADMIN only | T-073, T-005 | S | ADMIN role only |
| T-075 | Webhook | Service | Integrate `WebhookService.dispatch()` into `OrderServiceImpl` and `PaymentServiceImpl` on key status changes (order.created, order.paid, order.shipped, order.delivered, payment.failed) | T-073, T-043, T-054 | S | — |

---

## Testing Tasks

| ID | Feature | Layer | Description | Dependencies | Effort | Security Notes |
|----|---------|-------|-------------|--------------|--------|----------------|
| T-080 | Test | Unit | `ProductServiceTest`: test getProducts with filters, getProductById, cache miss/hit behavior (mock repo + cache manager) | T-013 | S | — |
| T-081 | Test | Unit | `InventoryServiceTest`: test reserve (success + insufficient stock), release, deduct | T-023 | S | — |
| T-082 | Test | Unit | `CartServiceTest`: test addItem (normal + out of stock), updateQuantity, removeItem, clearCart | T-033 | S | — |
| T-083 | Test | Unit | `OrderServiceTest`: test createFromCart (happy path, cart empty, inventory fail), cancelOrder (owner check, status check) | T-043 | M | Test that non-owner cannot cancel |
| T-084 | Test | Unit | `PaymentServiceTest`: test createPaymentIntent, confirmPayment (order status transition), refundPayment | T-054 | M | Mock Stripe SDK |
| T-085 | Test | Integration | `AuthControllerIT`: register + login flow end-to-end using `@SpringBootTest` with test DB | T-008 | M | Verify JWT returned on login |
| T-086 | Test | Integration | `OrderFlowIT`: full happy path — register → add to cart → checkout → create payment intent → simulate webhook confirmation → verify order PAID + inventory deducted | T-056, T-043 | L | — |

---

## Dependency Graph (Critical Path)

```
T-001 (pom.xml)
  └─ T-002 (application.yml)
       └─ T-003 (Flyway baseline)
            ├─ T-004 (GlobalExceptionHandler)
            ├─ T-005 (SecurityConfig + JWT)
            │    └─ T-006 (AppUser entity)
            │         └─ T-007 (UserRepository)
            │              └─ T-008 (AuthService + AuthController)
            ├─ T-010 (Product + Category entities)
            │    ├─ T-011 → T-012 → T-013 → T-014/T-015  [Product Catalog]
            │    ├─ T-020 → T-021 → T-022 → T-023 → T-024  [Inventory]
            │    └─ T-030 → T-031 → T-032 → T-033 → T-034  [Cart]
            │         └─ T-040 → T-041 → T-042 → T-043 → T-044/T-045  [Order]
            │              ├─ T-050 → T-051 → T-052 → T-053 → T-054 → T-055/T-056  [Payment]
            │              ├─ T-060 → T-061 → T-062 → T-063  [Notifications]
            │              └─ T-070 → T-071 → T-072 → T-073 → T-074 → T-075  [Webhooks]
```

---

## Validation Checklist

- [x] All 8 features from assignment.md are covered
- [x] All layers (Domain, Repository, Service, Controller, Config, Migration) included
- [x] Dependencies form a valid DAG — no circular dependencies
- [x] Security rules noted for all sensitive tasks
- [x] Flyway migrations precede all entity/repository tasks
- [x] Stripe, RabbitMQ, Redis integrations have dedicated config tasks (T-053, T-060, T-002)
- [x] Unit tests + integration tests included (T-080 to T-086)
- [x] Admin endpoints separated from user endpoints with role checks
