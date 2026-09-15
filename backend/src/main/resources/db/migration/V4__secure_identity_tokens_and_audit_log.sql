ALTER TABLE refresh_tokens
    ADD COLUMN replaced_by_token_id UUID,
    ADD COLUMN revocation_reason VARCHAR(32);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_replaced_by
        FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens (id),
    ADD CONSTRAINT chk_refresh_tokens_revocation_reason
        CHECK (revocation_reason IS NULL OR revocation_reason IN ('LOGOUT', 'ROTATED', 'REUSE_DETECTED', 'EXPIRED'));

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID,
    action VARCHAR(120) NOT NULL,
    entity_type VARCHAR(120) NOT NULL,
    entity_id UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_audit_logs_action_not_blank CHECK (btrim(action) <> ''),
    CONSTRAINT chk_audit_logs_entity_type_not_blank CHECK (btrim(entity_type) <> '')
);

CREATE INDEX idx_audit_logs_actor_created_at
    ON audit_logs (actor_id, created_at DESC)
    WHERE actor_id IS NOT NULL;
