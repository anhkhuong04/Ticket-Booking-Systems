CREATE INDEX idx_bookings_admin_created ON bookings (created_at DESC);
CREATE INDEX idx_payments_admin_created ON payments (created_at DESC);
CREATE INDEX idx_refunds_admin_requested ON refunds (requested_at DESC);
CREATE INDEX idx_users_admin_created ON users (created_at DESC);
