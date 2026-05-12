---
name: change-archive
description: Updates the existing context bundle in docs/bundles/ after a change has been applied to an archived feature. Updates the logic map, business rules, and file index in-place — never creates a new bundle. Use when the user runs /change-archive T-XXX after completing a change.
---

# Change Archive Skill

## Purpose
Keep the context bundle accurate after a change. An outdated bundle is worse than no bundle — it misleads future sessions.

Update the existing `docs/bundles/T-XXX-<feature>.md` to reflect the current state of the code.

---

## Instructions

### Phase 1: Load Current State
1. Read the existing bundle: `docs/bundles/T-XXX-<feature>.md`
2. From the bundle's Files Index, read only the files that were modified in the recent change
3. Compare current code against the bundle's Logic Map and Business Rules

### Phase 2: Identify What Changed
Note specifically:
- Which methods changed behavior
- Which business rules changed or were added/removed
- Whether the API surface changed (new/removed/modified endpoints)
- Whether exception handling changed
- Whether new files were added or old files removed

### Phase 3: Update the Bundle In-Place
Edit `docs/bundles/T-XXX-<feature>.md` — update only the sections that changed:

**Always update:**
- `archived:` date → today's date (add a `last_updated:` field if not present)
- Any Logic Map entries for modified methods
- Business Rules if they changed
- Key Decisions if a new decision was made
- Files Index if files were added/removed

**Format rules (same as feat-archive):**
- Logic Map: one line per method — behavior only, no signatures
- Business Rules: testable and concrete
- File paths: exact, so future change-do reads work

**Do NOT:**
- Rewrite sections that didn't change
- Add commentary about what changed ("Previously X, now Y") — the bundle reflects current state only
- Create a new bundle file

### Phase 4: Update archived metadata
Add or update the frontmatter:
```yaml
---
feature: Feature Name
task: T-XXX
issue: #N
archived: YYYY-MM-DD        # original archive date
last_updated: YYYY-MM-DD    # today
changes:
  - YYYY-MM-DD: [one-line description of what changed]
  - YYYY-MM-DD: [...]       # keep last 5 entries max
---
```

### Phase 5: Report
Tell the user:
- Which sections were updated
- Whether any tests changed (prompt to run `mvn test -q` if not already done)

---

## Anti-patterns
- Don't rewrite the whole bundle — surgical updates only
- Don't keep stale logic descriptions for unchanged methods
- Don't omit new files added during the change — update the Files Index
- Don't add change-history prose inside the bundle body — use the `changes:` frontmatter list
