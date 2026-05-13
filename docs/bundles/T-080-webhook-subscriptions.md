---
feature: Webhook Subscriptions for External Systems
task: T-080
issue: #9
archived: 2026-05-13
files:
  - src/main/java/com/example/eom/domain/WebhookSubscription.java
  - src/main/resources/db/migration/V8__create_webhook_subscriptions.sql
  - src/main/java/com/example/eom/repository/WebhookSubscriptionRepository.java
  - src/main/java/com/example/eom/service/WebhookService.java
  - src/main/java/com/example/eom/service/impl/WebhookServiceImpl.java
  - src/main/java/com/example/eom/controller/admin/AdminWebhookController.java
  - src/main/java/com/example/eom/dto/webhook/CreateWebhookRequest.java
  - src/main/java/com/example/eom/dto/webhook/WebhookResponse.java
  - src/main/java/com/example/eom/dto/webhook/WebhookPayload.java
  - src/main/java/com/example/eom/config/WebhookConfig.java
  - src/main/java/com/example/eom/service/impl/PaymentServiceImpl.java (order.paid dispatch)
  - src/main/java/com/example/eom/service/impl/OrderServiceImpl.java (order.shipped/delivered dispatch)
  - src/test/java/com/example/eom/service/WebhookServiceImplTest.java
  - src/test/java/com/example/eom/controller/AdminWebhookControllerTest.java
  - src/test/java/com/example/eom/service/PaymentServiceImplTest.java (added @Mock WebhookService)
  - src/test/java/com/example/eom/service/OrderServiceImplTest.java (added @Mock WebhookService)
---

# Bundle: Webhook Subscriptions for External Systems

## API Surface
| Method | Path | Auth | Handler |
|--------|------|------|---------|
| POST | /api/admin/webhooks | ADMIN JWT | `AdminWebhookController.create()` |
| GET | /api/admin/webhooks | ADMIN JWT | `AdminWebhookController.listAll()` |
| DELETE | /api/admin/webhooks/{id} | ADMIN JWT | `AdminWebhookController.delete()` |

Dispatch is internal-only — no HTTP endpoint triggers it directly.

## Logic Map

### WebhookServiceImpl (file: service/impl/WebhookServiceImpl.java)
- `create(request)`: validates each eventType against `VALID_EVENT_TYPES` set (throws `IllegalArgumentException` for unknown) → builds `WebhookSubscription` → saves → returns `WebhookResponse`
- `listAll()`: `repository.findAll()` → map to `WebhookResponse` list
- `delete(id)`: loads by id (throws `EntityNotFoundException` if missing) → sets `active = false` → saves (soft delete)
- `dispatch(eventType, orderId)`: `@Async` — queries `findActiveByEventType(eventType)` → if empty returns immediately → builds `WebhookPayload(eventType, orderId, now)` + JSON headers → iterates subscribers, POSTs via `webhookRestTemplate`; catches and logs any exception per subscriber without rethrowing

### WebhookConfig (file: config/WebhookConfig.java)
- `webhookRestTemplate(builder)`: `@Bean` — builds `RestTemplate` with 5s connect + 5s read timeout via `RestTemplateBuilder`

### AdminWebhookController (file: controller/admin/AdminWebhookController.java)
- `create(request)`: `@Valid` request → `webhookService.create()` → `201 Created`
- `listAll()`: → `webhookService.listAll()` → `200 OK`
- `delete(id)`: → `webhookService.delete(id)` → `204 No Content`

### PaymentServiceImpl — dispatch hook (file: service/impl/PaymentServiceImpl.java)
- `handleWebhookEvent` `payment_intent.succeeded` branch: after `markAsPaid` + `publishOrderConfirmed`, calls `webhookService.dispatch(EVENT_ORDER_PAID, orderId)` if orderId is non-null

### OrderServiceImpl — dispatch hooks (file: service/impl/OrderServiceImpl.java)
- `updateStatus` SHIPPED branch: calls `webhookService.dispatch(EVENT_ORDER_SHIPPED, orderId)` alongside `notificationPublisher.publishOrderShipped`
- `updateStatus` DELIVERED branch: calls `webhookService.dispatch(EVENT_ORDER_DELIVERED, orderId)`

### WebhookSubscriptionRepository (file: repository/WebhookSubscriptionRepository.java)
- `findActiveByEventType(eventType)`: custom JPQL — `JOIN s.eventTypes e WHERE e = :eventType AND s.active = true`

## Business Rules
1. Subscription URL must pass `@URL(protocol = "https")` — HTTP URLs are rejected at the controller with 400.
2. Valid event types are exactly: `"order.paid"`, `"order.shipped"`, `"order.delivered"` — any other value causes `IllegalArgumentException` (→ 409) in service.
3. DELETE is a soft delete — sets `active = false`; subscription remains in DB and `listAll()` still returns it with `active: false`.
4. Inactive subscriptions (`active = false`) are never included in dispatch queries — `findActiveByEventType` filters them out.
5. `dispatch()` is `@Async` — runs on a Spring task executor thread, never blocks the calling order/payment flow.
6. Failed HTTP delivery is caught and logged with eventType + orderId + url — message is NOT retried.
7. `dispatch()` is a no-op if no active subscribers exist for the event type (early return).

## Key Decisions
- **`@ElementCollection` for eventTypes**: join table `webhook_subscription_event_types`; enables clean JPQL `JOIN` query; portable between PostgreSQL and H2 test DB.
- **Soft delete (active=false)**: matches acceptance criteria "deactivates subscription"; preserves history.
- **`RestTemplate` as injectable `@Bean`**: defined in `WebhookConfig` so `WebhookServiceImpl` can receive it via constructor — mockable in `@ExtendWith(MockitoExtension.class)` unit tests without Spring context.
- **Catch-and-log per subscriber (no rethrow)**: a failing subscriber never affects other subscribers or the order flow; orderId in log enables manual investigation.
- **Event type constants on `WebhookService` interface**: `EVENT_ORDER_PAID`, `EVENT_ORDER_SHIPPED`, `EVENT_ORDER_DELIVERED` — callers (`PaymentServiceImpl`, `OrderServiceImpl`) reference constants, not raw strings.

## Exception Handling
- `IllegalArgumentException` (unknown event type in `create`) → 409 — `GlobalExceptionHandler`
- `EntityNotFoundException` (`delete` with unknown id) → 404 — `GlobalExceptionHandler`
- `Exception` in `dispatch` per-subscriber → caught, logged — not propagated

## Tests
- `WebhookServiceImplTest`: create valid request → saves + returns response; create unknown event type → throws; delete existing → active=false saved; delete not found → throws; dispatch active subscriber → correct POST with payload; dispatch no active subscribers → no HTTP call; dispatch HTTP failure → does not propagate — **6 cases** (+ 1 implicit no-propagate = 7)
- `AdminWebhookControllerTest`: POST no auth → 401; POST non-admin → 403; POST valid → 201 with body; POST HTTP url → 400; POST empty eventTypes → 400; GET → 200 list; DELETE existing → 204; DELETE not found → 404 — **8 cases**

## Files Index
**Domain:** src/main/java/com/example/eom/domain/WebhookSubscription.java
**Repository:** src/main/java/com/example/eom/repository/WebhookSubscriptionRepository.java
**Service Interface:** src/main/java/com/example/eom/service/WebhookService.java
**Service Impl:** src/main/java/com/example/eom/service/impl/WebhookServiceImpl.java
**Controller:** src/main/java/com/example/eom/controller/admin/AdminWebhookController.java
**DTO:** src/main/java/com/example/eom/dto/webhook/CreateWebhookRequest.java, src/main/java/com/example/eom/dto/webhook/WebhookResponse.java, src/main/java/com/example/eom/dto/webhook/WebhookPayload.java
**Config:** src/main/java/com/example/eom/config/WebhookConfig.java
**Migration:** src/main/resources/db/migration/V8__create_webhook_subscriptions.sql
**Tests:** src/test/java/com/example/eom/service/WebhookServiceImplTest.java, src/test/java/com/example/eom/controller/AdminWebhookControllerTest.java
