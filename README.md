# NovaLedger

A multi-tenant personal finance SaaS application built with **Spring Boot 3.x Modular Monolith** architecture.

> Portfolio project demonstrating senior backend engineering practices: multi-tenancy, JWT + Spring Security coexistence, AES-256 field encryption, AOP audit logging, Strategy Pattern for bank statement parsing, Flyway migrations, Docker, and Railway deployment.

---

## Live Demo

🔗 **https://novaledger-production.up.railway.app/page/login**

| Role | Email | Password |
|------|-------|----------|
| Member (Alice) | alice@example.com | demo1234 |
| Member (Bob) | bob@example.com | demo1234 |
| Admin | admin@example.com | admin1234 |

API Documentation: `/swagger-ui.html`

---

## Features

**Authentication & Security**
- JWT Bearer token (API clients) + Session-based login (web pages) — coexistence design
- Strict tenant isolation: every query is scoped by `tenant_id` extracted from JWT claims
- Role-based access control: `ADMIN` / `MEMBER`
- JWT logout with Redis blacklist — token invalidated on logout with TTL matching
- Email verification on registration (Gmail SMTP, token expires in 15 minutes)
- Forgot password flow with secure reset link (SHA-256 hashed tokens stored in DB)
- Login rate limiting: 5 failures per IP → 15-minute lockout (Redis-backed)
- AES-256 encryption for bank account numbers and credit card numbers at rest
- Sensitive data masking: account numbers displayed as `****5678`
- Security headers: `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy`
- MIME type validation on file upload (Apache Tika whitelist)

**Finance Management**
- Bank account management with alias support (soft delete, initial balance)
- Credit card management (billing cycle settings, encrypted card number)
- Manual transaction entry (income / expense) with balance auto-update
- Bank statement import via Excel/CSV — Strategy Pattern with pluggable per-bank parsers
  - Currently supported: CTBC (中國信託), E.SUN (玉山), Yongfeng (永豐), Yuanta (元大)
  - Upload → Parse → Preview → Confirm flow with SHA-256 dedup protection
  - Account number extracted from file and cross-validated against selected account
- Dashboard: total assets, per-account balance cards, empty state guidance
- Monthly and yearly report pages with Chart.js line charts and Flatpickr date range picker
- Exchange rate management (admin-controlled, cached via Spring Cache)

**Admin Backend**
- Admin Dashboard: system health indicators (pending import jobs, 24h error count, DB status)
- User management: paginated list, search by name/email, enable/disable accounts
- Audit log viewer: filterable by user, action type, date range; sensitive fields masked
- Parser capability overview: dynamic table from `ParserRegistry` (bank, code, supported formats)
- Exchange rate management page
- System config management: toggle `registration.enabled`, `maintenance.mode` etc. via `system_configs` table; cache auto-evicted on update

**Infrastructure**
- Subscription plan enforcement (FREE / PRO limits on accounts, cards, categories)
- Flyway migrations: 21 versioned scripts, 34+ tables
- Structured JSON logging in production (logstash-logback-encoder), MDC with `traceId / userId / tenantId`
- AOP audit logging via `@AuditLog` annotation — before/after values captured, sensitive fields masked
- Docker multi-stage build
- CI via GitHub Actions with MySQL 8.0 + Redis 7 service containers (unit + integration tests)
- Auto-deploy to Railway on push to `master`

---

## Architecture

```
Modular Monolith — Spring Boot 3.5.x

tableauDemoV2/
├── common/         # BaseEntity, BaseTenantEntity, TenantContext (ThreadLocal),
│                   # ApiResponse<T>, BusinessException, ErrorCode,
│                   # SensitiveDataMasker, AuditAction constants
├── auth/           # JWT (JwtTokenProvider, JwtAuthenticationFilter),
│                   # User / Tenant / Role / UserTenant entities,
│                   # RedisBlacklistService, AesEncryptionService,
│                   # EmailService, SystemConfigService
├── finance-core/   # Accounts, credit cards, transactions, import pipeline,
│                   # dashboard, exchange rates, reports
│                   # (bank / account / creditcard / transaction /
│                   #  importjob / importrecord / dashboard / exchangerate / report)
├── health/         # Health check endpoint
└── application/    # Entry point, SecurityConfig, GlobalExceptionHandler,
                    # LoggingAspect, AuditLogAspect, TraceIdFilter, LogMdcFilter,
                    # NavigationMenuConfig, Thymeleaf templates
```

**Module dependency rules**
- `application` → depends on all modules
- `auth` → cannot depend on `finance-core`
- `finance-core` → cannot depend on `auth`
- `common` → shared by all modules
- No circular dependencies allowed

**Multi-tenancy**
- Every business table has a `tenant_id` column
- All queries enforce `WHERE tenant_id = ?`
- `TenantContext` (ThreadLocal) is set by `JwtAuthenticationFilter` and always cleared in `finally`
- `tenantId` flows only from JWT claims — never trusted from the request body or headers

**Strategy Pattern — Bank Statement Import**
```
BankStatementParser (interface)
├── getParserKey()           → "{BANK_CODE}_{FILE_TYPE}_{YYYYMMDD}"
├── getBankCode()
├── getFileType()
├── canHandle(rows)          → auto-detect by header signature
├── extractAccountNumber()   → Optional<String>, default empty
└── parse(rows)              → List<ParseResult>

AbstractColumnBasedStatementParser (abstract)
└── shared row loop, column count check, ParseResult assembly

Implementations:
├── CtbcBankStatementParser     (中國信託, 822)
├── EsunBankStatementParser     (玉山, 808)
├── YongfengBankStatementParser (永豐, 807)
└── YuantaBankStatementParser   (元大, 806)

ParserRegistry: auto-wires all parsers via Spring DI,
resolves by parserKey or bankCode + fileType
Dedup: SHA-256(accountId + date + amount + balance) per row
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.x |
| Architecture | Modular Monolith (Maven multi-module) |
| Database | MySQL 8.0 |
| Migration | Flyway 9.x (21 scripts, 34+ tables) |
| Authentication | Spring Security + JJWT 0.11.5 |
| Cache / Blacklist | Redis (Spring Data Redis) |
| Email | Spring Boot Starter Mail (Gmail SMTP) |
| File Detection | Apache Tika (MIME type validation) |
| Encryption | AES-256 (javax.crypto) |
| Logging | Logback + logstash-logback-encoder 7.4, MDC |
| Frontend | Thymeleaf + Bootstrap 5 + Chart.js + SweetAlert2 + Flatpickr |
| API Docs | springdoc-openapi 2.8.6 / Swagger UI |
| Containerization | Docker (multi-stage) + docker-compose |
| CI | GitHub Actions (MySQL 8.0 + Redis 7 service containers) |
| Deployment | Railway |

---

## Local Setup

**Prerequisites:** Java 17, Maven, Docker

```bash
# Clone
git clone https://github.com/your-username/novaledger.git
cd novaledger

# Create .env
cp .env.example .env
# Set DB_ROOT_PASSWORD, JWT_SECRET, AES_SECRET_KEY, MAIL_USERNAME, MAIL_PASSWORD in .env

# Start with Docker Compose (MySQL + Redis + App)
docker compose up --build

# App:        http://localhost:8111
# Swagger UI: http://localhost:8111/swagger-ui.html
```

**Run tests**
```bash
.\mvnw clean test "-Dspring.profiles.active=dev" "-Dskip.npm=true"
```

---

## Environment Variables

| Variable | Description |
|----------|-------------|
| `DB_ROOT_PASSWORD` | MySQL root password |
| `JWT_SECRET` | JWT signing secret (min 256-bit) |
| `AES_SECRET_KEY` | AES-256 encryption key for sensitive fields |
| `SPRING_DATASOURCE_URL` | JDBC URL (auto-set in Docker Compose) |
| `SPRING_REDIS_HOST` | Redis host |
| `SPRING_REDIS_PORT` | Redis port (default: 6379) |
| `MAIL_USERNAME` | Gmail SMTP sender address |
| `MAIL_PASSWORD` | Gmail App Password |

---

## Database Schema

Managed by Flyway — 21 migration scripts:

| Script | Content |
|--------|---------|
| V1 | Users and tenants |
| V2 | RBAC (roles, permissions) |
| V3 | System master data (banks, currencies, transaction types) |
| V4 | Subscription plans |
| V5 | Accounts and assets |
| V6 | Transactions |
| V7 | Budgets and recurring transactions |
| V8 | Snapshots and exchange rates |
| V9 | Import pipeline (upload_jobs, upload_files, parsed_records, import_logs) |
| V10 | Reports and scheduled tasks |
| V11 | Seed: bank branches |
| V12 | Seed: test users |
| V13 | Seed: categories |
| V14 | Seed: user-tenant associations |
| V15 | Seed: test accounts |
| V16 | Seed: exchange rates |
| V17 | Seed: family demo data |
| V18 | Seed: dashboard demo (account balances snapshots) |
| V19 | System configs (registration toggle, maintenance mode, rate limit params) |
| V20 | Audit logs |
| V21 | Seed: admin tenant |

---

## API Overview

All responses use a unified `ApiResponse<T>` wrapper.
All exceptions are handled by `GlobalExceptionHandler` using `BusinessException` + `ErrorCode` enum.

| Module | Base Path | Notes |
|--------|-----------|-------|
| Auth | `/api/auth` | register, login, logout, verify-email, forgot-password, reset-password |
| Accounts | `/api/accounts` | CRUD + soft delete |
| Credit Cards | `/api/cards` | CRUD + soft delete |
| Transactions | `/api/transactions` | CRUD + balance sync |
| Import | `/api/import` | upload, preview, confirm |
| Dashboard | `/api/dashboard` | summary stats |
| Reports | `/api/reports` | monthly, yearly |
| Exchange Rates | `/api/exchange-rates` | GET open, POST/PUT/DELETE ADMIN only |
| Banks | `/api/banks` | |
| Admin | `/api/admin/*` | `ADMIN` role required |

Full interactive docs at `/swagger-ui.html`

---

## Project Conventions

- Constructor injection only — no `@Autowired` on fields
- Controller handles HTTP only — no business logic
- Service owns all business logic and `@Transactional`
- Repository is stateless — `tenantId` always passed explicitly as a parameter
- Entity and DTO strictly separated — entities never reach the Controller layer
- `BusinessException(ErrorCode)` for all domain errors — no raw `RuntimeException`
- `ApiResponse<T>` wraps all REST responses
- Static factory `from()` methods on response DTOs
- All static assets (JS/CSS) are vendored locally — no external CDN references (CSP compliant)
- No inline `style` or event handler attributes in Thymeleaf templates

---

## Design Decisions

**Modular Monolith over Microservices** — Controlled complexity at the portfolio stage. Clear module boundaries allow future extraction without premature operational overhead.

**JWT + Session coexistence** — Thymeleaf pages use server-side session; API clients use JWT Bearer tokens. Both paths work simultaneously. Login via `fetch` to `/api/auth/login` returns JWT and simultaneously writes a `SecurityContext` to session, so Thymeleaf page routes protected by Spring Security pass without a second login.

**Strategy Pattern for bank import** — Adding a new bank format requires only extending `AbstractColumnBasedStatementParser` and registering the bean. The import pipeline and dedup logic are untouched.

**Redis JWT blacklist** — Stateless JWT requires a blacklist for logout. Each invalidated token is stored in Redis with a TTL equal to the token's remaining lifetime — no unbounded growth, no extra DB calls on every request.

**AES-256 field encryption** — Bank account numbers and credit card numbers are encrypted at the service layer before persistence. The encryption key is injected via environment variable and never hardcoded.

**SHA-256 hashed tokens** — Password reset and email verification tokens are stored as SHA-256 hashes. The raw UUID is sent in the email link only; the DB never holds a reversible value.

**AOP audit logging** — `@AuditLog` annotation on service methods triggers `AuditLogAspect` to capture before/after state, user identity, and IP address asynchronously. Sensitive fields are masked before storage via `SensitiveDataMasker`.

---

## CI/CD

GitHub Actions triggers on push and PR to `dev` / `master`:
```
Checkout → JDK 17 (Temurin) → MySQL 8.0 service container → Redis 7 service container → mvn clean test
```
Railway auto-deploys from `master` on successful push.
