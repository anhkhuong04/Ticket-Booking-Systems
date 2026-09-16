CREATE TABLE customer_refund_requests (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE RESTRICT,
    refund_id UUID REFERENCES refunds (id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_customer_refund_requests_actor_key UNIQUE (actor_id, idempotency_key),
    CONSTRAINT chk_customer_refund_requests_key_not_blank CHECK (btrim(idempotency_key) <> ''),
    CONSTRAINT chk_customer_refund_requests_hash_sha256 CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_customer_refund_requests_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_customer_refund_requests_expiry ON customer_refund_requests (expires_at);
