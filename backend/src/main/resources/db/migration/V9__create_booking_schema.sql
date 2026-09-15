CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    booking_code VARCHAR(48) NOT NULL,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    showtime_id UUID NOT NULL REFERENCES showtimes (id) ON DELETE RESTRICT,
    hold_id UUID NOT NULL REFERENCES seat_holds (id) ON DELETE RESTRICT,
    voucher_id UUID,
    subtotal BIGINT NOT NULL,
    discount_amount BIGINT NOT NULL DEFAULT 0,
    service_fee BIGINT NOT NULL DEFAULT 0,
    total_amount BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    payment_deadline TIMESTAMPTZ NOT NULL,
    hard_deadline TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_bookings_code UNIQUE (booking_code),
    CONSTRAINT uq_bookings_hold UNIQUE (hold_id),
    CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'EXPIRED', 'PAYMENT_REVIEW', 'CANCELLED')),
    CONSTRAINT chk_bookings_amounts_non_negative CHECK (
        subtotal >= 0 AND discount_amount >= 0 AND service_fee >= 0 AND total_amount >= 0
    ),
    CONSTRAINT chk_bookings_total CHECK (total_amount = subtotal - discount_amount + service_fee),
    CONSTRAINT chk_bookings_deadlines CHECK (hard_deadline >= payment_deadline)
);

CREATE INDEX idx_bookings_user_created ON bookings (user_id, created_at DESC);
CREATE INDEX idx_bookings_showtime_status ON bookings (showtime_id, status);
CREATE INDEX idx_bookings_payment_deadline ON bookings (payment_deadline) WHERE status = 'PENDING_PAYMENT';

CREATE TABLE booking_items (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE RESTRICT,
    showtime_seat_id UUID NOT NULL REFERENCES showtime_seats (id) ON DELETE RESTRICT,
    seat_label_snapshot VARCHAR(32) NOT NULL,
    seat_type_snapshot VARCHAR(32) NOT NULL,
    unit_price BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_booking_items_label_not_blank CHECK (btrim(seat_label_snapshot) <> ''),
    CONSTRAINT chk_booking_items_price_positive CHECK (unit_price > 0)
);

CREATE INDEX idx_booking_items_booking ON booking_items (booking_id);
CREATE INDEX idx_booking_items_showtime_seat ON booking_items (showtime_seat_id);

CREATE TABLE booking_checkout_requests (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    -- A checkout claims its idempotency key before creating the booking in the same transaction.
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_booking_checkout_requests_actor_key UNIQUE (actor_id, idempotency_key),
    CONSTRAINT chk_booking_checkout_requests_key_not_blank CHECK (btrim(idempotency_key) <> ''),
    CONSTRAINT chk_booking_checkout_requests_hash_sha256 CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_booking_checkout_requests_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_booking_checkout_requests_expiry ON booking_checkout_requests (expires_at);

ALTER TABLE showtime_seats
    ADD CONSTRAINT fk_showtime_seats_current_booking
        FOREIGN KEY (current_booking_id) REFERENCES bookings (id) ON DELETE RESTRICT;
