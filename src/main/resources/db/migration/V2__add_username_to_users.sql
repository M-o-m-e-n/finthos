ALTER TABLE users
    ADD COLUMN IF NOT EXISTS username VARCHAR(50);

UPDATE users
SET username = email
WHERE username IS NULL;

ALTER TABLE users
    ALTER COLUMN username SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username
    ON users(username);

CREATE INDEX IF NOT EXISTS idx_users_username
    ON users(username);
