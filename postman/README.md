# Finthos API tests (Postman + Newman)

End-to-end checks of the running API against `docs/openapi.yaml`: 56 requests in 9 folders
covering auth, wallets, top-up, transfers, payments, history, idempotency, admin and processor
failures.

| File | What it is |
|---|---|
| `finthos.postman_collection.json` | The collection (Postman v2.1) — import it into Postman to run or edit requests |
| `finthos.local.postman_environment.json` | Environment for a local app (`baseUrl` = `http://localhost:8080`); the other variables are filled in during a run |
| `mock-processor.js` | Stand-in for the external payment processor |
| `run-api-tests.js` | Runs everything automatically with Newman |

## Run it

Prerequisites: JDK 17+, Node.js 18+, Docker running, nothing else listening on port 8080.

```bash
cd postman
npm ci        # first time only
npm test
```

`npm test`:
1. starts the mock processor on port 18072;
2. starts the app with `./mvnw clean spring-boot:run`, pointed at the mock
   (`PROCESSOR_BASE_URL`); Spring's Docker Compose support starts PostgreSQL;
3. runs **00 Setup** (registers fresh users), promotes the merchant and admin users in
   the database, then runs the other folders in order;
4. stops the app and the mock, and exits non-zero if any check failed.

Reports go to `postman/reports/`: `junit.xml` (for CI), `results.json`, and `app.log`.

### Against an app you already started

```bash
npm run mock-processor                                  # terminal 1
PROCESSOR_BASE_URL=http://localhost:18072 ./mvnw spring-boot:run   # terminal 2 (repo root)
npm run test:running-app                                # terminal 3 (postman/)
```

In PowerShell, terminal 2 is: `$env:PROCESSOR_BASE_URL="http://localhost:18072"; .\mvnw.cmd spring-boot:run`

Use a freshly started app: the last folder trips the processor circuit breaker
(see below), so a second run within ~10 s against the same app gets 503s.

Environment overrides: `FINTHOS_DB_URL` (default `postgres://finthos:finthos@localhost:5432/finthos`),
`MOCK_PROCESSOR_PORT` (default `18072`), `APP_START_TIMEOUT_MS` (default `240000`), and
`--base-url` for a different app URL.

## Mock processor

`POST /authorize` decides by the cents of the amount, so requests choose their outcome:

| Amount ends in | Response |
|---|---|
| `.13` | `DECLINED` → API returns 402 |
| `.99` | `TIMEOUT` → retried, then API returns 504 |
| anything else | `APPROVED` with an auth code |

## Running by hand in Postman

1. Import the collection and the environment, select **Finthos - local**.
2. Start the mock processor and the app as in "Against an app you already started".
3. Run the **00 Setup** folder.
4. The API cannot create admins or merchants, so promote them in the database
   (the ids are in the environment, e.g. via Adminer at http://localhost:8888):
   ```sql
   UPDATE users SET role = 'MERCHANT' WHERE id = '<merchantId>';
   UPDATE users SET role = 'ADMIN'    WHERE id = '<adminId>';
   ```
5. Run the remaining folders in order. Each run of **00 Setup** creates new users, so to
   start over, clear `runId` in the environment and run it again.

## Notes

- Balances are asserted exactly (e.g. 100 → 70 → 50 → 80 after the reversal), so folders
  depend on each other and must run in order.
- **08 Processor failures** must stay last: failures count towards the circuit breaker
  (opens at ≥50% failures over the last 10 calls, minimum 5), after which processor-backed
  requests return 503 for 10 s.
- Unauthenticated requests currently return 403 (Spring Security's default), while
  `docs/openapi.yaml` documents 401; those checks accept either.
