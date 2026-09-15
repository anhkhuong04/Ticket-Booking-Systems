CREATE TABLE seat_holds (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    showtime_id UUID NOT NULL REFERENCES showtimes (id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    hard_expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_seat_holds_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED', 'EXPIRED')),
    CONSTRAINT chk_seat_holds_deadlines CHECK (hard_expires_at >= expires_at),
    CONSTRAINT chk_seat_holds_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_seat_holds_expiry
    ON seat_holds (expires_at, id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_seat_holds_user_created
    ON seat_holds (user_id, created_at DESC);

CREATE TABLE seat_hold_items (
    hold_id UUID NOT NULL REFERENCES seat_holds (id) ON DELETE RESTRICT,
    showtime_seat_id UUID NOT NULL REFERENCES showtime_seats (id) ON DELETE RESTRICT,
    PRIMARY KEY (hold_id, showtime_seat_id)
);

CREATE TABLE idempotency_requests (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    operation VARCHAR(80) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    resource_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency_requests_actor_operation_key
        UNIQUE (actor_id, operation, idempotency_key),
    CONSTRAINT chk_idempotency_requests_key_not_blank CHECK (btrim(idempotency_key) <> ''),
    CONSTRAINT chk_idempotency_requests_hash_sha256 CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_idempotency_requests_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_idempotency_requests_expiry
    ON idempotency_requests (expires_at);

ALTER TABLE showtime_seats
    ADD CONSTRAINT fk_showtime_seats_current_hold
        FOREIGN KEY (current_hold_id) REFERENCES seat_holds (id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_showtime_seats_hold_link
        CHECK (
            (status = 'HELD' AND current_hold_id IS NOT NULL AND hold_expires_at IS NOT NULL)
            OR (status <> 'HELD' AND current_hold_id IS NULL AND hold_expires_at IS NULL)
        );

CREATE INDEX idx_showtime_seats_current_hold
    ON showtime_seats (current_hold_id)
    WHERE current_hold_id IS NOT NULL;
