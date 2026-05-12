---
task_id: T-060
feature: Payment Processing
status: pending
effort: L
dependencies: [T-050]
---

# URD — Payment Processing

## 1. Business Context
Payments use Stripe PaymentIntents with a split-confirmation model: the backend creates the intent and returns a `clientSecret`, the frontend/Postman confirms it directly with Stripe, and Stripe calls back via a signed webhook to confirm success. This decouples the payment confirmation flow from the API server and handles failures and retries robustly.

## 2. User Stories
- As a **shopper**, I want to initiate payment for my order so that I receive a client secret to confirm payment.
- As a **shopper**, I want my order marked as PAID automatically after payment confirmation so that I don't have to do anything extra.
- As the **system**, I want to verify the Stripe webhook signature so that only legitimate Stripe events update orders.
- As the **system**, I want to deduct inventory on payment confirmation so that reservations become permanent.
- As the **system**, I want to handle Stripe refund events so that REFUNDED orders are tracked correctly.

## 3. Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-060-1 | `POST /api/payments/intent` creates a Stripe PaymentIntent for the authenticated user's PENDING order | Must Have |
| FR-060-2 | Returns `{ "clientSecret": "..." }` for the frontend to confirm payment | Must Have |
| FR-060-3 | `POST /api/webhooks/stripe` receives Stripe events; verifies webhook signature before processing | Must Have |
| FR-060-4 | `payment_intent.succeeded` event: transitions order to PAID, deducts inventory, fires notification | Must Have |
| FR-060-5 | `payment_intent.payment_failed` event: logs failure; order remains PENDING | Should Have |
| FR-060-6 | `charge.refunded` event: transitions order to REFUNDED | Should Have |
| FR-060-7 | Invalid or unverified webhook signature returns `400` and is logged | Must Have |
| FR-060-8 | Stripe secret key loaded from `${STRIPE_SECRET_KEY}` env var — never hardcoded | Must Have |

## 4. API Endpoints
| Method | Path | Auth | Request Body | Response | Description |
|--------|------|------|--------------|----------|-------------|
| POST | /api/payments/intent | Bearer JWT | `CreatePaymentIntentRequest` | `PaymentIntentResponse` | Create Stripe PaymentIntent for an order |
| POST | /api/webhooks/stripe | None (Stripe signature header) | Raw Stripe event body | `200 OK` | Stripe webhook receiver |

## 5. Data Entities
No new entities. Updates to existing:

**Order** (existing)
- `stripePaymentIntentId` populated when intent is created
- `status` transitions: PENDING → PAID (on `payment_intent.succeeded`), PAID → REFUNDED (on `charge.refunded`)

**Flyway migration file:** none (uses existing `orders` table columns)

## 6. Business Rules
1. `POST /api/payments/intent` only valid for orders in PENDING status owned by the authenticated user.
2. PaymentIntent `amount` is derived from `Order.totalAmount` (converted to cents). Amount is NEVER trusted from the request body.
3. Stripe webhook signature MUST be verified using `Webhook.constructEvent(payload, sigHeader, ${STRIPE_WEBHOOK_SECRET})` before any processing.
4. Webhook handler MUST be idempotent — processing the same `payment_intent.succeeded` twice must not double-deduct inventory or double-fire notifications.
5. `stripePaymentIntentId` stored on the order after intent creation to support idempotency checks.
6. Stripe key and webhook secret are loaded from environment variables only.

## 7. Implementation Layers
1. **Domain** — no new entities; `OrderStatus` enum already has PAID and REFUNDED
2. **Migration** — none
3. **Repository** — `OrderRepository.findByStripePaymentIntentId(String id)` for webhook lookup
4. **Service**
   - `PaymentService` interface: `createPaymentIntent(Long userId, Long orderId)`, `handleWebhookEvent(String payload, String sigHeader)`
   - `PaymentServiceImpl` — Stripe SDK calls, idempotency check, delegates to `OrderService` and `InventoryService`
5. **Controller**
   - `PaymentController` — `/api/payments/**`
   - `StripeWebhookController` — `/api/webhooks/stripe`; uses `@RequestBody byte[]` to preserve raw body for signature verification
6. **Config** — `StripeConfig` bean initializing `Stripe.apiKey` from env var
7. **Tests**
   - `PaymentServiceImplTest` — create intent happy path, order not found, order not PENDING, idempotent webhook handling
   - `PaymentControllerTest` — POST /intent 200/400/403
   - `StripeWebhookControllerTest` — valid signature 200, invalid signature 400

## 8. Acceptance Criteria
- [ ] `POST /api/payments/intent` for a valid PENDING order returns `200` with `clientSecret`
- [ ] `POST /api/payments/intent` for a non-PENDING order returns `409`
- [ ] `POST /api/payments/intent` for another user's order returns `404`
- [ ] Stripe webhook with valid signature and `payment_intent.succeeded` → order status becomes PAID
- [ ] After PAID: inventory `reserved` decreases (deducted)
- [ ] Duplicate `payment_intent.succeeded` event does NOT deduct inventory twice
- [ ] Webhook with invalid/missing signature returns `400`
- [ ] No Stripe API key appears in any source file or log
- [ ] All payment tests pass

## 9. Security Checklist
- [ ] `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` loaded from env vars only
- [ ] Webhook signature verified before any state change
- [ ] Payment amount computed server-side from order total — never from client input
- [ ] `userId` extracted from JWT for `/api/payments/intent` — never from body
- [ ] Raw webhook body preserved as `byte[]` for Stripe signature verification (not parsed early)
- [ ] No Stripe key or secret in application.yml (only `${ENV_VAR}` placeholders)

## 10. Non-Functional Notes
- Webhook endpoint must use `@RequestBody byte[]` or `HttpServletRequest.getInputStream()` to preserve raw bytes — Spring's default body parsing would break signature verification.
- Stripe SDK: `com.stripe:stripe-java` — add to pom.xml.
- PaymentIntent currency defaults to `usd`; configurable via `app.payment.currency` if needed.
- Stripe test mode keys (`sk_test_...`) should be used in development; never use live keys in CI.
