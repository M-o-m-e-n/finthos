CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    full_name     VARCHAR(150)        NOT NULL,
    email         VARCHAR(150) UNIQUE NOT NULL,
    phone_number  VARCHAR(20) UNIQUE  NOT NULL,
    national_id   VARCHAR(20) UNIQUE,
    kyc_status    VARCHAR(20)         NOT NULL DEFAULT 'PENDING' CHECK (kyc_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    password_hash VARCHAR(255)        NOT NULL,
    status        VARCHAR(20)         NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at    TIMESTAMP           NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP           NOT NULL DEFAULT now()
);

CREATE TABLE accounts
(
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT             NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    account_number VARCHAR(30) UNIQUE NOT NULL,
    account_type   VARCHAR(20)        NOT NULL DEFAULT 'WALLET' CHECK (account_type IN ('WALLET', 'SAVING', 'CURRENT')),
    currency       VARCHAR(3)         NOT NULL DEFAULT 'EGP',
    balance        NUMERIC(18, 2)     NOT NULL DEFAULT 0 CHECK (balance >= 0),
    status         VARCHAR(20)        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at     TIMESTAMP          NOT NULL DEFAULT now()
);

CREATE TABLE transactions
(
    id                     BIGSERIAL PRIMARY KEY,
    reference_code         VARCHAR(40) UNIQUE NOT NULL,
    source_account_id      BIGINT REFERENCES accounts (id),
    destination_account_id BIGINT REFERENCES accounts (id),
    amount                 NUMERIC(18, 2)     NOT NULL CHECK (amount > 0),
    transaction_type       VARCHAR(20)        NOT NULL CHECK ( transaction_type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'PAYMENT') ),
    status                 VARCHAR(20)        NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    description            VARCHAR(255),
    created_at             TIMESTAMP          NOT NULL DEFAULT now()
);

CREATE TABLE cards
(
    id          BIGSERIAL PRIMARY KEY,
    account_id  BIGINT             NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    card_number VARCHAR(20) UNIQUE NOT NULL,
    card_type   VARCHAR(20)        NOT NULL CHECK ( card_type IN ('VIRTUAL', 'PHYSICAL') ),
    expiry_date DATE               NOT NULL,
    cvv_hash    VARCHAR(255)       NOT NULL,
    status      VARCHAR(20)        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at  TIMESTAMP          NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users (id),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id   BIGINT,
    details     JSONB,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_source ON transactions (source_account_id);
CREATE INDEX idx_transactions_dest ON transactions (destination_account_id);
CREATE INDEX idx_accounts_user ON accounts (user_id);
