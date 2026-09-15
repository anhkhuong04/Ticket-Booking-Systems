CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE RESTRICT,
    ticket_code VARCHAR(48) NOT NULL,
    qr_token_hash CHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_tickets_booking UNIQUE (booking_id),
    CONSTRAINT uq_tickets_code UNIQUE (ticket_code),
    CONSTRAINT uq_tickets_qr_token_hash UNIQUE (qr_token_hash),
    CONSTRAINT chk_tickets_status CHECK (status IN ('VALID', 'USED', 'CANCELLED')),
    CONSTRAINT chk_tickets_qr_token_hash_sha256 CHECK (qr_token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_tickets_used_at CHECK ((status = 'USED' AND used_at IS NOT NULL) OR (status <> 'USED' AND used_at IS NULL))
);

CREATE INDEX idx_tickets_status_issued ON tickets (status, issued_at DESC);
