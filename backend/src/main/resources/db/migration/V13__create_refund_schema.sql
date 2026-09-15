CREATE TABLE refunds (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE RESTRICT,
    payment_id UUID NOT NULL REFERENCES payments (id) ON DELETE RESTRICT,
    provider VARCHAR(32) NOT NULL,
    provider_refund_id VARCHAR(128),
    amount BIGINT NOT NULL,
    reason VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    refunded_at TIMESTAMPTZ,
    last_error_code VARCHAR(96),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_refunds_payment UNIQUE (payment_id),
    CONSTRAINT uq_refunds_provider_reference UNIQUE (provider, provider_refund_id),
    CONSTRAINT chk_refunds_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_refunds_reason_not_blank CHECK (btrim(reason) <> ''),
    CONSTRAINT chk_refunds_status CHECK (status IN ('REQUESTED', 'REFUNDED', 'REFUND_FAILED')),
    CONSTRAINT chk_refunds_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT chk_refunds_refunded_at CHECK (
        (status = 'REFUNDED' AND refunded_at IS NOT NULL AND provider_refund_id IS NOT NULL)
        OR (status <> 'REFUNDED' AND refunded_at IS NULL)
    )
);

CREATE INDEX idx_refunds_requested ON refunds (next_attempt_at, created_at) WHERE status = 'REQUESTED';
CREATE INDEX idx_refunds_booking ON refunds (booking_id, created_at DESC);

ALTER TABLE bookings DROP CONSTRAINT chk_bookings_status;
ALTER TABLE bookings ADD CONSTRAINT chk_bookings_status CHECK (
    status IN ('PENDING_PAYMENT', 'PAID', 'EXPIRED', 'PAYMENT_REVIEW', 'REFUND_PENDING', 'CANCELLED')
);
