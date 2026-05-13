---
name: sync-task
description: Syncs one or more context bundles in docs/bundles/ with the current state of the code after manual edits made outside the feat-do/change-do workflow. Auto-detects which bundles are affected from git diff, or targets a specific task when T-XXX is given. Use when the user runs /sync-task or /sync-task T-XXX.
---

# Sync Task Skill

## Purpose
Keep bundles accurate after out-of-workflow edits. An outdated bundle misleads future sessions more than having no bundle.

Difference from `change-archive`: `change-archive` is run after a managed `change-do` session. `sync-task` is run after ad-hoc manual edits — it auto-detects what changed and finds the affected bundle(s) itself.

---

## Instructions

### Phase 1: Find Changed Files

**If a task ID was given (e.g., `/sync-task T-090`):**
- Read `docs/bundles/T-090-*.md` directly — use the `files:` frontmatter list as the target set.

**If no task ID was given (`/sync-task`):**
- Run `git status --short` and `git diff --name-only HEAD` to get all modified/untracked files.
- Also check recent unpushed commits: `git log origin/main..HEAD --name-only --pretty=format:""` — include files from these commits too.
- Deduplicate the full changed-file list.

### Phase 2: Map Files → Bundles

Scan every `docs/bundles/*.md` frontmatter `files:` list.
For each changed file, find which bundle(s) list it (strip any inline annotations like `(active field added)` — match on the path only).

Build a map: `bundle → [changed files it owns]`.

If a changed file appears in **no bundle**:
- Note it as "untracked by any bundle" in the final report.
- Do NOT create a new bundle — that is `feat-archive`'s job.

If a changed file appears in **multiple bundles** (shared files like `GlobalExceptionHandler.java`):
- Update all affected bundles.

### Phase 3: Read Changed Files

For each bundle that needs updating:
1. Read the bundle file.
2. Read the current content of every changed file that belongs to that bundle.
3. Compare what the bundle's Logic Map / Business Rules / Exception Handling currently says against the actual code.

### Phase 4: Update Each Affected Bundle

Edit `docs/bundles/T-XXX-<feature>.md` — surgical updates only:

**Sections to update (only if they differ from current code):**
- **Logic Map**: re-describe any method whose behavior changed — one line per method, behavior only
- **Business Rules**: add/remove/update rules that changed
- **API Surface**: update if endpoints were added, removed, or signature changed
- **Exception Handling**: update if new exceptions mapped or HTTP codes changed
- **Tests**: update case counts if tests were added/removed
- **Files Index**: add any new files; remove deleted files

**Always update frontmatter:**
```yaml
last_updated: YYYY-MM-DD   # today
changes:
  - YYYY-MM-DD: [one-line summary of what was manually changed]
  # keep last 5 entries; drop oldest if over 5
```

**Do NOT:**
- Rewrite sections that didn't change
- Add commentary like "Previously X, now Y" — bundles reflect current state only
- Create a new bundle file

### Phase 5: Report

Tell the user:
- Which bundles were updated and which sections changed within each
- Any changed files not covered by any bundle (untracked files)
- If tests were touched: remind to run `mvn test -q`

---

## Edge Cases

- **File deleted from codebase**: remove it from the bundle's `files:` list and Files Index; note in `changes:` frontmatter.
- **New file added outside workflow**: flag as untracked — do not create a bundle entry for it unless the user explicitly wants it added to an existing bundle's scope.
- **No bundle found for any changed file**: report "No bundles need updating" and list the changed files so the user can decide if a new bundle is needed.
- **Bundle file not found for given T-XXX**: report error and list available bundles in `docs/bundles/`.
