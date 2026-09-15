CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320),
    phone VARCHAR(32),
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_users_full_name_not_blank CHECK (btrim(full_name) <> ''),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'LOCKED'))
);

CREATE OR REPLACE FUNCTION normalize_identity_email(raw_value TEXT)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
RETURNS NULL ON NULL INPUT
AS $$
    SELECT NULLIF(lower(btrim(raw_value)), '');
$$;

CREATE OR REPLACE FUNCTION normalize_identity_phone(raw_value TEXT)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
RETURNS NULL ON NULL INPUT
AS $$
    WITH digits_only AS (
        SELECT regexp_replace(btrim(raw_value), '[^0-9]', '', 'g') AS value
    )
    SELECT NULLIF(
        CASE
            WHEN value LIKE '84%' THEN '0' || substring(value FROM 3)
            ELSE value
        END,
        ''
    )
    FROM digits_only;
$$;

CREATE OR REPLACE FUNCTION normalize_identity_user_contacts()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    normalized_phone TEXT;
BEGIN
    NEW.email := normalize_identity_email(NEW.email);
    normalized_phone := normalize_identity_phone(NEW.phone);

    IF NEW.phone IS NOT NULL AND btrim(NEW.phone) <> '' AND normalized_phone IS NULL THEN
        RAISE EXCEPTION 'phone must contain at least one digit'
            USING ERRCODE = '22023';
    END IF;

    NEW.phone := normalized_phone;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_users_normalize_contacts
    BEFORE INSERT OR UPDATE OF email, phone ON users
    FOR EACH ROW
    EXECUTE FUNCTION normalize_identity_user_contacts();

ALTER TABLE users
    ADD CONSTRAINT chk_users_email_is_normalized
        CHECK (email IS NULL OR email = normalize_identity_email(email)),
    ADD CONSTRAINT chk_users_phone_is_normalized
        CHECK (phone IS NULL OR phone = normalize_identity_phone(phone));

CREATE UNIQUE INDEX uq_users_email_normalized
    ON users (email)
    WHERE email IS NOT NULL;

CREATE UNIQUE INDEX uq_users_phone_normalized
    ON users (phone)
    WHERE phone IS NOT NULL;

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_roles_code UNIQUE (code),
    CONSTRAINT chk_roles_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT chk_roles_name_not_blank CHECK (btrim(name) <> '')
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT chk_refresh_tokens_token_hash_sha256
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_refresh_tokens_expiry CHECK (expires_at > created_at),
    CONSTRAINT chk_refresh_tokens_revocation CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE INDEX idx_refresh_tokens_active_by_user
    ON refresh_tokens (user_id, expires_at)
    WHERE revoked_at IS NULL;

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT chk_password_reset_tokens_token_hash_sha256
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_password_reset_tokens_expiry CHECK (expires_at > created_at),
    CONSTRAINT chk_password_reset_tokens_used_at CHECK (used_at IS NULL OR used_at >= created_at)
);

CREATE INDEX idx_password_reset_tokens_active_by_user
    ON password_reset_tokens (user_id, expires_at)
    WHERE used_at IS NULL;

INSERT INTO roles (id, code, name, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000101', 'CUSTOMER', 'Khách hàng', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000102', 'TICKET_STAFF', 'Nhân viên soát vé', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000103', 'CINEMA_MANAGER', 'Quản lý rạp', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000104', 'SUPER_ADMIN', 'Quản trị viên hệ thống', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;
