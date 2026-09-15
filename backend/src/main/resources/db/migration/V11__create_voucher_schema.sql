CREATE TABLE vouchers (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    discount_type VARCHAR(16) NOT NULL,
    discount_value BIGINT NOT NULL,
    max_discount_amount BIGINT,
    min_order_amount BIGINT NOT NULL DEFAULT 0,
    usage_limit INTEGER,
    usage_count INTEGER NOT NULL DEFAULT 0,
    per_user_limit INTEGER,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_vouchers_code UNIQUE (code),
    CONSTRAINT chk_vouchers_code_normalized CHECK (code = upper(code) AND code = btrim(code)),
    CONSTRAINT chk_vouchers_discount_type CHECK (discount_type IN ('FIXED', 'PERCENT')),
    CONSTRAINT chk_vouchers_discount_value CHECK (
        (discount_type = 'FIXED' AND discount_value > 0)
        OR (discount_type = 'PERCENT' AND discount_value BETWEEN 1 AND 100)
    ),
    CONSTRAINT chk_vouchers_max_discount CHECK (
        max_discount_amount IS NULL OR (discount_type = 'PERCENT' AND max_discount_amount > 0)
    ),
    CONSTRAINT chk_vouchers_min_order CHECK (min_order_amount >= 0),
    CONSTRAINT chk_vouchers_usage_limit CHECK (usage_limit IS NULL OR usage_limit > 0),
    CONSTRAINT chk_vouchers_usage_count CHECK (usage_count >= 0 AND (usage_limit IS NULL OR usage_count <= usage_limit)),
    CONSTRAINT chk_vouchers_per_user_limit CHECK (per_user_limit IS NULL OR per_user_limit > 0),
    CONSTRAINT chk_vouchers_window CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at),
    CONSTRAINT chk_vouchers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_vouchers_active_window ON vouchers (status, starts_at, ends_at);

ALTER TABLE bookings
    ADD COLUMN voucher_code_snapshot VARCHAR(64),
    ADD CONSTRAINT fk_bookings_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers (id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_bookings_voucher_snapshot CHECK (
        (voucher_id IS NULL AND voucher_code_snapshot IS NULL)
        OR (voucher_id IS NOT NULL AND voucher_code_snapshot IS NOT NULL)
    );

CREATE TABLE voucher_redemptions (
    id UUID PRIMARY KEY,
    voucher_id UUID NOT NULL REFERENCES vouchers (id) ON DELETE RESTRICT,
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    discount_amount BIGINT NOT NULL,
    redeemed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_voucher_redemptions_booking UNIQUE (booking_id),
    CONSTRAINT chk_voucher_redemptions_discount CHECK (discount_amount > 0)
);

CREATE INDEX idx_voucher_redemptions_voucher_user ON voucher_redemptions (voucher_id, user_id);
