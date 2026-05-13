---
name: feat-do
description: Implements a feature from its GitHub issue. Reads the issue, shows a brief implementation plan in the conversation for approval, then writes code, runs tests, and closes the issue when done. Use when the user runs /feat-do T-XXX or /feat-do #N.
---

# Feat Do Skill

## Purpose
Turn a GitHub issue into working code. Single command from issue → implementation → tests → comment on issue. Issue is closed by `/feat-archive` after the bundle is written.

---

## Instructions

### Phase 1: Load Issue Context
1. Find the GitHub issue number for this task ID:
   ```bash
   gh issue list --search "T-XXX in:title" --json number,title --jq '.[0].number'
   ```
2. Fetch the issue content:
   ```bash
   gh issue view <issue-number> --json title,body,labels
   ```
3. Read `docs/ARCHITECTURE.md` — layer rules, existing patterns
4. Read `CLAUDE.md` — code style

### Phase 2: Show Implementation Plan (conversation only, no file saved)
Output a concise plan in the conversation covering:
- Files to create / modify (with purpose)
- Key implementation decisions
- Security checklist items relevant to this feature
- Test scenarios

**Wait for user confirmation before proceeding.**

### Phase 3: Implement
For each file in the plan:
1. Read the file first if it exists
2. Write/edit following Spring Boot conventions:
   - Entity: `@Entity`, `@Table`, Lombok, `@PrePersist` for timestamps
   - Repository: `extends JpaRepository`, derived query methods
   - Service: interface + `@Service @RequiredArgsConstructor` impl, `@Transactional`
   - Controller: `@RestController`, `@Valid` on inputs, thin — delegate only
   - DTO: records or plain classes, `@Schema` on every field, no JPA annotations

#### Security — check every file:
- [ ] No hardcoded secrets
- [ ] Bean Validation on all user inputs (`@Valid`, `@NotNull`, `@NotBlank`)
- [ ] Spring Data queries only — no raw JDBC string concat
- [ ] No sensitive data in logs

#### Swagger — every controller must have:
- `@Tag(name = "...")` on class
- `@Operation(summary = "...")` + `@ApiResponses` on each endpoint
- `@Schema(description = "...", example = "...")` on every DTO field

### Phase 4: Write Tests
**Service tests** (`@ExtendWith(MockitoExtension.class)`):
- Happy path, not-found path, validation/edge cases

**Controller tests** (`@WebMvcTest` + `@Import(SecurityConfig.class, JwtAuthFilter.class)` + `@MockBean` for services and `JwtUtil`):
- Success status codes
- Validation rejection (400)
- Auth rejection (401)

Run tests:
```bash
mvn test -q
```
Fix any failures before proceeding.

### Phase 5: Comment on Issue (do NOT close)
Add a comment summarizing what was implemented. Do NOT close the issue — it will be closed by `/feat-archive` after the bundle is written:
```bash
gh issue comment <issue-number> --body "$(cat <<'EOF'
## Implemented

**Files created/modified:**
- `path/to/File.java` — purpose

**Tests:** ServiceTest (X cases), ControllerTest (Y cases)

**Swagger:** All endpoints annotated — http://localhost:8080/swagger-ui.html

> Issue will be closed by `/feat-archive T-XXX`
EOF
)"
```

---

## Architecture Hard Rules
- Controllers never inject repositories — always through services
- Services never return JPA entities — convert to DTOs
- `userId` always from JWT principal — never from request body
- Admin endpoints: separate `Admin*Controller` with `@PreAuthorize("hasRole('ADMIN')")`

## Output to User
```
## T-XXX — Done

Files: [list]
Tests: [pass count]
Issue #N: commented (open — close via /feat-archive T-XXX)
Next: run /feat-archive T-XXX
```
