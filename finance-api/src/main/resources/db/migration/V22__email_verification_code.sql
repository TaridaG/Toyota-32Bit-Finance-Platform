CREATE TABLE IF NOT EXISTS email_verification_code (
    email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resend_available_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_email_verification_code PRIMARY KEY (email)
);

CREATE INDEX IF NOT EXISTS idx_email_verification_expires_at
    ON email_verification_code (expires_at);
