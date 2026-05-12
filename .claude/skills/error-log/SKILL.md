---
name: error-log
description: Synthesizes errors encountered in the current session and appends them to docs/ERRORS.md. Each entry records symptom, root cause, fix applied, affected files, and prevention tips. Use when the user runs /error-log.
---

# Error Log Skill

## Purpose
Extract every error/bug from the current conversation, document them in a structured format, and append to `docs/ERRORS.md` so future sessions have a searchable knowledge base of known issues and fixes.

One error = one entry. Do not merge unrelated errors.

---

## Instructions

### Phase 1: Extract Errors from Session
Scan the current conversation for:
- Stack traces, exception messages, test failures
- Browser errors, HTTP error responses
- Build failures, compilation errors
- Runtime misconfigurations (wrong ports, bad env vars, version mismatches)

For each error found, identify:
1. **Symptom** — what the user observed (error message, wrong behavior)
2. **Root cause** — the actual technical reason
3. **Fix** — what changed to resolve it
4. **Affected files** — which files were modified (exact paths)
5. **Prevention** — how to avoid it next time

### Phase 2: Read Existing Log
Read `docs/ERRORS.md` if it exists. Check for duplicate entries by comparing root cause.  
Skip any error that is already documented.

### Phase 3: Write Entries
For each **new** error, append one entry to `docs/ERRORS.md` using this format:

```markdown
## ERR-NNN — Short Title

**Date:** YYYY-MM-DD  
**Symptom:** What the user saw — HTTP status, error message, or failing test name.  
**Root Cause:** The specific technical reason this happened.  
**Fix:** What was changed to resolve it — be concrete.  
**Affected Files:**
- `path/to/file.java` — what changed

**Prevention:** One-line rule to avoid this class of error in the future.

---
```

Number entries sequentially (ERR-001, ERR-002, …). Read the existing file to determine the next number.

### Phase 4: Update Header (first run only)
If `docs/ERRORS.md` does not yet exist, create it with this header before the first entry:

```markdown
# Error Log — E-commerce Order Management API

> Running log of bugs encountered and fixed. Each entry is a reusable fix recipe.
> Format: symptom → root cause → fix → prevention

```

### Phase 5: Report
Tell the user:
- How many new entries were added
- ERR numbers assigned
- Any errors from the session that were skipped (already logged)

---

## Quality Rules
- Root cause must be specific ("SpringDoc 2.3.0 uses `ControllerAdviceBean(Object)` constructor removed in Spring 6.2" ✓, "version incompatibility" ✗)
- Fix must name the exact change ("updated `springdoc.version` from `2.3.0` to `2.7.0` in `pom.xml`" ✓, "updated dependency" ✗)
- Affected files must be exact paths — no globs, no vague descriptions
- Prevention must be actionable ("always check SpringDoc compatibility matrix before upgrading Spring Boot" ✓, "be careful with versions" ✗)
- One entry per distinct root cause — two symptoms from the same root = one entry
