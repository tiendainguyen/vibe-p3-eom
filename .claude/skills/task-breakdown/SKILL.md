---
name: task-breakdown
description: Analyzes assignment.md and breaks the project into business-feature tasks. Creates one GitHub issue per feature with full URD content. Use when the user runs /task-breakdown.
---

# Task Breakdown Skill

## Purpose
Read `assignment.md` → identify business features → create one GitHub issue per feature containing the full URD.

Issues are the source of truth for requirements. No local docs/urd/ files needed.

---

## Instructions

### Phase 1: Read Context
1. Read `assignment.md` — all features, integrations, goals
2. Read `docs/ARCHITECTURE.md` — existing design decisions and stack
3. Read `CLAUDE.md` — conventions
4. Run `gh repo view --json nameWithOwner` to confirm GitHub remote is set

### Phase 2: Identify Business Features
Extract a flat list of independent business capabilities. Each feature must:
- Represent one cohesive slice of user/business value
- Be implementable end-to-end in one session (≤ XL effort)
- Have clear, non-overlapping boundaries

Task ID format: `T-001` for infrastructure, then `T-010`, `T-020`, `T-030`... (gaps allow future insertion).

Effort scale: `S` < 1h · `M` 1–2h · `L` 2–4h · `XL` 4–8h

### Phase 3: Create GitHub Issues
For each feature, create one issue. The issue body IS the URD — write it directly in the issue:

```bash
gh issue create \
  --title "T-XXX: Feature Name" \
  --label "feature" \
  --body "$(cat <<'BODY'
## URD — Feature Name

**Task ID:** T-XXX | **Effort:** M | **Depends on:** T-YYY

### Business Context
Why this feature exists. What problem it solves.

### User Stories
- As a **[role]**, I want to **[action]** so that **[benefit]**

### Functional Requirements
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-XXX-1 | ... | Must Have |

### API Endpoints
| Method | Path | Auth | Request | Response |
|--------|------|------|---------|----------|
| POST | /api/... | public | CreateXxxRequest | XxxResponse |

### Data Entities
**EntityName** — fields: id, field1, field2 → Flyway: V<N>__create_<table>.sql

### Business Rules
1. [Rule 1]
2. [Rule 2]

### Implementation Layers
1. Domain — entities, enums
2. Migration — Flyway SQL
3. Repository — Spring Data interfaces
4. Service — interface + impl (list key methods)
5. Controller — endpoints + DTOs
6. Tests — unit tests per layer

### Acceptance Criteria
- [ ] [Testable condition]

### Security Checklist
- [ ] All inputs validated with Bean Validation
- [ ] Secrets via env vars only
- [ ] userId from JWT principal — never from request body
BODY
)"
```

### Phase 4: Validate
- [ ] Every feature from assignment.md has a GitHub issue
- [ ] No circular dependencies
- [ ] T-001 infrastructure has no dependencies

---

## Output
Report to user: total tasks, total effort estimate, critical path, and links to the created issues.
