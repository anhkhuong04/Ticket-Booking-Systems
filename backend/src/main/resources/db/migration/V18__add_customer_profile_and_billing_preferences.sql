ALTER TABLE users ADD COLUMN birth_date DATE;
ALTER TABLE users ADD CONSTRAINT chk_users_birth_date CHECK (birth_date IS NULL OR birth_date >= DATE '1900-01-01');

CREATE TABLE customer_billing_preferences (
    user_id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    recipient_type VARCHAR(16) NOT NULL,
    recipient_name VARCHAR(150) NOT NULL,
    tax_code VARCHAR(32),
    address VARCHAR(500),
    email VARCHAR(320) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_billing_type CHECK (recipient_type IN ('PERSONAL', 'BUSINESS')),
    CONSTRAINT chk_billing_name CHECK (btrim(recipient_name) <> ''),
    CONSTRAINT chk_billing_business CHECK (recipient_type = 'PERSONAL' OR (tax_code IS NOT NULL AND btrim(tax_code) <> '' AND address IS NOT NULL AND btrim(address) <> ''))
);
