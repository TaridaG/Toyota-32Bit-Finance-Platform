-- Immutable Keycloak login username; portal display username may change independently.
ALTER TABLE users ADD COLUMN auth_username VARCHAR(255);

UPDATE users SET auth_username = username WHERE auth_username IS NULL;

ALTER TABLE users ALTER COLUMN auth_username SET NOT NULL;

ALTER TABLE users ADD CONSTRAINT uq_users_auth_username UNIQUE (auth_username);
