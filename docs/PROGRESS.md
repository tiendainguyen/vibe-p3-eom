# Progress — E-commerce Order Management API

> Source: assignment.md
> URDs: [docs/urd/](urd/)

| ID | Feature | Status | Effort | URD |
|----|---------|--------|--------|-----|
| T-001 | Infrastructure Setup | `pending` | L | [T-001](urd/T-001-infrastructure-setup.md) |
| T-010 | Authentication & Users | `pending` | L | [T-010](urd/T-010-authentication-users.md) |
| T-020 | Product Catalog | `pending` | L | [T-020](urd/T-020-product-catalog.md) |
| T-030 | Inventory Management | `pending` | M | [T-030](urd/T-030-inventory-management.md) |
| T-040 | Shopping Cart | `pending` | M | [T-040](urd/T-040-shopping-cart.md) |
| T-050 | Order Management | `pending` | L | [T-050](urd/T-050-order-management.md) |
| T-060 | Payment Processing | `pending` | L | [T-060](urd/T-060-payment-processing.md) |
| T-070 | Email Notifications | `pending` | M | [T-070](urd/T-070-email-notifications.md) |
| T-080 | Outgoing Webhooks | `pending` | M | [T-080](urd/T-080-outgoing-webhooks.md) |
| T-090 | Admin Operations | `pending` | M | [T-090](urd/T-090-admin-operations.md) |

`pending` · `in_progress` · `completed` · `blocked`

## Dependency Graph

```
T-001 (Infrastructure)
  ├── T-010 (Auth & Users)
  │     └── T-040 (Cart) ──┐
  ├── T-020 (Products)      │
  │     └── T-030 (Inventory) ──┤
  │           └── T-040 (Cart)  │
  │                             ▼
  │                        T-050 (Orders)
  │                          ├── T-060 (Payments)
  │                          ├── T-070 (Email Notifications)
  │                          └── T-080 (Outgoing Webhooks)
  └── T-090 (Admin) ← T-010, T-020, T-050
```

## Critical Path

**T-001 → T-010 → T-020 → T-030 → T-040 → T-050 → T-060**

All tasks on this path must be completed in order before a full checkout demo is possible.
