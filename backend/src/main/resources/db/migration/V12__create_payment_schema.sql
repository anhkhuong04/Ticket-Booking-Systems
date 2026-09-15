CREATE TABLE payments (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE RESTRICT,
    provider VARCHAR(32) NOT NULL,
    provider_transaction_id VARCHAR(128) NOT NULL,
    provider_event_id VARCHAR(128),
    amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    paid_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_payments_provider_transaction UNIQUE (provider, provider_transaction_id),
    CONSTRAINT chk_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_payments_currency CHECK (currency = 'VND'),
    CONSTRAINT chk_payments_status CHECK (status IN ('INITIATED', 'SUCCESS', 'FAILED', 'EXPIRED')),
    CONSTRAINT chk_payments_paid_at CHECK ((status = 'SUCCESS' AND paid_at IS NOT NULL) OR (status <> 'SUCCESS' AND paid_at IS NULL))
);

CREATE UNIQUE INDEX uq_payments_booking_success ON payments (booking_id) WHERE status = 'SUCCESS';
CREATE INDEX idx_payments_booking_status ON payments (booking_id, status);
CREATE INDEX idx_payments_pending_expiry ON payments (expires_at) WHERE status = 'INITIATED';

CREATE TABLE payment_events (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments (id) ON DELETE RESTRICT,
    provider_event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_payment_events_provider_event UNIQUE (provider_event_id),
    CONSTRAINT chk_payment_events_event_not_blank CHECK (btrim(event_type) <> ''),
    CONSTRAINT chk_payment_events_hash_sha256 CHECK (payload_hash ~ '^[0-9a-f]{64}$')
);

CREATE INDEX idx_payment_events_payment ON payment_events (payment_id, received_at DESC);
