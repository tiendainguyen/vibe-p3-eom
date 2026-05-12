# p3-eom

A real-time collaboration tool for engineering teams

**Stack:** Spring Boot 3 + Java 21
**Build:** Maven (./mvnw)

## Commands

```bash
./mvnw spring-boot:run          # localhost:8080
./mvnw test                     # Run all tests
./mvnw package -DskipTests      # Build jar
./mvnw verify                   # Tests + checks
./mvnw flyway:migrate           # Run DB migrations  ← remove if no DB
```

## Project Structure

```
src/main/java//
├── controller/         # REST controllers (thin — delegate to services)
├── service/            # Business logic (interfaces + implementations)
├── repository/         # Spring Data JPA repositories
├── domain/             # JPA entities + value objects
├── dto/                # Request/response DTOs
└── config/             # Spring config
src/test/java//
docs/                           # Architecture doc
```

## Code Style

- Classes: `PascalCase` — methods/variables: `camelCase` — constants: `UPPER_SNAKE_CASE`
- Imports grouped: `java.*` → `javax.*` → third-party → project
- Controllers thin — validation + delegation only, no business logic
- Services depend on interfaces, not concrete implementations
- DTOs for all API boundaries — never expose JPA entities directly

## Architecture Rules

> Source of truth: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

### Layer boundaries

- **Controller** (controller/): HTTP concerns only — validation + delegation
- **Service** (service/): business logic — no HTTP awareness
- **Repository** (repository/): database queries — no business rules

### Hard boundaries

- Controllers never inject repositories — always go through services
- Services never expose JPA entities — convert to DTOs before returning
- Only services may call external APIs

## Security Hard Rules

> Full details: [.claude/rules/security.md](.claude/rules/security.md)
> Enforced by: [.claude/settings.json](.claude/settings.json) deny rules

- **NEVER** hardcode secrets, tokens, API keys, or passwords
- **NEVER** commit `application-prod.yml` or files with credentials
- **NEVER** disable CSRF, CORS, or Spring Security checks
- **ALWAYS** validate user input — use Bean Validation (`@Valid`, `@NotNull`)
- **ALWAYS** parameterized queries — use JPA/Spring Data, no raw JDBC string concat

## SOT References

| Document | Location | Purpose |
|----------|----------|---------|
| Architecture | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System design, layers, boundaries |
| Security Rules | [.claude/rules/security.md](.claude/rules/security.md) | Detailed security checklist |
| AI Permissions | [.claude/settings.json](.claude/settings.json) | Allow/deny/ask rules |

## Current State

- **Milestone:** in progress
- **Known issues:** none yet
- **Tech debt:** none yet

## AI Rules

- Follow existing patterns before inventing new ones
- Keep changes small and focused — one task per session
- When unsure about approach → explain options briefly, then ask
- Read docs/ARCHITECTURE.md before making structural changes
- Do not modify files outside the scope of the current task
