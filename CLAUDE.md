# Finthos — Digital Wallet

Java 17 / Spring Boot 3.4 modular monolith (microservice-ready), Spring Data JDBC,
PostgreSQL, Flyway. Full specs live in `docs/` — read them on demand, not all at once.

## Session start
1. Read `MILESTONES.md` first; find the next unchecked item.
2. Read only the doc section that milestone points to. Don't read `docs/SRS.md`,
   `docs/HLD.md` or `docs/LLD.md` in full unless asked — grep for the section
   (e.g. `grep -n "FR-4" docs/SRS.md`).

## Reference docs
- `docs/SRS.md` — requirements (FR-1..FR-8, NFR-1..NFR-8)
- `docs/HLD.md` — architecture, module boundaries, request-flow sequences
- `docs/LLD.md` — class design, schema notes, API contract, build order
- `docs/openapi.yaml` — source of truth for endpoint contracts
- `docs/ERD.mermaid` — schema diagram; must match `V1__baseline.sql`
- When a change alters module boundaries, flows, error codes or the schema, update
  `docs/HLD.md`, `docs/LLD.md`, `docs/openapi.yaml` and `docs/ERD.mermaid` in the same change.

## Build & test
```
./mvnw clean verify
```
- Every `@SpringBootTest` runs against a real PostgreSQL 16 via Testcontainers
  (`jdbc:tc:postgresql:16:///…?TC_DAEMON=true` in `src/test/resources/application.yaml`:
  one container shared by all test contexts) — **Docker must be running**. Plain
  Mockito unit tests need nothing.
- `ModularityTest` fails the build if a module breaks the rules below, and writes
  module diagrams to `target/spring-modulith-docs`.
- Local run: `docker compose up -d` then `./mvnw spring-boot:run` (Flyway migrates on startup).
- API tests: `cd postman && npm ci && npm test` runs the Postman collection with Newman against a
  freshly started app + mock processor (see `postman/README.md`). When an endpoint, status code or
  error code changes, update `postman/finthos.postman_collection.json` in the same change.

## Modules (Spring Modulith)
Top-level packages under `com.alahly.momkn.finthos` are modules. Dependency graph (no cycles):
```
user        -> common
wallet      -> common, user (UserRegistered event)
transaction -> common, wallet
integration -> common
payment     -> common, user, wallet, transaction, integration
admin       -> common, user, wallet, transaction
```
- Other modules may use only a module's **base package** (events such as
  `user.UserRegistered`) and its `service` / `domain` packages, exposed as
  `@NamedInterface` in their `package-info.java`. `repository`, `web`, `mapper`, `config`
  stay internal — another module never touches them (NFR-5).
- Allowed dependencies are declared per module in its base `package-info.java`
  (`@ApplicationModule(allowedDependencies = …)`). Change them deliberately; never widen
  them just to make `ModularityTest` pass.
- `common` is an OPEN shared kernel (errors, security, logging) and must not depend on
  any other module. Business exceptions handled by `GlobalExceptionHandler` live in
  `common.error`.
- Controllers get the caller from `@AuthenticationPrincipal AuthenticatedUser`
  (id, email, role) — don't look the user up through `UserService` just to get an id.
- Cross-module reactions go through events when a direct call would create a cycle
  (e.g. `wallet` creates the wallet on `UserRegistered`, synchronously in the same
  transaction).
- Module-internal helpers are package-private where possible (e.g. `LedgerService`).

## Non-negotiable rules
- Money is always `BigDecimal` / `NUMERIC(19,4)`, never `double`/`float`.
- Modules talk only through each other's `*Service` API (NFR-5); never another
  module's repository, tables or web DTOs.
- Every balance change writes a `ledger_entries` row in the same DB transaction
  (NFR-3). `LedgerService.record` is `Propagation.MANDATORY` to enforce this.
- `/topup`, `/transfers`, `/payments` require `Idempotency-Key`. Keys are scoped per
  user; the DB unique constraint is the real enforcement, `IdempotencyService` is the
  fast path and resolves duplicate-key races.
- Never credit a wallet before the processor returns `APPROVED`. On `DECLINED`/`TIMEOUT`
  the transaction is committed as `FAILED` and balances are untouched. Never hold a DB
  transaction open across the processor call.
- Money history is never deleted: FKs into `transactions`/`ledger_entries` don't cascade.
- Controllers contain no business logic; services contain no HTTP or SQL details.
- Domain entities may carry Spring Data mapping annotations (`@Table`, `@Id`, `@Version`,
  `Persistable`) but nothing else from Spring, and no public setters for state that has
  invariants — balances and statuses change only through domain methods
  (`credit`, `debit`, `markSuccess`, …).

## Database migrations
There is no production or shared database yet, so the schema lives in a single
`V1__baseline.sql` that may be edited in place; after changing it, reset the local DB
with `docker compose down -v`. Once any shared/deployed database exists, switch to
append-only migrations and update this section.

## When you finish a milestone
- Check the box in `MILESTONES.md`.
- Don't start the next milestone in the same turn unless asked — stop and report what
  changed so it can be reviewed.
