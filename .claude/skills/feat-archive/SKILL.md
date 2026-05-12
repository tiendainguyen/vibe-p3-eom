---
name: feat-archive
description: Archives a completed task by creating a structured archive in docs/archive/T-XXX/, updating ARCHITECTURE.md and security.md if needed, and maintaining or creating feature bundles in docs/bundles/. Use when a task is finished and the user runs /feat-archive T-XXX to preserve implementation details, key decisions, and cross-references for future reference.
---

# Feature Archive Skill

## Purpose
Archive completed task documentation, update architectural records, and bundle relevant documentation for future reference or task updates.

## Instructions

You are a technical documentation specialist archiving completed work and maintaining project knowledge base.

### Phase 1: Gather Task Context
1. Read `docs/plans/[TASK-ID]-plan.md` for the completed task
2. Read `docs/TASK_BREAKDOWN.md` to understand task relationships
3. Read `docs/ARCHITECTURE.md` for current system state
4. Identify all files created/modified during the task implementation

### Phase 2: Create Task Archive
Create comprehensive archive in `docs/archive/[TASK-ID]/`:

#### 2.1 Archive Structure
```
docs/archive/T-XXX/
├── summary.md              # Executive summary
├── implementation.md       # Detailed implementation notes
├── code-snippets.md        # Key code examples
├── tests.md                # Test coverage summary
└── references.md           # Related files and tasks
```

#### 2.2 Summary.md Content
```markdown
# Task T-XXX Archive — [Task Description]

> Completed: YYYY-MM-DD
> Feature Area: [e.g., Product Catalog]
> Layer: [e.g., Service]
> Effort: [Estimated vs. Actual]

## What Was Built
[Brief description of what was implemented]

## Key Files
- src/main/java/.../[File1].java — [Purpose]
- src/main/java/.../[File2].java — [Purpose]
- src/test/java/.../[TestFile].java — [Purpose]

## Dependencies Satisfied
This task unblocked:
- T-XXX: [Description]
- T-YYY: [Description]

## Architecture Impact
[How this task affected the overall system architecture]

## Security Notes
[Any security considerations or compliance notes]

## Lessons Learned
[What went well, what could be improved]
```

#### 2.3 Implementation.md Content
Detailed technical notes:
- Design decisions made during implementation
- Deviations from the original plan (with justifications)
- Challenges encountered and solutions
- Code patterns used (with examples)
- Configuration changes made

#### 2.4 Code-snippets.md Content
Extract and document key code examples:
- Entity definitions with annotations
- Service method signatures and key logic
- Repository query methods
- Controller endpoints with validation
- DTO structures

Include brief explanations of why each snippet is important for future reference.

#### 2.5 Tests.md Content
Document test coverage:
- List of test classes created
- Key test cases and what they verify
- Integration points tested
- Test data patterns used
- Coverage metrics (if available)

#### 2.6 References.md Content
Cross-references:
- Related tasks (dependencies, dependents)
- Related documentation (ARCHITECTURE.md sections)
- External resources consulted
- Configuration files modified
- Database migrations created

### Phase 3: Update Architecture Documentation
Analyze if `docs/ARCHITECTURE.md` needs updates:

#### 3.1 System Diagram
- If new components added, update the diagram
- If new external integrations added (Stripe, RabbitMQ, Redis), show them

#### 3.2 Layer Responsibilities Table
- Add examples from this task if they clarify boundaries
- Document new patterns established

#### 3.3 Key Boundaries
- Add new boundary rules if this task established them
- Example: "Payment processing always goes through PaymentService — no direct Stripe calls from controllers"

#### 3.4 Key Technical Decisions
Add new entries to the decisions table:
| Decision | Choice | Reason |
|----------|--------|--------|
| Inventory Reservations | Redis with TTL | Fast expiration, prevents overselling |

### Phase 4: Update Security Documentation
If task introduced new security patterns, update `.claude/rules/security.md`:

Add to relevant sections:
```markdown
## [Relevant Section]
- [New security pattern]: [Description]
  Example: src/main/java/.../[File].java:123
```

### Phase 5: Create Task Bundle
Create a standalone bundle document for this feature area in `docs/bundles/`:

#### 5.1 Bundle Structure
One bundle per major feature area (e.g., `docs/bundles/product-catalog-bundle.md`)

#### 5.2 Bundle Content
```markdown
# Feature Bundle — [Feature Area]

> Last updated: YYYY-MM-DD
> Tasks included: T-XXX, T-YYY, T-ZZZ

## Overview
[What this feature area does]

## Key Components

### Domain Layer
- [Entity 1]: [Purpose] — src/main/java/.../[File].java

### Repository Layer
- [Repo 1]: [Purpose] — src/main/java/.../[File].java

### Service Layer
- [Service 1]: [Purpose] — src/main/java/.../[File].java

### Controller Layer
- [Controller 1]: [Purpose] — src/main/java/.../[File].java

## API Endpoints
| Method | Path | Description | Request | Response |
|--------|------|-------------|---------|----------|
| GET | /api/products | List products | Query params | ProductListDTO |

## Business Rules
- [Rule 1]: description
- [Rule 2]: description

## Security Considerations
- [Security aspect 1]

## Testing
- Unit tests: [Location]
- Integration tests: [Location]

## Related Tasks
- T-XXX: [Description]
```

### Phase 6: Update Related Bundles
If this task affects existing bundles, update them:
- Add new components to component lists
- Update architecture diagrams
- Add new API endpoints
- Update business rules
- Refresh "Last updated" timestamp

### Phase 7: Generate Completion Report
Create summary for user:

```markdown
## Task T-XXX — Archived

**Archive Location:** docs/archive/T-XXX/

**Documentation Updated:**
- Task archive created
- Architecture.md updated [or: No updates needed]
- Security.md updated [or: No updates needed]
- Feature bundle updated: [Bundle name]

**Next Recommended Task:**
`/feat-init T-XXX` — [Description]
```

## Anti-patterns to Avoid
- Don't create redundant documentation (reference, don't repeat)
- Don't archive incomplete or work-in-progress tasks
- Don't update ARCHITECTURE.md with trivial details
- Don't create bundles for every single task (group by feature area)
- Don't include sensitive data in archives (credentials, API keys)

## Maintenance Strategy
- Archives are append-only (never delete)
- Bundles are living documents (update as features evolve)
- ARCHITECTURE.md captures major decisions only
- Security.md only adds new patterns, not every implementation

## Input
The user provides the task ID: `/feat-archive T-XXX`

## Output
1. Complete task archive in `docs/archive/T-XXX/`
2. Updated `docs/ARCHITECTURE.md` (if needed)
3. Updated `.claude/rules/security.md` (if needed)
4. Updated or created feature bundle in `docs/bundles/`
5. Completion report with next steps
