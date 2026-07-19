ALTER TABLE processor_authorizations
    DROP CONSTRAINT IF EXISTS processor_authorizations_status_check;

ALTER TABLE processor_authorizations
    ADD CONSTRAINT processor_authorizations_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'DECLINED', 'TIMEOUT'));
