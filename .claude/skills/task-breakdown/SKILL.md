---
name: task-breakdown
description: Analyzes assignment.md and breaks the project into business-feature tasks (one task per feature). For each task generates a URD saved to docs/urd/T-XXX-<feature>.md. Also creates docs/PROGRESS.md as a minimal status board. Use when the user runs /task-breakdown.
---

# Task Breakdown Skill

## Purpose
Read `assignment.md` and produce two outputs:
1. `docs/urd/T-XXX-<feature>.md` — one User Requirements Document per business feature
2. `docs/PROGRESS.md` — minimal status board (ID, feature name, status, URD link)

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

### Phase 4: Write URDs
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

### Phase 5: Write PROGRESS.md (status board)
Save to `docs/PROGRESS.md`. Minimal — only what changes as work progresses:

```markdown
# Progress — [Project Name]

> Source: assignment.md
> URDs: [docs/urd/](urd/)

| ID | Feature | Status |
|----|---------|--------|
| T-001 | Infrastructure Setup | `pending` |
| T-010 | Feature Name | `pending` |
[One row per feature]

`pending` · `in_progress` · `completed` · `blocked`
```

### Phase 6: Validation Checklist
Before completing:
- [ ] Every feature from `assignment.md` has a URD in `docs/urd/`
- [ ] Each URD has a complete API endpoint table
- [ ] Each URD has testable acceptance criteria
- [ ] `PROGRESS.md` has one row per task
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
1. `docs/urd/T-XXX-<feature>.md` — one URD per task
2. `docs/PROGRESS.md` — status board

Report a summary to the user: total tasks, total estimated effort, critical path.
