ALTER TABLE users
    ADD COLUMN IF NOT EXISTS preferred_locale varchar(8) NOT NULL DEFAULT 'en',
    ADD COLUMN IF NOT EXISTS preferred_currency varchar(8) NOT NULL DEFAULT 'USD';
