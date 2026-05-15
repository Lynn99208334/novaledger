# NovaLedger

A multi-tenant personal finance SaaS application built with **Spring Boot 3.x Modular Monolith** architecture.

> Portfolio project demonstrating senior backend engineering practices: multi-tenancy, JWT + Spring Security coexistence, Strategy Pattern for bank statement parsing, Flyway migrations, Docker, and Railway deployment.

---

## Live Demo

🔗 **[https://your-railway-url.railway.app](https://tableaudemov2-production.up.railway.app/page/login)**

| Role | Email | Password |
|------|-------|----------|
| Member (Alice) | alice@example.com | demo1234 |
| Member (Bob) | bob@example.com | demo1234 |
| Admin | admin@example.com | admin1234 |

API Documentation: `/swagger-ui.html`

---

## Features

**Authentication**
- JWT Bearer token (API clients) + Session-based login (web pages) — coexistence design
- Strict tenant isolation: every query is scoped by `tenant_id` extracted from JWT claims
- Role-based access control: `ADMIN` / `MEMBER`
- JWT logout with Redis blacklist — token invalidated on logout with TTL matching

**Finance Management**
- Bank account management (soft delete, initial balance)
- Credit card management (billing cycle settings)
- Manual transaction entry (income / expense) with balance auto-update
- Bank statement import via Excel/CSV — Strategy Pattern with pluggable per-bank parsers
  - Currently supported: CTBC (中國信託), E.SUN (玉山), Yongfeng (永豐)
  - Upload → Parse → Preview → Confirm flow with duplicate protection
- Dashboard: total assets, monthly income/expense, category breakdown (Chart.js)
- Exchange rate management (admin-controlled)
- Admin backend: bank management, user plan control

**Infrastructure**
- Subscription plan enforcement (FREE / PRO limits on accounts, cards, categories)
- Flyway migrations: 18 versioned scripts, 34 tables
- Docker multi-stage build
- CI via GitHub Actions on push/PR to `dev` and `master`
- Auto-deploy to Railway on push

---

## Architecture

```
Modular Monolith — Spring Boot 3.5.x

tableauDemoV2/
├── common/         # BaseEntity, BaseTenantEntity, TenantContext (ThreadLocal),
│                   # ApiResponse<T>, BusinessException, ErrorCode
├── auth/           # JWT (JwtTokenProvider, JwtAuthenticationFilter),
│                   # User / Tenant / Role / UserTenant entities,
│                   # RedisBlacklistService
├── finance-core/   # Accounts, credit cards, transactions, import pipeline,
│                   # dashboard, exchange rates
│                   # (bank / account / creditcard / transaction /
│                   #  importjob / importrecord / dashboard / exchangerate)
├── health/         # Health check endpoint
└── application/    # Entry point, SecurityConfig, GlobalExceptionHandler,
                    # LoggingAspect, TraceIdFilter, Thymeleaf templates
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
├── getParserKey()        → "{BANK_CODE}_{FILE_TYPE}_{YYYYMMDD}"
├── getBankCode()
├── getFileType()
├── canHandle(rows)
└── parse(rows) → List<ParseResult>

Implementations:
├── CtbcBankStatementParser     (中國信託)
├── EsunBankStatementParser     (玉山)
└── YongfengBankStatementParser (永豐)

ParserRegistry: auto-wires all parsers via Spring DI,
resolves by parserKey or bankCode + fileType
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.x |
| Architecture | Modular Monolith (Maven multi-module) |
| Database | MySQL 8.0 |
| Migration | Flyway 9.x (18 scripts, 34 tables) |
| Authentication | Spring Security + JJWT 0.11.5 |
| Cache / Blacklist | Redis (Spring Data Redis) |
| Frontend | Thymeleaf + Bootstrap 5 + Chart.js + SweetAlert2 |
| WebSocket | STOMP over SockJS |
| API Docs | springdoc-openapi 2.8.6 / Swagger UI |
| Containerization | Docker (multi-stage) + docker-compose |
| CI | GitHub Actions |
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
# Set DB_ROOT_PASSWORD and JWT_SECRET in .env

# Start with Docker Compose (MySQL + App)
docker compose up --build

# App:        http://localhost:8111
# Swagger UI: http://localhost:8111/swagger-ui.html
```

**Run tests**
```bash
mvn clean test -Dskip.npm=true
```

---

## Environment Variables

| Variable | Description |
|----------|-------------|
| `DB_ROOT_PASSWORD` | MySQL root password |
| `JWT_SECRET` | JWT signing secret (min 256-bit) |
| `SPRING_DATASOURCE_URL` | JDBC URL (auto-set in Docker Compose) |
| `SPRING_REDIS_HOST` | Redis host |
| `SPRING_REDIS_PORT` | Redis port (default: 6379) |

---

## Database Schema

Managed by Flyway — 18 migration scripts:

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
| V11–V18 | Seed data (banks, test users, demo accounts, exchange rates, dashboard demo) |

---

## API Overview

All responses use a unified `ApiResponse<T>` wrapper.
All exceptions are handled by `GlobalExceptionHandler` using `BusinessException` + `ErrorCode` enum.

| Module | Base Path | Notes |
|--------|-----------|-------|
| Auth | `/api/auth` | register, login, logout |
| Accounts | `/api/accounts` | CRUD + soft delete |
| Credit Cards | `/api/cards` | CRUD + soft delete |
| Transactions | `/api/transactions` | CRUD + balance sync |
| Import | `/api/import` | upload, preview, confirm |
| Dashboard | `/api/dashboard` | summary stats |
| Exchange Rates | `/api/exchange-rates` | |
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

---

## Design Decisions

**Modular Monolith over Microservices** — Controlled complexity at the portfolio stage. Clear module boundaries allow future extraction without premature operational overhead.

**JWT + Session coexistence** — Thymeleaf pages use server-side session; API clients use JWT Bearer tokens. Both paths work simultaneously without requiring a full SPA rewrite.

**Strategy Pattern for bank import** — Adding a new bank format requires only implementing `BankStatementParser` and registering it as a Spring bean. The import pipeline itself is unchanged.

**Redis JWT blacklist** — Stateless JWT requires a blacklist for logout. Each invalidated token is stored in Redis with a TTL equal to the token's remaining lifetime — no unbounded growth, no extra DB calls on every request.

---

## CI/CD

GitHub Actions triggers on push and PR to `dev` / `master`:
```
Checkout → JDK 17 (Temurin) → mvn clean test -Dskip.npm=true
```
Railway auto-deploys from `master` on successful push.
