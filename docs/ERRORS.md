# Error Log — E-commerce Order Management API

> Running log of bugs encountered and fixed. Each entry is a reusable fix recipe.
> Format: symptom → root cause → fix → prevention

---

## ERR-001 — GitHub MCP Authentication Failure

**Date:** 2026-05-12
**Symptom:** `MCP error -32603: Authentication Failed: Bad credentials` when calling any `mcp__github__*` tool.
**Root Cause:** GitHub token not configured or expired in MCP settings — the MCP server had no valid `GITHUB_TOKEN`.
**Fix:** Installed `gh` CLI via Homebrew (`brew install gh`) and authenticated with `gh auth login`, which stored credentials for the `gh` CLI to use instead of MCP.
**Affected Files:** *(none — configuration issue only)*
**Prevention:** Before running any skill that creates GitHub issues (`/task-breakdown`, `/feat-do`), verify connectivity with `gh repo view --json nameWithOwner`. If MCP GitHub fails, fall back to `gh` CLI.

---

## ERR-002 — `gh` CLI Not Installed

**Date:** 2026-05-12
**Symptom:** `zsh: command not found: gh` when running `gh repo view --json nameWithOwner` in task-breakdown skill.
**Root Cause:** GitHub CLI (`gh`) was not installed on the machine.
**Fix:** Ran `brew install gh && gh auth login` (chose `GitHub.com → HTTPS → Login with a web browser`).
**Affected Files:** *(none — tooling setup)*
**Prevention:** Add `gh` CLI to project prerequisites in README. Document: `brew install gh && gh auth login`.

---

## ERR-003 — Spring Security Returns 403 Instead of 401 for Unauthenticated Requests

**Date:** 2026-05-12
**Symptom:** Test `AuthControllerTest.getMe_noJwt_returns401` failed — expected HTTP 403 but received 403. GET `/api/users/me` without JWT returned 403 Forbidden instead of 401 Unauthorized.
**Root Cause:** No `AuthenticationEntryPoint` was configured in `SecurityConfig`. Spring Security's default behavior for unauthenticated requests to protected endpoints is to return 403, not 401, when there is no entry point override.
**Fix:** Added a custom `AuthenticationEntryPoint` lambda to the `.exceptionHandling()` block in `SecurityConfig` that writes a JSON `ErrorResponseDTO` with status 401.
**Affected Files:**
- `src/main/java/com/example/eom/config/SecurityConfig.java` — added `.exceptionHandling(ex -> ex.authenticationEntryPoint(...))`, injected `ObjectMapper`

**Prevention:** For any stateless REST API using Spring Security, always configure `authenticationEntryPoint` to return JSON 401. Without it, Spring Security sends 403 for all unauthenticated access, which is semantically wrong for REST.

---

## ERR-004 — `NoResourceFoundException` Mapped to 500 Instead of 404

**Date:** 2026-05-12
**Symptom:** Accessing a non-existent path under `/swagger-ui/**` (e.g., `/swagger-ui/v3/api-docs`) returned `{"status":500,"error":"Internal Server Error",...}` instead of 404.
**Root Cause:** Spring Framework 6 (Spring Boot 3.x) introduced `org.springframework.web.servlet.resource.NoResourceFoundException` for missing static resources. The catch-all `@ExceptionHandler(Exception.class)` in `GlobalExceptionHandler` was catching it and returning 500.
**Fix:** Added an explicit `@ExceptionHandler(NoResourceFoundException.class)` handler in `GlobalExceptionHandler` that returns `ResponseEntity` with HTTP 404.
**Affected Files:**
- `src/main/java/com/example/eom/config/GlobalExceptionHandler.java` — added `handleNoResource(NoResourceFoundException ex)` method

**Prevention:** In every Spring Boot 3.x project, always add a `NoResourceFoundException` handler to `GlobalExceptionHandler`. It does not exist in Spring Boot 2.x so it is a common migration gap.

---

## ERR-005 — Exception Catch-All Swallowing Stack Traces Silently

**Date:** 2026-05-12
**Symptom:** Server returned `{"status":500,"error":"Internal Server Error","message":"An unexpected error occurred"}` with no corresponding log output — impossible to diagnose root cause without guessing.
**Root Cause:** The catch-all `@ExceptionHandler(Exception.class)` in `GlobalExceptionHandler` was returning a response without logging the exception object, silently discarding the stack trace.
**Fix:** Added `@Slf4j` to `GlobalExceptionHandler` and added `log.error("Unhandled exception: {}", ex.getMessage(), ex)` as the first line of `handleGeneral()`.
**Affected Files:**
- `src/main/java/com/example/eom/config/GlobalExceptionHandler.java` — added `@Slf4j`, added `log.error(...)` call

**Prevention:** Every catch-all exception handler must log the full exception (`ex`, not just `ex.getMessage()`). Silent 500s make debugging exponentially harder.

---

## ERR-006 — SpringDoc 2.3.0 Incompatible with Spring Boot 3.4.1 (Spring 6.2.x)

**Date:** 2026-05-12
**Symptom:** GET `/v3/api-docs` returned HTTP 500. Swagger UI showed "Unable to render this definition — does not specify a valid version field". Stack trace: `NoSuchMethodError: 'void org.springframework.web.method.ControllerAdviceBean.<init>(java.lang.Object)'` at `springdoc-openapi-starter-common-2.3.0.jar`.
**Root Cause:** SpringDoc 2.3.0 (Feb 2024) internally calls `new ControllerAdviceBean(Object)` in `GenericResponseService.getGenericMapResponse()`. This constructor was removed in Spring Framework 6.2.x (shipped with Spring Boot 3.4.x). The API break causes a `NoSuchMethodError` at runtime when SpringDoc tries to generate the OpenAPI spec.
**Fix:** Updated `springdoc.version` from `2.3.0` to `2.7.0` in `pom.xml`. SpringDoc 2.7.0 is built against Spring Framework 6.2.x and uses the updated `ControllerAdviceBean` API.
**Affected Files:**
- `pom.xml` — `<springdoc.version>2.3.0</springdoc.version>` → `<springdoc.version>2.7.0</springdoc.version>`

**Prevention:** SpringDoc minor version must track Spring Boot minor version. Compatibility rule: SpringDoc 2.N.x → Spring Boot 3.N.x (approximately). Always check the [SpringDoc compatibility matrix](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot) before upgrading Spring Boot.

---
