CREATE TABLE ticket_scan_logs (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES tickets (id) ON DELETE RESTRICT,
    scanner_user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    cinema_id UUID NOT NULL REFERENCES cinemas (id) ON DELETE RESTRICT,
    result VARCHAR(32) NOT NULL,
    scanned_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_ticket_scan_logs_result CHECK (result IN ('VALID'))
);

CREATE UNIQUE INDEX uq_ticket_scan_logs_success
    ON ticket_scan_logs (ticket_id)
    WHERE result = 'VALID';

CREATE INDEX idx_ticket_scan_logs_cinema_scanned_at
    ON ticket_scan_logs (cinema_id, scanned_at DESC);
