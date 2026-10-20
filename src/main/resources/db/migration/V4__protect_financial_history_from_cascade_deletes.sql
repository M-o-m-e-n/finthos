-- V3 made deleting a user cascade through wallets into transactions, ledger_entries and
-- processor_authorizations, which silently destroys the audit trail (and the counterparty's
-- history). Money history must never be deleted as a side effect: restore plain FKs so the
-- database refuses such deletes. Users with history are disabled instead (see AdminService).
-- wallets -> users stays ON DELETE CASCADE: a user with no history can still be removed.

ALTER TABLE transactions DROP CONSTRAINT IF EXISTS fk_tx_source_wallet;
ALTER TABLE transactions
    ADD CONSTRAINT fk_tx_source_wallet
        FOREIGN KEY (source_wallet_id) REFERENCES wallets(id);

ALTER TABLE transactions DROP CONSTRAINT IF EXISTS fk_tx_target_wallet;
ALTER TABLE transactions
    ADD CONSTRAINT fk_tx_target_wallet
        FOREIGN KEY (target_wallet_id) REFERENCES wallets(id);

ALTER TABLE transactions DROP CONSTRAINT IF EXISTS fk_tx_reversed_transaction;
ALTER TABLE transactions
    ADD CONSTRAINT fk_tx_reversed_transaction
        FOREIGN KEY (reversed_transaction_id) REFERENCES transactions(id);

ALTER TABLE transactions DROP CONSTRAINT IF EXISTS fk_tx_admin;
ALTER TABLE transactions
    ADD CONSTRAINT fk_tx_admin
        FOREIGN KEY (reversed_by_admin_id) REFERENCES users(id);

ALTER TABLE ledger_entries DROP CONSTRAINT IF EXISTS fk_ledger_wallet;
ALTER TABLE ledger_entries
    ADD CONSTRAINT fk_ledger_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets(id);

ALTER TABLE ledger_entries DROP CONSTRAINT IF EXISTS fk_ledger_transaction;
ALTER TABLE ledger_entries
    ADD CONSTRAINT fk_ledger_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id);

ALTER TABLE processor_authorizations DROP CONSTRAINT IF EXISTS fk_processor_transaction;
ALTER TABLE processor_authorizations
    ADD CONSTRAINT fk_processor_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(id);
