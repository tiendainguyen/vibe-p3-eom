---
name: change-do
description: Loads the context bundle for an archived feature then implements a change. MUST be invoked before modifying any feature that has a bundle in docs/bundles/. Auto-trigger: when the user asks to change/fix/update something in a feature that has been archived, invoke this skill first to load the bundle context before reading or editing any source files. Use when the user runs /change-do T-XXX or when editing an archived feature.
---

# Change Do Skill

## Purpose
Apply a change to an already-implemented (archived) feature. The bundle in `docs/bundles/` gives full context in one read — avoiding the need to scan all source files from scratch.

Token efficiency goal: bundle read (1 file) + targeted source reads (1–3 files) → change → done.

---

## Instructions

### Phase 1: Load Bundle Context (ALWAYS FIRST)
1. List available bundles:
   ```bash
   ls docs/bundles/
   ```
2. Identify which bundle(s) are relevant to the change being requested
3. Read the relevant bundle(s) — this gives: API surface, logic map, business rules, file index
4. Fetch the GitHub issue (body + comments) to detect requirement changes since archive:
   ```bash
   gh issue view <N> --json number,title,body,comments --jq '{archived_req: .body, changes: [.comments[] | {date: .createdAt, author: .author.login, body: .body}]}'
   ```
   - Issue number comes from the bundle frontmatter (`issue: #N`)
   - If there are comments: scan for "Requirement Change" markers — surface any changed FRs to the user before proceeding
   - If no comments: requirements unchanged since archive, continue
5. From the bundle's **Files Index**, read ONLY the specific files needed for the change (not all files)

If no bundle exists for the feature being changed → proceed as normal but note that `/feat-archive` should be run after.

### Phase 2: Understand the Change
From the bundle context + targeted file reads:
- Identify exactly which methods/classes need to change
- Check if the change affects: business rules, API surface, exception handling, security
- Note any downstream effects on other features' bundles

Show the user a 3–5 line summary of what will change and why, then proceed (no need to wait unless the change is large or risky).

### Phase 3: Implement the Change
Follow the same rules as `feat-do`:
- Read the specific file(s) before editing
- Apply the minimal change — no unrelated cleanup
- Maintain all existing patterns from the bundle's Key Decisions
- Validate inputs, maintain security rules
- Update Swagger annotations if the API surface changes

### Phase 4: Update Tests
- If behavior changed: update/add the relevant test cases
- Run: `mvn test -q`
- Fix failures before continuing

### Phase 5: Create GitHub Issue for Tracking (if significant)
If the change is non-trivial (new behavior, API change, business rule update):
```bash
gh issue create \
  --title "change(T-XXX): Short description" \
  --label "change" \
  --body "What changed and why"
```
Close it immediately after implementation:
```bash
gh issue close <number>
```
Skip issue creation for trivial fixes (typo, minor validation tweak).

### Phase 6: Prompt for Archive Update
Tell the user:
```
Change complete. Run /change-archive T-XXX to update the context bundle.
```

---

## Trigger Rules
This skill should be invoked when:
- User says "change X", "fix X", "update X", "modify X" where X is an archived feature
- User starts editing a file that appears in any `docs/bundles/*.md` files index
- User asks about how something works in an archived feature (read bundle first, then answer)

Skip invocation only when:
- No bundles exist yet (first session)
- The change is to a new file not covered by any bundle
