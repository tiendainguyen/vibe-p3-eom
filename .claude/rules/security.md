# Security Rules

> These rules are loaded by Claude Code automatically. Violations should be caught by .claude/settings.json deny rules.

## Secrets & Credentials

- Never hardcode secrets, API keys, tokens, or passwords in source code
- Never read or write .env files — use environment variables at runtime
- Never commit private keys (*.pem, *.key) or credential files
- Never log sensitive data: passwords, tokens, PII, session IDs

## Code Safety

- Never use eval(), exec(), Function(), or dynamic code execution
- Never use string concatenation for SQL queries — always parameterized
- Never disable security features (CORS, CSRF, auth checks, rate limiting)
- Never trust user input — always validate and sanitize

## Dependencies

- Never run npx with untrusted packages
- Never install packages without checking — use `ask` permission
- Never use wildcard versions in package.json (prefer exact or ^)

## Git Safety

- Never force push to main/master/develop
- Never skip pre-commit hooks (--no-verify)
- Never reset --hard without explicit instruction
