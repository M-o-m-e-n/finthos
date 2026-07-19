# Finthos — Digital Wallet

A modular-layered monolith (microservice-ready) digital wallet backend built with Java 17 and Spring Boot 3.4.5.

Users can hold balances, top up from external sources, transfer peer-to-peer, pay merchants, and view transaction history. The system integrates with a mock payment processor for authorization.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.4.5 |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| ORM | Spring Data JDBC |
| Object Mapping | MapStruct 1.6.3 |
| Boilerplate | Lombok |
| Logging | Zalando Logbook (JSON) |
| Validation | Jakarta Bean Validation |
| Build | Maven |
| Containerization | Docker Compose |
| Testing | JUnit 5, Mockito, MockWebServer |

## Project Structure

```
com.alahly.momkn.finthos
├── common/          # Security, JWT, error handling, correlation IDs
├── user/            # Registration, login, profile
├── wallet/          # Balance management, optimistic locking, ledger
├── transaction/     # P2P transfers, history, idempotency
├── payment/         # Top-up and merchant payments
├── integration/     # External processor HTTP client
└── admin/           # Admin user management, transaction reversal
```

Each module follows the layered pattern: **web → service → domain → repository**.

## Quick Start

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Maven (or use the included `mvnw`)

### Run

```bash
# Start PostgreSQL + Adminer
docker compose up -d

# Start the application
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. Flyway runs migrations automatically on startup.

**Adminer** (DB UI): `http://localhost:8888` — login: `finthos / finthos / finthos`

### Run Tests

```bash
./mvnw clean verify
```

Tests use an H2 in-memory database (PostgreSQL mode) with a mocked processor — no external dependencies needed.

## API Reference

All endpoints are under `/api`. Authentication uses `Authorization: Bearer <token>`.

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/auth/register` | — | Register (username, email, password). Returns user + JWT. |
| `POST` | `/api/auth/login` | — | Login (email, password). Returns JWT. |

### Users & Wallets

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/users/me` | JWT | Current user profile |
| `GET` | `/api/wallets/me` | JWT | Current user wallet (balance, currency) |
| `POST` | `/api/wallets/topup` | JWT | Top up wallet (requires `Idempotency-Key` header) |

### Transactions

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/transfers` | JWT | P2P transfer (requires `Idempotency-Key` header) |
| `POST` | `/api/payments` | JWT | Merchant payment (requires `Idempotency-Key` header) |
| `GET` | `/api/transactions` | JWT | List transactions (filter: `type`, `from`, `to`, `page`, `size`) |
| `GET` | `/api/transactions/{id}` | JWT | Transaction detail |

### Admin

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/admin/users` | ADMIN | Paginated user list |
| `POST` | `/api/admin/transactions/{id}/reverse` | ADMIN | Reverse a completed transaction |

### Error Response Format

```json
{
  "timestamp": "2026-07-19T10:00:00Z",
  "status": 422,
  "error": "INSUFFICIENT_FUNDS",
  "message": "Insufficient funds: balance 50.00, attempted debit 100.00",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

| Status | Error Code | When |
|--------|------------|------|
| 400 | `VALIDATION_ERROR` | Invalid request body |
| 401 | `UNAUTHORIZED` | Bad credentials / missing token |
| 402 | `PROCESSOR_DECLINED` | Payment processor declined |
| 404 | `NOT_FOUND` | Resource doesn't exist |
| 409 | `EMAIL_ALREADY_EXISTS` / `USERNAME_ALREADY_EXISTS` | Duplicate registration |
| 422 | `INSUFFICIENT_FUNDS` | Not enough balance |
| 500 | `INTERNAL_ERROR` | Unexpected error |
| 504 | `PROCESSOR_TIMEOUT` | Processor didn't respond |

## Database Schema

Six tables managed by Flyway:

- **users** — id, username, email, password_hash, role (USER/MERCHANT/ADMIN), enabled
- **wallets** — id, user_id, balance (NUMERIC 19,4), currency, version (optimistic lock)
- **transactions** — id, type, amount, status, idempotency_key, source/target wallet IDs
- **ledger_entries** — id, wallet_id, transaction_id, delta, balance_after (immutable audit)
- **processor_authorizations** — processor call attempts with status, timing, auth code
- **auth_audit_log** — login attempts with outcome, IP, correlation ID

## Configuration

Key properties in `application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/finthos
    username: finthos
    password: finthos

finthos:
  jwt:
    secret: "your-secret-key"
    expiration-ms: 3600000

processor:
  base-url: http://localhost:8072
  timeout-ms: 2000
  retry-count: 1

logging:
  level:
    org.zalando.logbook: TRACE
```

## Key Design Decisions

- **BigDecimal everywhere** — all monetary values use `BigDecimal` / `NUMERIC(19,4)`
- **Double-entry ledger** — every balance change produces an immutable `ledger_entries` row in the same transaction
- **Optimistic locking** — `Wallet.version` prevents lost updates on concurrent operations
- **Idempotency** — money-moving endpoints require an `Idempotency-Key` header enforced at the DB level
- **Processor-first** — wallets are never credited before the processor returns APPROVED
- **Correlation IDs** — every request gets a UUID for tracing across logs and error responses
