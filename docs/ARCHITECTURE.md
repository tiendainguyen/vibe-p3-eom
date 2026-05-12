# Architecture — E-commerce Order Management API

> Last verified: 2026-05-12

## System Diagram

```
[Client / Postman]
        │ HTTPS REST
        ▼
[Spring Boot 3 API : 8080]
  ├── SecurityFilter (JWT)
  ├── Controllers  (HTTP layer)
  ├── Services     (business logic)
  ├── Repositories (Spring Data JPA)
  │
  ├──► PostgreSQL   (primary data store)
  ├──► Redis        (product catalog cache)
  ├──► RabbitMQ     (async email notifications)
  └──► Stripe API   (payment processing)
```

## Source Tree

```
src/main/java/com/example/eom/
├── Application.java
├── config/
├── controller/
│   └── admin/
├── service/
├── repository/
├── domain/
│   └── enums/
└── dto/

src/main/resources/
├── application.yml
└── db/migration/     ← Flyway V1–V8

src/test/
├── java/com/example/eom/
└── resources/application-test.yml
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
| Auth | Stateless JWT | REST API — no server-side session needed |
| Payments | Stripe PaymentIntents | Client confirms on frontend; webhook confirms on backend |
| Async notifications | RabbitMQ | Decouple email sending from order flow; retry on failure |
| Caching | Redis `@Cacheable` | Product catalog is read-heavy; reduces DB load |
| Migrations | Flyway | Version-controlled schema; auto-runs on startup |
| Inventory locking | Pessimistic lock on reserve/deduct | Prevent overselling under concurrent orders |

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
   - `NotificationPublisher` fires `OrderConfirmedEvent`
   - `WebhookService` dispatches to external subscribers

### Order Status Lifecycle
```
PENDING → PAID → PROCESSING → SHIPPED → DELIVERED
           │
           └──► CANCELLED (before PAID, releases inventory)
           └──► REFUNDED  (after PAID, via Stripe refund)
```
