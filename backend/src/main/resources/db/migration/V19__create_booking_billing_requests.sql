CREATE TABLE booking_billing_requests (
    booking_id UUID PRIMARY KEY REFERENCES bookings (id) ON DELETE RESTRICT,
    recipient_type VARCHAR(16) NOT NULL,
    recipient_name VARCHAR(150) NOT NULL,
    tax_code VARCHAR(32),
    address VARCHAR(500),
    email VARCHAR(320) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_booking_billing_type CHECK (recipient_type IN ('PERSONAL', 'BUSINESS')),
    CONSTRAINT chk_booking_billing_name CHECK (btrim(recipient_name) <> ''),
    CONSTRAINT chk_booking_billing_business CHECK (recipient_type = 'PERSONAL' OR (tax_code IS NOT NULL AND btrim(tax_code) <> '' AND address IS NOT NULL AND btrim(address) <> ''))
);
