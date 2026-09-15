CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    aggregate_type VARCHAR(120) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    last_error_code VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_outbox_events_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'DEAD_LETTER')),
    CONSTRAINT chk_outbox_events_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_outbox_events_dispatch
    ON outbox_events (available_at, created_at)
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX idx_outbox_events_dead_letter
    ON outbox_events (updated_at DESC)
    WHERE status = 'DEAD_LETTER';
