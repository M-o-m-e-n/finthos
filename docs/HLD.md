# High-Level Design (HLD)
## Digital Wallet — FinTech Intern Project

| Field | Value |
|---|---|
| Document | High-Level Design |
| Version | 1.0 |
| Stack | Java 17+ / Spring Boot 3.x, PostgreSQL |
| Architecture | Modular-Layered Monolith (microservice-ready) |
| Related | See SRS.md and LLD.md |

---

## 1. Architectural Goals

The system is a **modular-layered monolith**: one deployable unit, but internally divided into self-contained feature modules, each with its own layers. This is deliberately chosen so the team learns clean boundaries now and can extract modules into **microservices** later without rewriting business logic.

Design principles:
- **Layering** — every module follows Controller → Service → Repository → Domain.
- **Modularity** — modules talk to each other only through service interfaces, never by reaching into another module's repository or tables.
- **Dependency direction** — dependencies point inward (web depends on service, service depends on domain). Domain depends on nothing.
- **Externalized integration** — anything outside the system (the mock processor) is reached through one adapter, isolating the rest of the code from external changes.

---

## 2. Layered View

```mermaid
graph TD
    subgraph Presentation Layer
        C1[REST Controllers]
        DTO[DTOs / Request-Response models]
    end
    subgraph Application/Service Layer
        S1[Domain Services]
        S2[Transaction Orchestration]
        IDEMP[Idempotency Handling]
    end
    subgraph Domain Layer
        E1[Entities: User, Wallet, Transaction, LedgerEntry]
        R1[Domain Rules / Validation]
    end
    subgraph Infrastructure Layer
        REPO[JPA Repositories]
        PROC[ProcessorClient adapter]
        DB[(PostgreSQL)]
        SEC[Security / JWT]
    end

    C1 --> DTO
    C1 --> S1
    S1 --> S2
    S2 --> IDEMP
    S1 --> E1
    S1 --> R1
    S1 --> REPO
    S2 --> PROC
    REPO --> DB
    PROC -->|HTTP| MOCK[Mock External Processor]
    C1 --> SEC
```

**Layer responsibilities**

| Layer | Responsibility | Must NOT do |
|---|---|---|
| Presentation | HTTP, request validation, map DTO ⇄ domain | Contain business rules |
| Service/Application | Business logic, orchestration, transactions | Handle HTTP or SQL details |
| Domain | Entities, invariants (e.g. balance ≥ 0) | Know about Spring/web |
| Infrastructure | Persistence, external HTTP, security | Contain business decisions |

---

## 3. Module View (microservice-ready boundaries)

```mermaid
graph LR
    subgraph Monolith Deployable
        UM[User Module]
        WM[Wallet Module]
        TM[Transaction Module]
        PM[Payment Module]
        IM[Integration Module]
    end
    UM -->|user info| WM
    WM -->|balance ops| TM
    PM -->|debit/credit| WM
    PM -->|authorize| IM
    TM -->|authorize top-up| IM
    IM -->|HTTP| MOCK[(Mock Processor)]
```

| Module | Owns | Future Microservice |
|---|---|---|
| **User** | Registration, login, JWT, roles | `user-service` |
| **Wallet** | Balances, ledger, atomic debit/credit | `wallet-service` |
| **Transaction** | Transfers, history, transaction lifecycle | `transaction-service` |
| **Payment** | Merchant payments, top-up orchestration | `payment-service` |
| **Integration** | ProcessorClient adapter, retries, timeouts | `integration-service` / gateway |

Each module is a Java package (`com.company.wallet.<module>`) with sub-packages `web`, `service`, `domain`, `repository`. Cross-module calls go through a published `*Service` interface only — this is the seam along which microservices will later be split.

---

## 4. Request Flow — Top-up (representative money flow)

```mermaid
sequenceDiagram
    actor U as User
    participant API as TopUp Controller
    participant SVC as PaymentService
    participant PROC as ProcessorClient
    participant MOCK as Mock Processor
    participant WAL as WalletService
    participant DB as Database

    U->>API: POST /wallets/topup { amount, Idempotency-Key }
    API->>SVC: topUp(userId, amount, key)
    SVC->>DB: check idempotency key
    alt already processed
        DB-->>SVC: existing result
        SVC-->>API: return cached result
    else new request
        SVC->>DB: create Transaction(PENDING)
        SVC->>PROC: authorize(txId, amount)
        PROC->>MOCK: POST /authorize
        MOCK-->>PROC: APPROVED / DECLINED / TIMEOUT
        alt APPROVED
            PROC-->>SVC: APPROVED
            SVC->>WAL: credit(walletId, amount)
            WAL->>DB: update balance + ledger entry
            SVC->>DB: Transaction -> SUCCESS
            SVC-->>API: 200 success
        else DECLINED or TIMEOUT
            PROC-->>SVC: failure
            SVC->>DB: Transaction -> FAILED (no balance change)
            SVC-->>API: 402 / 504 error
        end
    end
    API-->>U: response
```

---

## 5. Deployment View

**Now (monolith):**
```mermaid
graph TD
    Client[API Client / Postman] -->|REST/JSON| APP[Wallet App - Spring Boot]
    APP --> DBP[(PostgreSQL)]
    APP -->|HTTP| MOCKP[Mock Processor - separate app]
```

**Later (microservices target):**
```mermaid
graph TD
    GW[API Gateway] --> US[user-service]
    GW --> WS[wallet-service]
    GW --> TS[transaction-service]
    GW --> PS[payment-service]
    PS --> IS[integration-service]
    IS -->|HTTP| REALGW[Real / Mock Processor]
    US --> USD[(user db)]
    WS --> WSD[(wallet db)]
    TS --> TSD[(tx db)]
```

The migration path: extract one module at a time, replace in-process service calls with REST/messaging, and give each service its own database. Because modules already avoid shared tables, this is incremental.

---

## 6. Cross-Cutting Concerns

| Concern | Approach |
|---|---|
| Security | Spring Security + JWT filter; role-based access for admin endpoints |
| Transactions | `@Transactional` at service layer; balance updates + ledger in one unit of work |
| Idempotency | `Idempotency-Key` stored with transaction; unique constraint prevents duplicates |
| Resilience | Timeout + bounded retry on ProcessorClient; failure ⇒ transaction FAILED, never inconsistent |
| Error handling | Central `@RestControllerAdvice` mapping domain exceptions to HTTP codes |
| Logging | Correlation ID via filter/MDC; structured logs at module boundaries |
| Config | `application.yml` per profile; processor URL/timeout externalized |

---

## 7. Technology Choices

| Area | Choice | Why |
|---|---|---|
| Language | Java 17+ | Records, modern APIs |
| Framework | Spring Boot 3.x | Standard for FinTech backends |
| Data | Spring Data JPA + PostgreSQL | Relational integrity for money |
| Migrations | Flyway | Versioned schema |
| HTTP client | Spring `RestClient`/`WebClient` | Calls to mock processor |
| Auth | Spring Security + JWT | Simple, standard |
| Testing | JUnit 5, Mockito, Testcontainers | Unit + integration |
| Build | Maven or Gradle | Team preference |

---

## 8. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Interns bypass module boundaries | Code review checklist; package-private repositories |
| Inconsistent balances on processor timeout | Never credit before APPROVED; transactional design |
| Scope creep | Freeze scope to High-priority FRs in SRS |
| Hardcoded processor URL | Enforce externalized config (NFR) |
