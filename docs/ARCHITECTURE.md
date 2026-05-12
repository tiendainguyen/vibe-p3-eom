# Architecture — E-commerce Order Management API

> Last verified: 2026-05-12

## System Diagram

```
[Client / Postman]
        │ HTTPS REST
        ▼
[Spring Boot 3.4.1 API : 8080]
  ├── SecurityFilter (JWT — wired in T-010)
  ├── Controllers  (HTTP layer)
  ├── Services     (business logic)
  ├── Repositories (Spring Data JPA)
  │
  ├──► PostgreSQL   (primary data store)
  ├──► Redis        (product catalog cache — T-020)
  ├──► RabbitMQ     (async email notifications — T-070)
  └──► Stripe API   (payment processing — T-060)
```

## Source Tree

```
src/main/java/com/example/eom/
├── Application.java              ← @SpringBootApplication + @EnableAsync
├── config/
│   ├── SecurityConfig.java       ← JWT filter chain, BCrypt bean
│   ├── GlobalExceptionHandler.java ← @RestControllerAdvice
│   ├── OpenApiConfig.java        ← Swagger JWT bearer scheme
│   ├── RabbitMQConfig.java       ← queues/exchanges (T-070)
│   └── RedisConfig.java          ← @EnableCaching (T-020)
├── controller/
│   └── admin/                    ← Admin*Controller classes
├── service/                      ← interfaces + impl
├── repository/                   ← Spring Data JPA interfaces
├── domain/
│   └── enums/                    ← OrderStatus, Role, etc.
└── dto/
    └── ErrorResponseDTO.java     ← shared error shape

src/main/resources/
├── application.yml               ← all secrets via ${ENV_VAR}
└── db/migration/
    ├── V1__baseline.sql          ← empty baseline
    ├── V2__create_users.sql      ← T-010
    ├── V3__create_products.sql   ← T-020
    ├── V4__create_inventory.sql  ← T-030
    ├── V5__create_cart_items.sql ← T-040
    ├── V6__create_orders.sql     ← T-050
    ├── V7__create_order_items.sql ← T-050
    ├── V8__create_webhook_subscriptions.sql ← T-080
    └── V9__add_user_active_flag.sql ← T-090

src/test/
├── java/com/example/eom/
│   └── ApplicationTests.java
└── resources/
    └── application-test.yml     ← H2 in-memory, no Redis/RabbitMQ
```

## Layer Responsibilities

| Layer | Location | Does | Does NOT |
|-------|----------|------|----------|
| Controller | `controller/` | Parse HTTP, validate with `@Valid`, delegate to service, return DTOs | Business logic, direct repo access |
| Service | `service/` | Business logic, orchestration, DTO conversion | HTTP awareness, direct DB calls |
| Repository | `repository/` | Spring Data queries, Flyway migrations | Business rules |
| Domain | `domain/` | JPA entities, value objects, enums | HTTP, business logic |
| DTO | `dto/` | Request/response shapes | Persistence annotations |
| Config | `config/` | Bean wiring, Security, RabbitMQ, Stripe, Redis | Business logic |

## Key Boundaries

- Controllers never inject repositories — always go through services
- Services never return JPA entities — always convert to DTOs
- Only services may call Stripe, RabbitMQ, or other external APIs
- `userId` always extracted from JWT principal — never trusted from request body
- Admin endpoints separated into `Admin*Controller` classes with `@PreAuthorize("hasRole('ADMIN')")`

## Key Technical Decisions

| Decision | Choice | Reason |
|----------|--------|--------|
| Auth | Stateless JWT (jjwt 0.12.6) | REST API — no server-side session needed |
| Payments | Stripe PaymentIntents | Client confirms on frontend; webhook confirms on backend |
| Async notifications | RabbitMQ | Decouple email sending from order flow; retry on failure |
| Caching | Redis `@Cacheable` | Product catalog is read-heavy; reduces DB load |
| Migrations | Flyway | Version-controlled schema; auto-runs on startup |
| Inventory locking | Pessimistic lock on reserve/deduct | Prevent overselling under concurrent orders |
| API docs | SpringDoc OpenAPI 2.3.0 | Swagger UI at /swagger-ui.html |
| Password hashing | BCrypt (strength 12) | Industry standard; ~250ms per hash acceptable for auth |

## Core Flows

### Checkout Flow
1. User adds items to cart → `CartService` checks inventory availability
2. User calls `POST /api/orders` → `OrderService.createFromCart()`:
   - Snapshots prices from product catalog
   - Reserves inventory (`InventoryService.reserve()`)
   - Clears cart
   - Creates Order (status: PENDING)
3. User calls `POST /api/payments/intent` → `PaymentService` creates Stripe PaymentIntent, returns `clientSecret`
4. Client confirms payment on Stripe (frontend / Postman)
5. Stripe calls `POST /api/webhooks/stripe` → signature verified → `payment_intent.succeeded` event:
   - Order status → PAID
   - Inventory deducted (reservation confirmed)
   - `NotificationPublisher` fires `OrderConfirmedEvent` → RabbitMQ → email sent
   - `WebhookService` dispatches to external subscribers

### Order Status Lifecycle
```
PENDING → PAID → PROCESSING → SHIPPED → DELIVERED
           │
           └──► CANCELLED (before PAID, releases inventory)
           └──► REFUNDED  (after PAID, via Stripe refund webhook)
```

## Dependencies (pom.xml)

| Dependency | Version | Purpose |
|------------|---------|---------|
| spring-boot-starter-parent | 3.4.1 | BOM + plugin management |
| spring-boot-starter-web | (managed) | REST controllers |
| spring-boot-starter-security | (managed) | JWT filter chain |
| spring-boot-starter-data-jpa | (managed) | Repositories |
| spring-boot-starter-data-redis | (managed) | Cache |
| spring-boot-starter-amqp | (managed) | RabbitMQ |
| spring-boot-starter-mail | (managed) | Email |
| spring-boot-starter-validation | (managed) | Bean Validation |
| spring-boot-starter-actuator | (managed) | Health endpoint |
| flyway-core + flyway-database-postgresql | (managed) | Migrations |
| postgresql | (managed) | Driver |
| jjwt-api/impl/jackson | 0.12.6 | JWT |
| stripe-java | 25.3.0 | Stripe SDK |
| springdoc-openapi-starter-webmvc-ui | 2.3.0 | Swagger UI |
| lombok | (managed) | Boilerplate reduction |
| h2 (test) | (managed) | In-memory DB for @DataJpaTest |

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| DB_URL | No | jdbc:postgresql://localhost:5432/eom | PostgreSQL JDBC URL |
| DB_USER | No | postgres | DB username |
| DB_PASSWORD | **Yes** | — | DB password |
| REDIS_HOST | No | localhost | Redis host |
| REDIS_PORT | No | 6379 | Redis port |
| RABBITMQ_HOST | No | localhost | RabbitMQ host |
| RABBITMQ_USER | No | guest | RabbitMQ username |
| RABBITMQ_PASSWORD | No | guest | RabbitMQ password |
| MAIL_HOST | **Yes** | — | SMTP host |
| MAIL_USER | **Yes** | — | SMTP username |
| MAIL_PASSWORD | **Yes** | — | SMTP password |
| STRIPE_SECRET_KEY | **Yes** | — | Stripe secret key (sk_test_...) |
| STRIPE_WEBHOOK_SECRET | **Yes** | — | Stripe webhook signing secret |
| JWT_SECRET | **Yes** | — | JWT signing secret (≥ 32 chars) |
| JWT_EXPIRATION_MS | No | 86400000 | Token expiry (ms), default 24h |
