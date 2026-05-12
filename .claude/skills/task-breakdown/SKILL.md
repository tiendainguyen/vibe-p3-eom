---
name: task-breakdown
description: Analyzes assignment.md and breaks the project into business-feature tasks (one task per feature). For each task generates a URD saved to docs/urd/T-XXX-<feature>.md. Also generates docs/SRS.md and updates docs/TASK_BREAKDOWN.md as the master index. Use when the user runs /task-breakdown.
---

# Task Breakdown Skill

## Purpose
Read `assignment.md` and produce three outputs:
1. `docs/SRS.md` — full Software Requirements Specification for the project
2. `docs/urd/T-XXX-<feature>.md` — one User Requirements Document per business feature
3. `docs/TASK_BREAKDOWN.md` — master index linking every task to its URD

Tasks are split **by business feature only** — not by technical layer. One task = one cohesive business capability. Implementation layers (Domain → Repository → Service → Controller) are detailed inside each URD, not at the breakdown level.

---

## Instructions

### Phase 1: Read Context
1. Read `assignment.md` — extract all features, integrations, and goals
2. Read `docs/ARCHITECTURE.md` — note existing design decisions
3. Read `CLAUDE.md` — note project conventions and stack
4. Read `.claude/rules/security.md` — note security constraints

### Phase 2: Identify Business Features
Extract a flat list of independent business capabilities from `assignment.md`. Each feature must:
- Represent a single, coherent slice of user or business value
- Be implementable end-to-end by one developer in one session (≤ XL effort)
- Have clear boundaries — not overlap with another feature

Typical features for an e-commerce API (adapt to the actual assignment):
- Infrastructure Setup (base project wiring — always first)
- Authentication & Users
- Product Catalog
- Inventory Management
- Shopping Cart
- Order Management
- Payment Processing
- Email Notifications
- Outgoing Webhooks
- Admin Operations

### Phase 3: Assign Task IDs and Effort
- Task ID format: `T-XXX` starting at `T-001` for infrastructure, then `T-010`, `T-020`... per feature (gaps allow future insertion)
- Effort: S (< 1h) · M (1–2h) · L (2–4h) · XL (4–8h)
- Note which tasks are blocked by others (dependencies)

### Phase 4: Write SRS
Save to `docs/SRS.md`. Include:

```markdown
# Software Requirements Specification — [Project Name]

> Version: 1.0  
> Date: YYYY-MM-DD  
> Source: assignment.md

## 1. Project Overview
One paragraph: what the system does, who uses it, the business value.

## 2. Stakeholders & User Roles
| Role | Description | Permissions |
|------|-------------|-------------|
| Guest | Unauthenticated visitor | Browse catalog only |
| Customer | Registered user | Cart, orders, payments |
| Admin | Staff operator | Order management, inventory, webhooks |

## 3. Functional Requirements
One section per feature. Use requirement IDs (FR-XXX).

### 3.1 Feature Name
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-001 | ... | Must Have |

[Repeat for each feature]

## 4. Non-Functional Requirements
| ID | Category | Requirement |
|----|----------|-------------|
| NFR-001 | Security | All secrets via environment variables — never hardcoded |
| NFR-002 | Security | JWT stateless auth — no server sessions |
| NFR-003 | Performance | Product catalog reads cached in Redis |
| NFR-004 | Reliability | Inventory mutations use pessimistic locking |
| NFR-005 | Async | Email notifications sent via RabbitMQ queue |
[Add project-specific NFRs]

## 5. External Integrations
| System | Purpose | Auth Method |
|--------|---------|-------------|

## 6. Technology Stack
| Concern | Technology | Reason |
|---------|------------|--------|

## 7. Data Model Overview
High-level entity relationships (text diagram).

## 8. API Design Principles
- RESTful conventions: plural nouns, HTTP verbs, status codes
- All responses wrapped in consistent envelope (or plain DTO — document choice)
- Pagination via `page` + `size` query params
- Errors follow `ErrorResponseDTO` shape from GlobalExceptionHandler

## 9. Security Architecture
- Auth flow summary
- Which endpoints are public vs. authenticated vs. admin-only
- Input validation strategy
- Secrets management

## 10. Constraints & Assumptions
List any known constraints (team size, timeline, deployment target).
```

### Phase 5: Write URDs
For each feature task, save `docs/urd/T-XXX-<kebab-feature-name>.md`:

```markdown
---
task_id: T-XXX
feature: Feature Name
status: pending
effort: M
dependencies: [T-YYY, T-ZZZ]
---

# URD — Feature Name

## 1. Business Context
Why this feature exists. What user/business problem it solves. One short paragraph.

## 2. User Stories
- As a **[role]**, I want to **[action]** so that **[benefit]**
[3–8 user stories]

## 3. Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-XXX-1 | ... | Must Have / Should Have / Nice to Have |
[Derived from user stories — be specific and testable]

## 4. API Endpoints
| Method | Path | Auth | Request Body | Response | Description |
|--------|------|------|--------------|----------|-------------|
| POST | /api/... | Bearer JWT | `CreateXxxRequest` | `XxxResponse` | ... |
[All endpoints for this feature — admin endpoints listed separately]

## 5. Data Entities
List JPA entities involved, key fields, and relationships:

**EntityName**
- `id` (Long, PK)
- `field` (type, constraints)
- `relationship` → OtherEntity (OneToMany/ManyToOne/etc.)

[Flyway migration file: V<N>__create_<table>.sql]

## 6. Business Rules
Numbered, testable constraints:
1. [Rule 1]
2. [Rule 2]

## 7. Implementation Layers
Ordered list of what to build (for use by /feat-init and /feat-apply):
1. **Domain** — entities, enums, value objects
2. **Migration** — Flyway SQL scripts
3. **Repository** — Spring Data interfaces + custom queries
4. **Service** — interface + implementation (list methods)
5. **Controller** — endpoint classes + DTOs
6. **Config** — any Spring config beans needed
7. **Tests** — unit + integration test classes

## 8. Acceptance Criteria
- [ ] [Testable condition — verifiable by a reviewer]
[5–10 criteria]

## 9. Security Checklist
- [ ] Secrets via env vars — never hardcoded
- [ ] All inputs validated with Bean Validation
- [ ] Parameterized queries only (Spring Data JPA)
- [ ] userId extracted from JWT — never from request body
- [ ] [Feature-specific items]

## 10. Non-Functional Notes
Performance, caching, transactional requirements specific to this feature.
```

### Phase 6: Write TASK_BREAKDOWN.md (master index)
Save to `docs/TASK_BREAKDOWN.md`:

```markdown
# Task Breakdown — [Project Name]

> Generated: YYYY-MM-DD  
> Source: assignment.md  
> SRS: [docs/SRS.md](SRS.md)

## Summary
- **Total Features:** XX
- **Total Estimated Effort:** XX–XX hours
- **Critical Path:** T-001 → T-010 → T-020 → T-030 → T-040 → ...

## Feature Tasks

| ID | Feature | Status | Effort | Dependencies | URD |
|----|---------|--------|--------|--------------|-----|
| T-001 | Infrastructure Setup | `pending` | M | — | [URD](urd/T-001-infrastructure.md) |
| T-010 | Authentication & Users | `pending` | L | T-001 | [URD](urd/T-010-auth.md) |
[One row per feature]

## Status Legend
`pending` · `in_progress` · `completed` · `blocked`

## Dependency Graph
[ASCII or text diagram showing the critical path]
```

### Phase 7: Validation Checklist
Before completing:
- [ ] Every feature from `assignment.md` has a task + URD
- [ ] Each URD has a complete API endpoint table
- [ ] Each URD has testable acceptance criteria
- [ ] SRS covers all functional + non-functional requirements
- [ ] `TASK_BREAKDOWN.md` links to every URD file
- [ ] No task has a circular dependency
- [ ] Infrastructure is always T-001 (no dependencies)

---

## Anti-patterns to Avoid
- Do NOT split a feature by technical layer (e.g., don't make "Product Entity" a separate task from "Product Service")
- Do NOT create tasks that violate security rules
- Do NOT make tasks so large they can't be completed in one session (split into sub-features if needed)
- Do NOT copy the exact same acceptance criteria to every URD — make them feature-specific

---

## Output
1. `docs/SRS.md` — project-wide requirements specification
2. `docs/urd/T-XXX-<feature>.md` — one URD per task
3. `docs/TASK_BREAKDOWN.md` — master index with status tracking

Report a summary to the user: total tasks, total estimated effort, critical path.
