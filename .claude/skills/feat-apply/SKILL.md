---
name: feat-apply
description: Executes the approved implementation plan for a task by writing Spring Boot code following architectural boundaries, running tests, and updating task status in docs/PROGRESS.md. Use when the user runs /feat-apply T-XXX to implement a task that has an approved plan in docs/plans/.
---

# Feature Apply Skill

## Purpose
Execute the implementation plan for a task, writing code that strictly follows the plan and project architecture rules — including unit tests and Swagger/OpenAPI documentation for every controller.

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
- Add explicit todos for: unit tests, Swagger annotations
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

#### 3.4 Swagger / OpenAPI Documentation (MANDATORY)
Every controller endpoint MUST be annotated with SpringDoc OpenAPI annotations.
Do NOT skip this step even for simple CRUD.

**Controller class level:**
```java
@Tag(name = "Resource Name", description = "Short description of this controller's responsibility")
```

**Each endpoint method:**
```java
@Operation(
    summary = "One-line description of what this endpoint does",
    description = "Optional longer description; omit if summary is enough"
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Success",
        content = @Content(schema = @Schema(implementation = ResponseDto.class))),
    @ApiResponse(responseCode = "400", description = "Validation error"),
    @ApiResponse(responseCode = "404", description = "Resource not found"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
})
```

**Request/Response DTOs:**
```java
@Schema(description = "Request body for creating/updating X")
public class XRequest {
    @Schema(description = "Human-readable field description", example = "example value")
    @NotNull
    private String fieldName;
}
```

**Verify `springdoc-openapi-starter-webmvc-ui` is present in `pom.xml`.** If missing, add:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

#### 3.5 Unit Tests (MANDATORY)
Write tests ALONGSIDE each class, not at the end. Minimum coverage requirements:

**Service unit tests** (`src/test/java/.../service/`):
- Use JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
- Mock ALL dependencies with `@Mock` / `@InjectMocks`
- Test cases per method:
  - Happy path (valid input → expected output)
  - Not-found path (entity missing → exception thrown)
  - Validation/edge cases relevant to business logic
- Example structure:
```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserServiceImpl userService;

    @Test
    void findById_existingUser_returnsDto() { ... }

    @Test
    void findById_unknownId_throwsNotFoundException() { ... }
}
```

**Controller unit tests** (`src/test/java/.../controller/`):
- Use `@WebMvcTest(XController.class)` + `@MockBean` for services
- Test HTTP method, URL mapping, status codes, and response body
- Test input validation rejection (400 Bad Request)
- Example structure:
```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;

    @Test
    void getUser_validId_returns200() throws Exception {
        given(userService.findById(1L)).willReturn(mockDto());
        mockMvc.perform(get("/api/users/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createUser_missingName_returns400() throws Exception { ... }
}
```

**Repository tests** (only when custom queries exist):
- Use `@DataJpaTest` with an in-memory H2 database
- Test custom JPQL/native queries with real data setup

#### 3.6 Update Todo
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

#### 4.4 Swagger Completeness
- [ ] Every controller class has `@Tag`
- [ ] Every endpoint has `@Operation` + `@ApiResponses`
- [ ] Every request/response DTO field has `@Schema`
- [ ] `springdoc-openapi-starter-webmvc-ui` in pom.xml
- [ ] Swagger UI accessible at `http://localhost:8080/swagger-ui.html` (verify after run)

#### 4.5 Test Coverage
- [ ] Service tests: happy path + not-found + edge cases
- [ ] Controller tests: success status + validation rejection
- [ ] All tests pass: `./mvnw test`
- [ ] Code compiles: `./mvnw compile`

### Phase 5: Documentation Update
Update `docs/plans/[TASK-ID]-plan.md`:
- Add "## Implementation Notes" section at the end
- Document any deviations from the plan (with justification)
- Note actual implementation time vs. estimate

### Phase 6: Mark Task Complete
Update `docs/PROGRESS.md`:
- Change task status from "pending" to "completed"
- Add completion date

### Phase 7: Summary Report
Present to user:
```markdown
## Task [TASK-ID] — Completed

**Implemented:**
- [File 1]: [What was done]
- [File 2]: [What was done]

**Unit Tests:**
- [ServiceTest]: [methods covered]
- [ControllerTest]: [endpoints covered]

**Swagger:**
- All endpoints annotated — Swagger UI at http://localhost:8080/swagger-ui.html

**Next Steps:**
- Task T-XXX is now unblocked and ready
- Consider running `/feat-init T-XXX` to continue
```

## Anti-patterns to Avoid
- Don't deviate from the plan without documenting why
- Don't skip reading existing files before editing
- Don't skip writing unit tests — they are mandatory, not optional
- Don't skip Swagger annotations — every endpoint must be documented
- Don't leave TODO comments or placeholder code
- Don't expose JPA entities in controllers
- Don't inject repositories directly in controllers
- Don't use raw SQL or string concatenation for queries
- Don't hardcode configuration values
- Don't write only happy-path tests — cover failure cases too

## Error Handling Strategy
If you encounter blockers:
1. Document the issue in implementation notes
2. Try to resolve with minimal plan deviation
3. If major deviation needed, STOP and report to user

## Input
The user provides the task ID: `/feat-apply T-XXX`

## Output
1. Implemented code files (created/modified)
2. Unit test files with passing tests (service + controller layers)
3. Swagger/OpenAPI annotations on all controllers and DTOs
4. Updated documentation
5. Summary report with next steps
