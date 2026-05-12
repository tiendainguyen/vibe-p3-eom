---
task_id: T-070
feature: Email Notifications
status: pending
effort: M
dependencies: [T-050]
---

# URD — Email Notifications

## 1. Business Context
Customers expect email confirmations when orders are placed and shipped. Sending emails synchronously inside the order flow would slow down response times and couple order creation to email provider availability. RabbitMQ decouples the notification: `OrderService` publishes an event, and a separate consumer processes it asynchronously with retry on failure.

## 2. User Stories
- As a **shopper**, I want to receive an order confirmation email when my order is paid so that I have a record of my purchase.
- As a **shopper**, I want to receive a shipping notification email when my order is shipped so that I can track delivery.
- As the **system**, I want email sending decoupled from the order flow so that email failures don't roll back orders.
- As the **system**, I want failed email sends retried automatically so that transient SMTP errors don't result in missed notifications.

## 3. Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-070-1 | `OrderConfirmedEvent` published to RabbitMQ when order status changes to PAID | Must Have |
| FR-070-2 | `OrderShippedEvent` published to RabbitMQ when order status changes to SHIPPED | Should Have |
| FR-070-3 | `EmailNotificationConsumer` listens on queue and sends email via JavaMailSender | Must Have |
| FR-070-4 | Email contains order ID, items summary, and total amount | Must Have |
| FR-070-5 | Failed email delivery retried up to 3 times via RabbitMQ dead-letter queue | Should Have |
| FR-070-6 | SMTP credentials loaded from environment variables | Must Have |
| FR-070-7 | No email-sending logic in `OrderService` — only event publishing | Must Have |

## 4. API Endpoints
No REST endpoints for this feature — it is entirely event-driven.

| Component | Direction | Description |
|-----------|-----------|-------------|
| `NotificationPublisher` | Outbound | Publishes `OrderEvent` to RabbitMQ exchange |
| `EmailNotificationConsumer` | Inbound | Consumes from `notifications.queue`, sends email |

## 5. Data Entities
No new JPA entities.

**RabbitMQ topology:**
- Exchange: `orders.exchange` (topic, durable)
- Queue: `notifications.queue` (durable)
- Dead-letter queue: `notifications.dlq`
- Routing key: `order.confirmed`, `order.shipped`

**Flyway migration file:** none

## 6. Business Rules
1. Event publishing is fire-and-forget from `OrderService`'s perspective — publishing failure MUST NOT roll back the order transaction.
2. `OrderConfirmedEvent` is fired by `PaymentServiceImpl` after order status transitions to PAID (T-060 integration).
3. `OrderShippedEvent` is fired by `OrderServiceImpl` after status transitions to SHIPPED.
4. Email content is plain-text (HTML optional) — never includes sensitive data like passwords or payment card details.
5. Consumer logs both success and failure — failure details are not exposed externally.
6. SMTP credentials (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`) come from env vars only.

## 7. Implementation Layers
1. **Domain** — `OrderConfirmedEvent` record, `OrderShippedEvent` record (simple Java records, not JPA entities)
2. **Migration** — none
3. **Repository** — none
4. **Service**
   - `NotificationPublisher` — `publishOrderConfirmed(OrderConfirmedEvent)`, `publishOrderShipped(OrderShippedEvent)` via `RabbitTemplate`
   - `EmailNotificationConsumer` — `@RabbitListener` methods; uses `JavaMailSender`
5. **Controller** — none (event-driven only)
6. **Config**
   - `RabbitMQConfig` updated: declare `notifications.queue`, DLQ, binding
   - `MailConfig` — `JavaMailSender` bean (or Spring Boot auto-config via `application.yml`)
7. **Tests**
   - `NotificationPublisherTest` — verify `RabbitTemplate.convertAndSend` called with correct routing key and payload (mock RabbitTemplate)
   - `EmailNotificationConsumerTest` — verify `JavaMailSender.send` called with correct recipient and subject (mock JavaMailSender)

## 8. Acceptance Criteria
- [ ] After order transitions to PAID, `OrderConfirmedEvent` message appears in `notifications.queue`
- [ ] `EmailNotificationConsumer` calls `JavaMailSender.send` with correct `to` address
- [ ] Email body contains order ID and total amount
- [ ] Email failure does NOT cause order status to revert
- [ ] Failed message moves to DLQ after max retries (verifiable via RabbitMQ management UI)
- [ ] No SMTP credentials appear in source code or logs
- [ ] `NotificationPublisherTest` and `EmailNotificationConsumerTest` pass

## 9. Security Checklist
- [ ] SMTP credentials loaded from env vars: `${MAIL_USERNAME}`, `${MAIL_PASSWORD}`
- [ ] Email `to` address comes from the User entity (from DB) — never from the event payload directly (prevents email injection)
- [ ] Email body is sanitized — no raw user-supplied HTML
- [ ] No PII (passwords, payment details) included in event payload or email body

## 10. Non-Functional Notes
- RabbitMQ DLQ prevents infinite retry loops; failed messages park in DLQ for manual inspection.
- In test/CI environments, use `spring.mail.host=localhost` with Greenmail or mock `JavaMailSender`.
- `@RabbitListener` creates a separate thread pool — no risk of blocking the HTTP thread pool.
- For demo: use Mailtrap or a test SMTP server so email sending is verifiable without real credentials.
