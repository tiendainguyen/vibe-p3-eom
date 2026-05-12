---
name: feat-archive
description: After a feature is implemented, creates a compact context bundle at docs/bundles/T-XXX-<feature>.md. The bundle maps every source file to its business logic so future sessions can load full context in ~1 read instead of reading all source files. Use when the user runs /feat-archive T-XXX.
---

# Feat Archive Skill

## Purpose
Create a single, dense context document (`docs/bundles/T-XXX-<feature>.md`) that captures everything needed to understand or modify this feature in a future session — without re-reading all source files.

The bundle format is designed for **context efficiency**: one file load ≈ full feature context.

---

## Instructions

### Phase 1: Identify All Feature Files
1. Find the GitHub issue number for this task ID — try in order:
   - If `docs/PROGRESS.md` exists: read it and find the `#N` for this task ID
   - Otherwise: search GitHub — `gh issue list --search "T-XXX in:title" --json number,title --jq '.[0].number'`
2. Use git to find all files changed in this feature:
   ```bash
   git diff --name-only HEAD~N HEAD   # or git log --name-only
   ```
3. Read each file to understand its current logic

### Phase 2: Write the Bundle
Save to `docs/bundles/T-XXX-<kebab-feature-name>.md`.

The bundle has a strict format — every section must be complete but tight:

```markdown
---
feature: Feature Name
task: T-XXX
issue: #N
archived: YYYY-MM-DD
files:
  - src/main/java/com/example/eom/...
  - [all files belonging to this feature]
---

# Bundle: Feature Name

## API Surface
| Method | Path | Auth | Handler |
|--------|------|------|---------|
| POST | /api/... | public | Controller.method() |

## Logic Map

### ClassName (file: path/to/File.java)
- `methodName(params)`: [what it does in one line — inputs → key steps → output/side-effect]
- `methodName2(params)`: [same format]
[Only methods with real logic — skip trivial getters/setters]

[One section per service/filter/component that has logic. Skip pure DTOs and repositories with only derived methods.]

## Business Rules
1. [Concrete, testable rule — not vague]
2. [...]

## Key Decisions
- [Decision]: [why — one line]
- [Decision]: [why — one line]

## Exception Handling
- [ExceptionClass] → [HTTP status] — [handler location]

## Tests
- [TestClass]: [what it covers, how many cases]

## Files Index
[Group by layer — keep paths exact so change-do can read them directly]
**Domain:** path/to/Entity.java, path/to/Enum.java
**Repository:** path/to/Repo.java
**Service:** path/to/Service.java, path/to/impl/ServiceImpl.java
**Controller:** path/to/Controller.java
**DTO:** path/to/dto/Request.java, path/to/dto/Response.java
**Config:** path/to/Config.java (if modified)
**Migration:** src/main/resources/db/migration/VN__name.sql
**Tests:** src/test/.../ServiceTest.java, src/test/.../ControllerTest.java
```

### Phase 3: Update ARCHITECTURE.md (only if needed)
Update only if this feature introduced a **new pattern or boundary rule** not already documented. Skip for standard CRUD additions.

### Phase 4: Close GitHub Issue
Use the issue number found in Phase 1, then close it with a comment pointing to the bundle:

```bash
gh issue close <N> --comment "Archived to docs/bundles/T-XXX-<feature>.md"
```

If `gh` is not authenticated or the command fails, log the error and continue — do not block the archive.

### Phase 5: Report
Tell the user:
- Bundle location: `docs/bundles/T-XXX-<feature>.md`
- File count archived
- Whether ARCHITECTURE.md was updated
- GitHub issue #N closed (or skipped with reason if it failed)

---

## Quality Rules for Bundles
- Logic Map entries must be one line each — if it takes two lines, split the method description
- Business Rules must be testable ("email must be unique" ✓, "handle user data properly" ✗)
- File paths must be exact — change-do will use them to Read files directly
- No copy-paste from Javadoc — describe behavior, not signatures
- No sensitive values — no example passwords, no real secrets
