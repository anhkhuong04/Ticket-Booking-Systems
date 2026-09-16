ALTER TABLE bookings DROP CONSTRAINT chk_bookings_status;
ALTER TABLE bookings ADD CONSTRAINT chk_bookings_status CHECK (
    status IN ('PENDING_PAYMENT', 'PAID', 'EXPIRED', 'PAYMENT_REVIEW', 'REFUND_PENDING', 'REFUNDED', 'CANCELLED')
);

ALTER TABLE seat_holds DROP CONSTRAINT chk_seat_holds_status;
ALTER TABLE seat_holds ADD CONSTRAINT chk_seat_holds_status CHECK (
    status IN ('ACTIVE', 'CONSUMED', 'RELEASED', 'EXPIRED', 'CANCELLED')
);

ALTER TABLE voucher_redemptions ADD COLUMN restored_at TIMESTAMPTZ;
CREATE INDEX idx_voucher_redemptions_active_voucher_user
    ON voucher_redemptions (voucher_id, user_id)
    WHERE restored_at IS NULL;
