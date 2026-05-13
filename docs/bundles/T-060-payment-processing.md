---
feature: Payment Processing
task: T-060
issue: N/A (no GitHub remote — local task ID only)
archived: 2026-05-13
files:
  - src/main/java/com/example/eom/controller/PaymentController.java
  - src/main/java/com/example/eom/controller/StripeWebhookController.java
  - src/main/java/com/example/eom/service/PaymentService.java
  - src/main/java/com/example/eom/service/impl/PaymentServiceImpl.java
  - src/main/java/com/example/eom/service/gateway/StripeGateway.java
  - src/main/java/com/example/eom/service/gateway/impl/StripeGatewayImpl.java
  - src/main/java/com/example/eom/dto/payment/CreatePaymentIntentRequest.java
  - src/main/java/com/example/eom/dto/payment/PaymentIntentResponse.java
  - src/main/java/com/example/eom/repository/OrderRepository.java (findByStripePaymentIntentId)
  - src/main/java/com/example/eom/service/OrderService.java (linkPaymentIntent, markAsPaid, markAsRefunded, findOrderIdByPaymentIntentId)
  - src/main/java/com/example/eom/service/impl/OrderServiceImpl.java (linkPaymentIntent, markAsPaid, markAsRefunded, findOrderIdByPaymentIntentId)
  - src/test/java/com/example/eom/controller/PaymentControllerTest.java
  - src/test/java/com/example/eom/service/PaymentServiceImplTest.java
---

# Bundle: Payment Processing

## API Surface
| Method | Path | Auth | Handler |
|--------|------|------|---------|
| POST | /api/payments/intent | Bearer JWT | `PaymentController.createIntent()` |
| POST | /api/webhooks/stripe | None (Stripe-Signature header) | `StripeWebhookController.handleStripeWebhook()` |

## Logic Map

### PaymentController (file: controller/PaymentController.java)
- `createIntent(request, auth)`: extracts userId from JWT principal string → delegates to `paymentService.createIntent(userId, request)` → returns 200 with `PaymentIntentResponse`

### StripeWebhookController (file: controller/StripeWebhookController.java)
- `handleStripeWebhook(payload, sigHeader)`: receives raw `String` payload + `Stripe-Signature` header → delegates to `paymentService.handleWebhookEvent()` → returns `200 void`; public endpoint (no JWT)

### PaymentServiceImpl (file: service/impl/PaymentServiceImpl.java)
- `createIntent(userId, request)`: fetches order via `orderService.getById(userId, orderId)` (ownership + PENDING enforced by OrderService) → converts `totalAmount` to cents via `movePointRight(2)` → calls `stripeGateway.createPaymentIntent(amountCents, "usd", orderId)` → saves intent ID via `orderService.linkPaymentIntent()` → returns `PaymentIntentResponse`
- `handleWebhookEvent(payload, sigHeader)`: calls `stripeGateway.constructEvent()` to verify signature (throws `IllegalArgumentException` on failure) → switches on `event.getType()`: `payment_intent.succeeded` → `orderService.markAsPaid(piId)` + `notificationPublisher.publishOrderConfirmed(orderId)`; `payment_intent.payment_failed` → log warn; `charge.refunded` → `orderService.markAsRefunded(piId)`; default → log debug
- `extractPaymentIntentId(event)`: deserializes event data as `PaymentIntent` → returns `pi.getId()` or null
- `extractPaymentIntentIdFromCharge(event)`: deserializes event data as `Charge` → returns `charge.getPaymentIntent()` or null

### StripeGatewayImpl (file: service/gateway/impl/StripeGatewayImpl.java)
- Constructor: sets `Stripe.apiKey` from `${stripe.secret-key}` env var at bean creation time
- `createPaymentIntent(amountCents, currency, orderId)`: builds `PaymentIntentCreateParams` with amount, currency, and `order_id` metadata → calls `PaymentIntent.create(params)`
- `constructEvent(payload, sigHeader, endpointSecret)`: delegates to `Webhook.constructEvent()` — throws `StripeException` on bad signature

### OrderServiceImpl (payment-related methods, file: service/impl/OrderServiceImpl.java)
- `linkPaymentIntent(orderId, paymentIntentId)`: loads order by ID → sets `stripePaymentIntentId` field → saves
- `markAsPaid(paymentIntentId)`: looks up order by `stripePaymentIntentId` (idempotent: `ifPresent`) → sets `status = PAID` → saves
- `markAsRefunded(paymentIntentId)`: looks up order by `stripePaymentIntentId` (idempotent: `ifPresent`) → sets `status = REFUNDED` → saves
- `findOrderIdByPaymentIntentId(paymentIntentId)`: looks up order by `stripePaymentIntentId` → maps to `order.getId()` or null

## Business Rules
1. `POST /api/payments/intent` enforces order ownership and PENDING status through `OrderService.getById(userId, orderId)` — access violations surface as `AccessDeniedException` (→ 403), not-found as `EntityNotFoundException` (→ 404).
2. Payment amount is always computed server-side: `order.totalAmount * 100` (cents) — the request body carries only `orderId`, never an amount.
3. Stripe webhook signature MUST be verified via `Webhook.constructEvent()` before any state change; failure → `IllegalArgumentException` → 400.
4. Webhook handlers use `ifPresent` — processing the same `payment_intent.succeeded` twice is idempotent (PAID → PAID no-op).
5. `stripePaymentIntentId` is stored on the Order after intent creation to enable webhook-to-order lookup and idempotency.
6. `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` are loaded from environment variables (`${stripe.secret-key}`, `${stripe.webhook-secret}`) — never hardcoded.
7. The `/api/webhooks/**` path is publicly accessible in `SecurityConfig` (no JWT required) — security is enforced solely by Stripe signature verification.
8. Currency defaults to `usd`; not configurable at the request level.

## Key Decisions
- `StripeGateway` interface wraps all Stripe SDK calls: allows mocking in tests without Stripe test credentials.
- `@RequestBody String` on webhook controller (not `byte[]`): Stripe SDK's `Webhook.constructEvent()` accepts `String` — raw bytes not strictly required when using the Java SDK wrapper.
- Amount conversion uses `BigDecimal.movePointRight(2).longValue()` to avoid floating-point rounding.
- `markAsPaid`/`markAsRefunded` use `ifPresent` silently dropping unknown `paymentIntentId` values — prevents noise from unrelated Stripe events.

## Exception Handling
- `EntityNotFoundException` → 404 — `GlobalExceptionHandler`
- `AccessDeniedException` → 403 — `GlobalExceptionHandler`
- `IllegalArgumentException` (invalid webhook sig) → 400 — `GlobalExceptionHandler`
- `IllegalStateException` (Stripe API failure on intent creation) → 409 — `GlobalExceptionHandler`

## Tests
- `PaymentControllerTest`: covers POST /intent — 401 (no auth), 200 (valid), 400 (null orderId), 404 (not found), 403 (wrong user), 409 (Stripe error); POST /webhooks/stripe — 200 (valid sig), 400 (invalid sig), 200 (no auth = public); 9 cases total
- `PaymentServiceImplTest`: covers `createIntent` — happy path (amount conversion, linkPaymentIntent called), order not found, wrong user, Stripe API failure; `handleWebhookEvent` — invalid signature; 5 cases total

## Files Index
**Service Interface:** src/main/java/com/example/eom/service/PaymentService.java
**Service Impl:** src/main/java/com/example/eom/service/impl/PaymentServiceImpl.java
**Gateway Interface:** src/main/java/com/example/eom/service/gateway/StripeGateway.java
**Gateway Impl:** src/main/java/com/example/eom/service/gateway/impl/StripeGatewayImpl.java
**Controller:** src/main/java/com/example/eom/controller/PaymentController.java, src/main/java/com/example/eom/controller/StripeWebhookController.java
**DTO:** src/main/java/com/example/eom/dto/payment/CreatePaymentIntentRequest.java, src/main/java/com/example/eom/dto/payment/PaymentIntentResponse.java
**Repository (cross-feature):** src/main/java/com/example/eom/repository/OrderRepository.java — `findByStripePaymentIntentId`
**Service (cross-feature):** src/main/java/com/example/eom/service/OrderService.java, src/main/java/com/example/eom/service/impl/OrderServiceImpl.java — payment lifecycle methods
**Tests:** src/test/java/com/example/eom/controller/PaymentControllerTest.java, src/test/java/com/example/eom/service/PaymentServiceImplTest.java
