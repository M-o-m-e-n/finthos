-- Finthos baseline schema.
-- No production database exists yet, so this file is edited in place (see CLAUDE.md).
--
-- Deletion policy: money history is never deleted as a side effect. Only
-- wallets -> users cascades (a user with no history can be removed); every FK into
-- or out of transactions / ledger_entries / processor_authorizations is restrictive.

CREATE TABLE users
(
    id              UUID PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL
        CHECK (role IN ('USER', 'MERCHANT', 'ADMIN')),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wallets
(
    id              UUID PRIMARY KEY,
    user_id         UUID          NOT NULL UNIQUE,
    balance         NUMERIC(19,4) NOT NULL DEFAULT 0
        CHECK (balance >= 0),
    currency        VARCHAR(3)    NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wallet_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE transactions
(
    id                          UUID PRIMARY KEY,
    type                        VARCHAR(20)   NOT NULL
        CHECK (type IN ('TOP_UP', 'TRANSFER', 'PAYMENT', 'REVERSAL')),
    amount                      NUMERIC(19,4) NOT NULL
        CHECK (amount > 0),
    status                      VARCHAR(20)   NOT NULL
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REVERSED')),
    -- "<user id>:<client key>", so keys are scoped per user (FR-7)
    idempotency_key             VARCHAR(255)  NOT NULL UNIQUE,
    source_wallet_id            UUID,
    target_wallet_id            UUID,
    reversed_transaction_id     UUID,
    reversed_by_admin_id        UUID,
    correlation_id              VARCHAR(255),
    created_at                  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tx_source_wallet
        FOREIGN KEY (source_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_tx_target_wallet
        FOREIGN KEY (target_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_tx_reversed_transaction
        FOREIGN KEY (reversed_transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_tx_admin
        FOREIGN KEY (reversed_by_admin_id) REFERENCES users(id),
    CONSTRAINT chk_tx_different_wallets
        CHECK (source_wallet_id IS NULL
            OR target_wallet_id IS NULL
            OR source_wallet_id <> target_wallet_id)
);

CREATE TABLE ledger_entries
(
    id                  UUID PRIMARY KEY,
    wallet_id           UUID          NOT NULL,
    transaction_id      UUID          NOT NULL,
    delta               NUMERIC(19,4) NOT NULL,
    balance_after       NUMERIC(19,4) NOT NULL
        CHECK (balance_after >= 0),
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ledger_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_ledger_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

CREATE TABLE processor_authorizations
(
    id                  UUID PRIMARY KEY,
    transaction_id      UUID          NOT NULL,
    reference           VARCHAR(255)  NOT NULL,
    amount              NUMERIC(19,4) NOT NULL,
    currency            VARCHAR(3)    NOT NULL,
    status              VARCHAR(20)   NOT NULL
        CHECK (status IN ('PENDING', 'APPROVED', 'DECLINED', 'TIMEOUT')),
    auth_code           VARCHAR(100),
    attempt_number      INTEGER       NOT NULL
        CHECK (attempt_number > 0),
    timeout_ms          INTEGER       NOT NULL
        CHECK (timeout_ms > 0),
    requested_at        TIMESTAMP     NOT NULL,
    responded_at        TIMESTAMP,

    CONSTRAINT fk_processor_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

-- UNIQUE columns (users.email, users.username, wallets.user_id, idempotency_key)
-- are already indexed by their constraints.
CREATE INDEX idx_transactions_source_wallet ON transactions(source_wallet_id);
CREATE INDEX idx_transactions_target_wallet ON transactions(target_wallet_id);
CREATE INDEX idx_transactions_status        ON transactions(status);
CREATE INDEX idx_transactions_created_at    ON transactions(created_at);
CREATE INDEX idx_transactions_correlation   ON transactions(correlation_id);
CREATE INDEX idx_ledger_wallet              ON ledger_entries(wallet_id);
CREATE INDEX idx_ledger_transaction         ON ledger_entries(transaction_id);
CREATE INDEX idx_processor_transaction      ON processor_authorizations(transaction_id);
