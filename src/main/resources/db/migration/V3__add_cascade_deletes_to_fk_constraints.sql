-- Fix FK cascade chains so DELETE FROM users propagates correctly:
--   users → wallets (already CASCADE) → transactions → processor_authorizations
--                                             → ledger_entries
--
-- Also handle reversed_by_admin_id with SET NULL (preserve tx history).

-- transactions → wallets (source)
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS fk_tx_source_wallet;
ALTER TABLE transactions
    ADD CONSTRAINT fk_tx_source_wallet
        FOREIGN KEY (source_wallet_id) REFERENCES wallets(id)
        ON DELETE CASCADE;

-- transactions → wallets (target)
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS fk_tx_target_wallet;
ALTER TABLE transactions
    ADD CONSTRAINT fk_tx_target_wallet
        FOREIGN KEY (target_wallet_id) REFERENCES wallets(id)
        ON DELETE CASCADE;

-- transactions → transactions (self-ref: reversed_transaction_id)
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS fk_tx_reversed_transaction;
ALTER TABLE transactions
    ADD CONSTRAINT fk_tx_reversed_transaction
        FOREIGN KEY (reversed_transaction_id) REFERENCES transactions(id)
        ON DELETE SET NULL;

-- transactions → users (reversed_by_admin_id)
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS fk_tx_admin;
ALTER TABLE transactions
    ADD CONSTRAINT fk_tx_admin
        FOREIGN KEY (reversed_by_admin_id) REFERENCES users(id)
        ON DELETE SET NULL;

-- ledger_entries → wallets
ALTER TABLE ledger_entries DROP CONSTRAINT IF EXISTS fk_ledger_wallet;
ALTER TABLE ledger_entries
    ADD CONSTRAINT fk_ledger_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets(id)
        ON DELETE CASCADE;

-- ledger_entries → transactions
ALTER TABLE ledger_entries DROP CONSTRAINT IF EXISTS fk_ledger_transaction;
ALTER TABLE ledger_entries
    ADD CONSTRAINT fk_ledger_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id)
        ON DELETE CASCADE;

-- processor_authorizations → transactions
ALTER TABLE processor_authorizations DROP CONSTRAINT IF EXISTS fk_processor_transaction;
ALTER TABLE processor_authorizations
    ADD CONSTRAINT fk_processor_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id)
        ON DELETE CASCADE;
