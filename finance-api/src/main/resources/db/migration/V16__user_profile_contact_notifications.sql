ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone varchar(32),
    ADD COLUMN IF NOT EXISTS notify_security_alerts boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS notify_product_updates boolean NOT NULL DEFAULT false;
