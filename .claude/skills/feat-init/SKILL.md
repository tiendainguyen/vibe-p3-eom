---
name: feat-init
description: Creates a detailed implementation plan for a specific task from docs/TASK_BREAKDOWN.md, including files to create/modify, step-by-step implementation instructions, security checklist, and testing strategy. Saves the plan to docs/plans/T-XXX-plan.md. Use when the user runs /feat-init T-XXX to plan a task before implementing it.
---

# Feature Init Skill

## Purpose
Initialize a detailed implementation plan for a specific task from TASK_BREAKDOWN.md, following Spring Boot best practices and project architecture rules.

## Instructions

You are a senior Spring Boot developer creating a detailed implementation plan for a specific task.

### Phase 1: Task Context Loading
1. Read `docs/TASK_BREAKDOWN.md` to find the target task by ID and its URD link
2. Read `docs/urd/T-XXX-<feature>.md` — the URD for this task (contains user stories, API endpoints, entities, business rules, and acceptance criteria). This is the primary requirements source.
3. Read `docs/ARCHITECTURE.md` to understand architectural constraints
4. Read `CLAUDE.md` for code style and conventions
5. Read `.claude/rules/security.md` for security requirements
6. Identify all dependency tasks and verify they are completed

### Phase 2: Codebase Analysis
Use the Explore agent to:
- Find similar existing implementations in the codebase
- Identify reusable patterns (services, DTOs, validators)
- Locate related configuration files
- Check for existing tests that can be used as templates

### Phase 3: Plan Creation
Create a detailed implementation plan with these sections:

#### 3.1 Overview
- Task ID and description
- Feature area and layer
- Estimated effort and actual task complexity

#### 3.2 Prerequisites Check
- [ ] All dependency tasks completed
- [ ] Required database migrations applied
- [ ] Required dependencies in pom.xml
- [ ] Required configuration in application.yml

#### 3.3 Files to Create/Modify
List each file with clear purpose:
```
CREATE src/main/java/com/example/domain/Product.java
  Purpose: JPA entity for products with validation

MODIFY src/main/java/com/example/service/ProductService.java
  Purpose: Add search method with filtering

CREATE src/test/java/com/example/service/ProductServiceTest.java
  Purpose: Unit tests for ProductService
```

#### 3.4 Implementation Steps
Numbered steps in execution order:
1. **Create Product entity** (src/main/java/.../domain/Product.java)
   - Add JPA annotations: @Entity, @Table, @Id
   - Add validation: @NotNull, @Size, @Min
   - Add audit fields: createdAt, updatedAt
   - Follow naming conventions from CLAUDE.md

2. **Create ProductRepository interface** (src/main/java/.../repository/ProductRepository.java)
   - Extend JpaRepository<Product, Long>
   - Add custom query methods using Spring Data conventions
   - No raw SQL — use method naming or @Query with JPQL

[Continue with detailed steps...]

#### 3.5 Security Considerations
For this task, apply:
- [ ] Input validation using Bean Validation (@Valid, @NotNull, etc.)
- [ ] Parameterized queries (JPA/Spring Data only)
- [ ] No hardcoded secrets or credentials
- [ ] Proper error handling without leaking sensitive data

#### 3.6 Testing Strategy
- **Unit Tests**: Test service logic in isolation with mocked dependencies
- **Integration Tests**: Test repository with @DataJpaTest
- **Controller Tests**: Test endpoints with @WebMvcTest
- **Test Data**: Use realistic but safe test data (no real credentials)

#### 3.7 Validation Checklist
Before considering task complete:
- [ ] Code follows architectural boundaries (Controller → Service → Repository)
- [ ] DTOs used for API boundaries (no entity exposure)
- [ ] Services depend on interfaces, not implementations
- [ ] All imports grouped correctly (java.* → javax.* → third-party → project)
- [ ] Code style matches existing codebase (PascalCase classes, camelCase methods)
- [ ] Tests written and passing
- [ ] No security rule violations
- [ ] No TODOs or placeholder code left behind

### Phase 4: Output Format
Save the plan to `docs/plans/[TASK-ID]-plan.md`.

### Phase 5: User Confirmation
Present the plan summary to the user:
- Task ID and description
- Files to create/modify count
- Estimated implementation time
- Key security considerations
- Ask: "Ready to proceed with implementation? (Use `/feat-apply [TASK-ID]`)"

## Anti-patterns to Avoid
- Don't create plans that violate architectural boundaries
- Don't skip security considerations
- Don't create overly generic plans (be specific to this task)
- Don't ignore existing patterns in the codebase
- Don't plan to expose JPA entities directly in controllers
- Don't plan hardcoded configuration values

## Input
The user provides the task ID: `/feat-init T-XXX`

## Output
1. Detailed plan saved to `docs/plans/T-XXX-plan.md`
2. Summary presented to user with next steps
