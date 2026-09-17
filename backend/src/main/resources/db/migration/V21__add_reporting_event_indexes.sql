CREATE INDEX idx_payments_reporting_paid_at ON payments (paid_at, booking_id)
    WHERE status = 'SUCCESS';

CREATE INDEX idx_refunds_reporting_refunded_at ON refunds (refunded_at, booking_id)
    WHERE status = 'REFUNDED';

CREATE INDEX idx_refunds_reporting_sla ON refunds (requested_at, booking_id)
    WHERE status = 'REQUESTED';

CREATE INDEX idx_refunds_reporting_failed_booking ON refunds (booking_id)
    WHERE status = 'REFUND_FAILED';
