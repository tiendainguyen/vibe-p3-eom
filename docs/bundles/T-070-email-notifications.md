---
feature: Async Email Notifications via RabbitMQ
task: T-070
issue: #8
archived: 2026-05-13
files:
  - src/main/java/com/example/eom/dto/notification/OrderConfirmedEvent.java
  - src/main/java/com/example/eom/dto/notification/OrderShippedEvent.java
  - src/main/java/com/example/eom/service/impl/EmailNotificationConsumer.java
  - src/main/java/com/example/eom/config/RabbitMQConfig.java
  - src/main/java/com/example/eom/service/NotificationPublisher.java
  - src/main/java/com/example/eom/service/impl/NotificationPublisherImpl.java
  - src/main/java/com/example/eom/service/impl/OrderServiceImpl.java (publishOrderShipped hook)
  - src/main/java/com/example/eom/dto/order/UpdateOrderStatusRequest.java (trackingInfo field)
  - src/test/java/com/example/eom/service/NotificationPublisherImplTest.java
  - src/test/java/com/example/eom/service/EmailNotificationConsumerTest.java
  - src/test/java/com/example/eom/service/OrderServiceImplTest.java (updated for new deps)
  - src/test/java/com/example/eom/controller/OrderControllerTest.java (updated constructor calls)
---

# Bundle: Async Email Notifications via RabbitMQ

## API Surface
No new HTTP endpoints — feature is entirely event-driven via RabbitMQ.

## Logic Map

### RabbitMQConfig (file: config/RabbitMQConfig.java)
- Declares `DirectExchange("eom.notifications")`, two durable queues (`eom.order.confirmed`, `eom.order.shipped`), and bindings with routing keys `order.confirmed` / `order.shipped`
- Exposes constants: `EXCHANGE`, `ORDER_CONFIRMED_QUEUE`, `ORDER_CONFIRMED_KEY`, `ORDER_SHIPPED_QUEUE`, `ORDER_SHIPPED_KEY` — consumed by publisher and consumer
- Registers `Jackson2JsonMessageConverter` bean so RabbitTemplate serializes events as JSON

### NotificationPublisherImpl (file: service/impl/NotificationPublisherImpl.java)
- `publishOrderConfirmed(orderId)`: loads Order + User + OrderItems from repos → builds `OrderConfirmedEvent(orderId, userId, userEmail, totalAmount, itemCount)` → `rabbitTemplate.convertAndSend(EXCHANGE, ORDER_CONFIRMED_KEY, event)` → logs
- `publishOrderShipped(orderId, trackingInfo)`: loads Order + User → builds `OrderShippedEvent(orderId, userId, userEmail, trackingInfo)` → `rabbitTemplate.convertAndSend(EXCHANGE, ORDER_SHIPPED_KEY, event)` → logs
- `loadOrder(orderId)` / `loadUser(userId)`: private helpers, throw `EntityNotFoundException` if not found

### EmailNotificationConsumer (file: service/impl/EmailNotificationConsumer.java)
- `handleOrderConfirmed(event)`: `@RabbitListener(queues = ORDER_CONFIRMED_QUEUE)` → builds `SimpleMailMessage` to `event.userEmail()` with subject "Order Confirmed — #orderId" and body listing itemCount + total → `mailSender.send()` → catches all exceptions, logs error without rethrowing (prevents poison-message loop)
- `handleOrderShipped(event)`: `@RabbitListener(queues = ORDER_SHIPPED_QUEUE)` → builds shipping email; includes `Tracking:` line only if `trackingInfo` is non-null and non-blank → same catch-and-log error handling
- `buildConfirmedBody(event)` / `buildShippedBody(event)`: private helpers producing plain-text email bodies

### OrderServiceImpl (cross-feature hook, file: service/impl/OrderServiceImpl.java)
- `updateStatus(orderId, request)`: after saving SHIPPED status, calls `notificationPublisher.publishOrderShipped(orderId, request.trackingInfo())` — only fires for SHIPPED transition, not other status advances

## Business Rules
1. `publishOrderConfirmed` is called by `PaymentServiceImpl` after `payment_intent.succeeded` webhook — outside any DB transaction, so no risk of publishing before commit.
2. `publishOrderShipped` is called inside `OrderServiceImpl.updateStatus` `@Transactional` immediately after `orderRepository.save()` — message may be sent before commit in edge cases, but acceptable for this scope.
3. Email consumer catches all exceptions and logs with orderId — failed emails are dropped (not requeued), preventing poison-message loops; orderId in log allows manual retry.
4. Email addresses are always taken from the `User` entity looked up by `userId` — never from the event payload itself or from any client input.
5. No financial detail beyond `orderTotal` is included in emails; no passwords, tokens, or payment method details.
6. `MAIL_HOST`, `MAIL_USER`, `MAIL_PASSWORD` loaded from env vars only — never hardcoded.

## Key Decisions
- **Plain-text emails, no Thymeleaf**: avoids adding a template dependency; body built with string concatenation in private helpers.
- **Catch-and-log in consumer (no rethrow)**: prevents a failing SMTP server from causing infinite requeue; orderId logged for manual recovery.
- **`Jackson2JsonMessageConverter` bean**: enables automatic JSON serialization/deserialization of event records — consumer receives a typed object, not a raw byte array.
- **`NotificationPublisher` interface kept as service abstraction**: `PaymentServiceImpl` and `OrderServiceImpl` depend on the interface, not on RabbitMQ directly — allows mocking in tests without a broker.
- **`trackingInfo` added as optional field to `UpdateOrderStatusRequest`**: backward-compatible (null if not provided); passed through to `OrderShippedEvent` and shown in email only if present.

## Exception Handling
- `EntityNotFoundException` in publisher (order/user not found) → propagates up to caller — `GlobalExceptionHandler` maps to 404 if triggered from a controller context; in webhook context it logs as unhandled
- `MailException` (SMTP failure) in consumer → caught, logged with orderId — message is NOT requeued

## Tests
- `NotificationPublisherImplTest`: covers `publishOrderConfirmed` (correct event payload, exchange, routing key), `publishOrderShipped` with tracking, `publishOrderShipped` with null tracking — 3 cases; uses `ArgumentCaptor` with `eq()` matchers for all 3 RabbitTemplate args
- `EmailNotificationConsumerTest`: covers confirmed email (to/subject/body), shipped email with tracking (body includes tracking), shipped email without tracking (no "Tracking:" line), mail failure does not propagate — 4 cases

## Files Index
**DTO (events):** src/main/java/com/example/eom/dto/notification/OrderConfirmedEvent.java, src/main/java/com/example/eom/dto/notification/OrderShippedEvent.java
**DTO (modified):** src/main/java/com/example/eom/dto/order/UpdateOrderStatusRequest.java
**Service Interface:** src/main/java/com/example/eom/service/NotificationPublisher.java
**Service Impl:** src/main/java/com/example/eom/service/impl/NotificationPublisherImpl.java, src/main/java/com/example/eom/service/impl/EmailNotificationConsumer.java
**Service (cross-feature):** src/main/java/com/example/eom/service/impl/OrderServiceImpl.java
**Config:** src/main/java/com/example/eom/config/RabbitMQConfig.java
**Tests:** src/test/java/com/example/eom/service/NotificationPublisherImplTest.java, src/test/java/com/example/eom/service/EmailNotificationConsumerTest.java
