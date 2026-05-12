---
task_id: T-080
feature: Outgoing Webhooks
status: pending
effort: M
dependencies: [T-050]
---

# URD — Outgoing Webhooks

## 1. Business Context
External systems (client ERP, fulfilment partners, analytics platforms) need real-time notifications when order states change. This feature allows subscribers to register a URL and event type; when a matching order event fires, the system delivers a signed HTTP POST to that URL. This makes the backend immediately integrable with any external system without custom connectors.

## 2. User Stories
- As an **external system operator**, I want to register a webhook URL for order events so that my system is notified automatically.
- As an **external system operator**, I want to deactivate or delete my webhook subscription so that I stop receiving events.
- As an **admin**, I want to list all registered webhooks so that I can audit integrations.
- As the **system**, I want to deliver webhook payloads with a HMAC signature so that receivers can verify authenticity.
- As the **system**, I want failed webhook deliveries retried so that transient network errors don't cause missed events.

## 3. Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-080-1 | `POST /api/webhooks/subscriptions` registers a webhook URL + event type | Must Have |
| FR-080-2 | `GET /api/webhooks/subscriptions` lists own subscriptions | Must Have |
| FR-080-3 | `DELETE /api/webhooks/subscriptions/{id}` removes a subscription | Must Have |
| FR-080-4 | On `order.confirmed` and `order.shipped` events, deliver signed POST to all matching active subscribers | Must Have |
| FR-080-5 | Payload signed with HMAC-SHA256 using a per-subscription secret; signature in `X-Signature-256` header | Must Have |
| FR-080-6 | Delivery attempt retried up to 3 times with exponential backoff on non-2xx response | Should Have |
| FR-080-7 | `GET /api/admin/webhooks/subscriptions` lists all subscriptions across all users | Should Have |
| FR-080-8 | Webhook secret generated server-side and returned once at creation — never stored in plain text | Must Have |

## 4. API Endpoints
| Method | Path | Auth | Request Body | Response | Description |
|--------|------|------|--------------|----------|-------------|
| POST | /api/webhooks/subscriptions | Bearer JWT | `CreateWebhookRequest` | `WebhookSubscriptionResponse` | Register webhook subscription |
| GET | /api/webhooks/subscriptions | Bearer JWT | — | `List<WebhookSubscriptionResponse>` | List own subscriptions |
| DELETE | /api/webhooks/subscriptions/{id} | Bearer JWT | — | `204 No Content` | Remove subscription |
| GET | /api/admin/webhooks/subscriptions | Bearer JWT (ADMIN) | — | `List<WebhookSubscriptionResponse>` | List all subscriptions |

## 5. Data Entities
**WebhookSubscription**
- `id` (Long, PK, auto-increment)
- `user` → User (ManyToOne, not null)
- `url` (String, not null, max 500) — target URL
- `eventType` (Enum: ORDER_CONFIRMED / ORDER_SHIPPED, not null)
- `secretHash` (String, not null) — BCrypt hash of the generated secret
- `active` (Boolean, not null, default true)
- `createdAt` (Instant, not null)

**Flyway migration file:** `V8__create_webhook_subscriptions.sql`

## 6. Business Rules
1. Webhook secret is generated server-side (UUID or 32-byte random hex) and returned **once** in the creation response — it cannot be retrieved again.
2. Secret is stored as a BCrypt hash — used only for display (original secret returned once); actual HMAC signing uses the plain secret stored temporarily in memory during delivery (or a separate `signingKey` field if needed).

   > **Implementation note:** Store the plain secret hashed for display but keep a `signingKey` column (plain, not logged) for HMAC signing, since BCrypt is not reversible. Alternatively, store the plain signing key encrypted at rest if key management is available.

3. Delivery to subscriber URL uses `RestTemplate` or `WebClient` in a `@Async` method — must not block the order flow.
4. Only active (`active=true`) subscriptions receive deliveries.
5. A user may only manage their own subscriptions; admin can list all.
6. Max 10 active subscriptions per user (configurable via `app.webhook.max-per-user`).

## 7. Implementation Layers
1. **Domain** — `WebhookSubscription` entity, `WebhookEventType` enum
2. **Migration** — `V8__create_webhook_subscriptions.sql`
3. **Repository** — `WebhookSubscriptionRepository` with `findByUserIdAndActiveTrue(Long userId)`, `findByEventTypeAndActiveTrue(WebhookEventType)`
4. **Service**
   - `WebhookService` interface: `register(Long userId, CreateWebhookRequest)`, `listByUser(Long userId)`, `delete(Long userId, Long id)`, `dispatch(WebhookEventType, Object payload)`
   - `WebhookServiceImpl` — `dispatch` is `@Async`, HMAC-SHA256 signing, retry logic
5. **Controller**
   - `WebhookSubscriptionController` — `/api/webhooks/subscriptions/**`
   - `AdminWebhookController` — `/api/admin/webhooks/subscriptions` (ADMIN only)
6. **Config** — `@EnableAsync` in `AsyncConfig`; `RestTemplate` bean for outbound HTTP
7. **Tests**
   - `WebhookServiceImplTest` — register, list, delete, dispatch (mock RestTemplate), HMAC signature generation
   - `WebhookSubscriptionControllerTest` — POST 201, GET 200, DELETE 204, 403 cross-user delete

## 8. Acceptance Criteria
- [ ] `POST /api/webhooks/subscriptions` returns `201` with subscription ID and one-time secret
- [ ] Secret cannot be retrieved after creation (GET response omits it)
- [ ] On order PAID: `WebhookService.dispatch(ORDER_CONFIRMED, payload)` is called and POST reaches the subscriber URL
- [ ] Outbound POST includes `X-Signature-256` header with valid HMAC-SHA256 signature
- [ ] Non-2xx response from subscriber triggers retry (up to 3 attempts)
- [ ] `DELETE /api/webhooks/subscriptions/{id}` for another user's subscription returns `404`
- [ ] More than 10 active subscriptions per user returns `400`
- [ ] All webhook tests pass

## 9. Security Checklist
- [ ] Webhook signing key never logged
- [ ] `url` field validated as a valid HTTP/HTTPS URL (`@Pattern` or `@URL`)
- [ ] HMAC-SHA256 used for payload signing — not MD5 or SHA1
- [ ] Subscription secret returned only once at creation — not stored reversibly
- [ ] User can only delete their own subscriptions (userId check in service)
- [ ] Admin list endpoint protected with `@PreAuthorize("hasRole('ADMIN')")`

## 10. Non-Functional Notes
- `@Async` dispatch prevents webhook delivery from blocking the HTTP response thread.
- Retry with exponential backoff: attempt 1 immediate, attempt 2 after 5s, attempt 3 after 25s (basic implementation; for production use Spring Retry or RabbitMQ DLQ).
- For demo: use `https://webhook.site` as the subscriber URL to observe deliveries in real time.
- Circuit breaker (Resilience4j) not in scope — simple retry is sufficient for demo.
