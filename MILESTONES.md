# Digital Wallet — Milestone Plan

Read this file first, every session. Work top to bottom; don't skip ahead.
Each milestone lists which doc sections to read — read only those, not the
whole document.

## M0 — Repo & spec bootstrap ✅ (done before implementation starts)
- [x] `docs/SRS.md`, `docs/HLD.md`, `docs/LLD.md` committed
- [x] `docs/ERD.mermaid` — full ERD (users, wallets, transactions, ledger_entries,
      processor_authorizations)
- [x] `docs/openapi.yaml` — validated OpenAPI 3.0 spec, 13 paths

## M1 — Project skeleton + DB wiring + Auth (FR-1, NFR-4)
Read: `LLD.md` §1 (package structure), §7 (schema notes), `SRS.md` FR-1, NFR-4
- [x] Maven/Gradle skeleton with module packages (`user/wallet/transaction/payment/integration/common`)
- [x] Flyway wired to run `docs/migrations/*.sql` on startup
- [x] `User`, `Role` domain classes + `UserRepository`
- [x] `POST /api/auth/register` — 201, 409 on duplicate email
- [x] `POST /api/auth/register` — 409 on duplicate username
- [x] `POST /api/auth/login` — JWT on success, 401 on bad password
- [x] Spring Security + JWT filter (`common/security`)
- [x] BCrypt password hashing
- [x] Unit tests: duplicate email, bad login
- **Definition of done:** can register + login via Postman/curl and get a valid JWT.

## M2 — Wallet module (FR-2)
Read: `LLD.md` §2 (domain model), §7 (wallets table)
- [x] `Wallet` entity with `credit()`/`debit()`, `InsufficientFundsException` on
      `balance - amount < 0`
- [x] Auto-create a zero-balance wallet on user registration
- [x] `WalletRepository` with optimistic locking (`@Version`)
- [x] `GET /api/wallets/me`
- [x] Unit tests: debit below zero throws, credit/debit update balance correctly
- **Definition of done:** new user has exactly one wallet at balance 0; direct
  service-level debit/credit tested without HTTP or processor involved.

## M3 — Transaction module: P2P transfer (FR-4)
Read: `LLD.md` §3 (service layer), §4 (transfer sequence diagram), `SRS.md` FR-4
- [x] `Transaction`, `TxType`, `TxStatus`, `LedgerEntry` domain classes
- [x] `TransferService.transfer()` — single `@Transactional`: debit sender,
      credit receiver, write 2 ledger entries, all-or-nothing rollback
- [x] `POST /api/transfers` — 200 on success, 422 `INSUFFICIENT_FUNDS` on overdraw
- [x] `GlobalExceptionHandler` — first error mappings (`InsufficientFundsException`→422,
      `EntityNotFoundException`→404)
- [x] Integration test: concurrent transfers against the same wallet (optimistic
      lock triggers a retry or a clean failure, not a lost update)
- **Definition of done:** transferring more than balance never partially applies;
  concurrent transfer test passes.

## M4 — Integration module: ProcessorClient (mock processor (using SoapUI) contract)
Read: `LLD.md` §8 (mock processor contract + client sketch), `SRS.md` §4.1
- [x] `ProcessorClient`, `ProcessorRequest`, `ProcessorResponse`
- [x] `ProcessorProperties` bound to `processor.base-url` / `timeout-ms` / `retry-count`
      (externalized config, no hardcoded URL — NFR enforced)
- [x] Bounded retry + timeout on the HTTP call (NFR-6)
- [x] Point `base-url` at a WireMock stub or a stub Spring Boot app for local dev
      (see LLD §8 — you build this mock separately)
- **Definition of done:** `ProcessorClient.authorize()` returns APPROVED/DECLINED/
  TIMEOUT against the stub, with retry/timeout config actually taking effect.

## M5 — Payment module: top-up + merchant payment (FR-3, FR-5)
Read: `LLD.md` §5 (top-up sequence diagram), `HLD.md` §4 (top-up request flow),
`SRS.md` FR-3, FR-5
- [x] `PaymentService.topUp()` — create `PENDING` tx → authorize → credit only on
      `APPROVED` → mark `SUCCESS`/`FAILED`
- [x] `PaymentService.payMerchant()` — same pattern, source = user wallet,
      target = merchant wallet
- [x] `POST /api/wallets/topup`, `POST /api/payments`
- [x] Error mapping: `ProcessorDeclinedException`→402, `ProcessorTimeoutException`→504
- [x] Tests: APPROVED / DECLINED / TIMEOUT paths, balance unchanged on failure
- **Definition of done:** all three processor outcomes verified end-to-end against
  the mock, no balance change on DECLINED or TIMEOUT.

## M6 — Idempotency, logging, error handling (FR-7, NFR-6, NFR-7)
Read: `SRS.md` FR-7, NFR-7; `HLD.md` §6 (cross-cutting concerns)
- [x] `Idempotency-Key` required on topup/transfer/payment; DB unique constraint
      is the enforcement, service checks first for a fast path
- [x] Repeated request with same key returns cached result, no double-charge
- [x] Correlation-ID filter + MDC logging at module boundaries
- [x] `correlationId` populated in the standard error body
- **Definition of done:** same idempotency key sent twice → one balance change,
  both responses identical.

## M7 — Testing pass (NFR-8)
Read: `LLD.md` §9 (testing strategy — must-cover cases list)
- [x] Unit: wallet invariants, service logic with processor mocked
- [x] Integration: full money flows against mock processor + real DB
- [x] API: MockMvc/REST Assured contract + error-code tests
- [x] Explicit coverage: insufficient funds, top-up APPROVED/DECLINED/TIMEOUT,
      duplicate idempotency key, concurrent transfers
- **Definition of done:** every "must-cover case" in LLD §9 has a passing test.

## M8 — Admin: user list + reversal (FR-8)
Read: `SRS.md` FR-8; `LLD.md` §6 (reversal endpoint), `ERD.mermaid`
(`reversed_transaction_id`, `reversed_by_admin_id`)
- [x] `GET /api/admin/users` (admin-only, role check)
- [x] `POST /api/admin/transactions/{id}/reverse` — creates a `REVERSAL` tx +
      compensating ledger entries, restores balances
- [x] 403 for non-admin callers
- **Definition of done:** reversing a transfer restores both wallets to
  pre-transfer balances, with a full audit trail in `ledger_entries`.

## M9 — Hardening & final acceptance
Read: `SRS.md` §7 (Definition of Done, Out of Scope)
- [x] `GET /api/transactions` pagination + filters (type, date range) — FR-6
- [x] `GET /api/transactions/{id}` — single transaction detail
- [x] `GET /api/users/me` — current user profile
- [x] NFR-1 sanity check: wallet ops < 300ms locally, excluding processor latency
- [x] Full run-through of SRS §7 Definition of Done checklist — all High-priority FRs
      implemented and tested
- [x] Confirm nothing in "Out of Scope" crept in (multi-currency, real bank, KYC/AML, UI)
- **Definition of done:** all High-priority FRs pass acceptance criteria as written
  in `SRS.md`; project matches SRS §7 exactly.

---
### How to use this file with the agent
- Start every session with: *"Read MILESTONES.md and continue from the next
  unchecked item."*
- After a milestone is implemented and tested, ask the agent to check the box
  and stop — review the diff before starting the next milestone in a fresh turn.
- If a milestone needs re-scoping, edit this file directly rather than
  re-explaining context verbally each session — it's the persistent memory.
