---
name: feat-apply
description: Executes the approved implementation plan for a task by writing Spring Boot code following architectural boundaries, running tests, and updating task status in docs/TASK_BREAKDOWN.md. Use when the user runs /feat-apply T-XXX to implement a task that has an approved plan in docs/plans/.
---

# Feature Apply Skill

## Purpose
Execute the implementation plan for a task, writing code that strictly follows the plan and project architecture rules.

## Instructions

You are a senior Spring Boot developer implementing code according to an approved plan.

### Phase 1: Plan Loading & Validation
1. Read the plan from `docs/plans/[TASK-ID]-plan.md`
2. Read `docs/ARCHITECTURE.md` to refresh architectural rules
3. Read `CLAUDE.md` for code style requirements
4. Read `.claude/rules/security.md` for security constraints
5. Verify all prerequisites are met before starting

### Phase 2: Setup Todo List
Create a todo list from the implementation steps in the plan:
- Use TodoWrite to create one todo per implementation step
- Mark the first step as `in_progress`
- Update todos in real-time as you work

### Phase 3: Implementation Loop
For each step in the plan:

#### 3.1 Read Before Writing
- If modifying an existing file, ALWAYS use Read tool first
- Check for similar patterns in the codebase
- Understand the current implementation before making changes

#### 3.2 Write Code
Follow the plan exactly:
- Use Edit tool for modifications, Write tool for new files
- Follow Spring Boot conventions:
  - Entities: JPA annotations, validation, equals/hashCode
  - Repositories: Spring Data interfaces, proper method naming
  - Services: Interface + implementation, @Service, @Transactional
  - Controllers: @RestController, @RequestMapping, @Valid on inputs
  - DTOs: Simple POJOs, validation annotations
- Follow code style from CLAUDE.md:
  - PascalCase for classes
  - camelCase for methods and variables
  - UPPER_SNAKE_CASE for constants
  - Imports grouped: java.* → javax.* → third-party → project

#### 3.3 Security Compliance
For EVERY file, verify:
- [ ] No hardcoded secrets, API keys, or passwords
- [ ] Input validation using Bean Validation (@Valid, @NotNull, etc.)
- [ ] Parameterized queries only (Spring Data JPA)
- [ ] No raw JDBC or string concatenation in queries
- [ ] Proper error handling (don't leak sensitive info)

#### 3.4 Testing
After implementing code:
- Write unit tests for services (mock dependencies)
- Write integration tests for repositories (@DataJpaTest)
- Write controller tests (@WebMvcTest)
- Run tests: `./mvnw test`

#### 3.5 Update Todo
- Mark current step as `completed`
- Mark next step as `in_progress`
- Continue until all steps done

### Phase 4: Validation & Verification
After completing all steps:

#### 4.1 Architecture Compliance
- [ ] Controllers only delegate to services (no business logic)
- [ ] No JPA entities exposed in controller methods (use DTOs)
- [ ] Proper layer separation maintained

#### 4.2 Code Quality
- [ ] No TODOs or placeholder comments left
- [ ] All imports organized correctly
- [ ] Proper error handling with meaningful messages

#### 4.3 Security Final Check
- [ ] No violations of `.claude/rules/security.md`
- [ ] All user inputs validated
- [ ] No security features disabled
- [ ] No sensitive data in logs

#### 4.4 Testing
- [ ] All tests pass: `./mvnw test`
- [ ] Code compiles: `./mvnw compile`

### Phase 5: Documentation Update
Update `docs/plans/[TASK-ID]-plan.md`:
- Add "## Implementation Notes" section at the end
- Document any deviations from the plan (with justification)
- Note actual implementation time vs. estimate

### Phase 6: Mark Task Complete
Update `docs/TASK_BREAKDOWN.md`:
- Change task status from "pending" to "completed"
- Add completion date

### Phase 7: Summary Report
Present to user:
```markdown
## Task [TASK-ID] — Completed

**Implemented:**
- [File 1]: [What was done]
- [File 2]: [What was done]

**Tests:**
- [Test 1]: [Coverage]

**Next Steps:**
- Task T-XXX is now unblocked and ready
- Consider running `/feat-init T-XXX` to continue
```

## Anti-patterns to Avoid
- Don't deviate from the plan without documenting why
- Don't skip reading existing files before editing
- Don't skip writing tests
- Don't leave TODO comments or placeholder code
- Don't expose JPA entities in controllers
- Don't inject repositories directly in controllers
- Don't use raw SQL or string concatenation for queries
- Don't hardcode configuration values

## Error Handling Strategy
If you encounter blockers:
1. Document the issue in implementation notes
2. Try to resolve with minimal plan deviation
3. If major deviation needed, STOP and report to user

## Input
The user provides the task ID: `/feat-apply T-XXX`

## Output
1. Implemented code files (created/modified)
2. Test files with passing tests
3. Updated documentation
4. Summary report with next steps
