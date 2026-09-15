CREATE TABLE price_profiles (
    id UUID PRIMARY KEY,
    cinema_id UUID REFERENCES cinemas (id) ON DELETE RESTRICT,
    name VARCHAR(160) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_price_profiles_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT chk_price_profiles_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CONSTRAINT chk_price_profiles_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_price_profiles_cinema_effective ON price_profiles (cinema_id, status, effective_from DESC);
CREATE INDEX idx_price_profiles_system_effective ON price_profiles (status, effective_from DESC) WHERE cinema_id IS NULL;

CREATE TABLE price_rules (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES price_profiles (id) ON DELETE RESTRICT,
    day_type VARCHAR(16) NOT NULL,
    time_from TIME,
    time_to TIME,
    screen_format VARCHAR(32),
    seat_type VARCHAR(32),
    amount BIGINT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_price_rules_day_type CHECK (day_type IN ('ANY', 'WEEKDAY', 'WEEKEND')),
    CONSTRAINT chk_price_rules_time_range CHECK ((time_from IS NULL AND time_to IS NULL) OR (time_from IS NOT NULL AND time_to IS NOT NULL AND time_from < time_to)),
    CONSTRAINT chk_price_rules_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_price_rules_priority_non_negative CHECK (priority >= 0)
);

CREATE INDEX idx_price_rules_profile_priority ON price_rules (profile_id, priority DESC);

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE showtimes (
    id UUID PRIMARY KEY,
    movie_id UUID NOT NULL REFERENCES movies (id) ON DELETE RESTRICT,
    auditorium_id UUID NOT NULL REFERENCES auditoriums (id) ON DELETE RESTRICT,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    sales_close_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_showtimes_period CHECK (end_at > start_at),
    CONSTRAINT chk_showtimes_sales_close CHECK (sales_close_at <= start_at),
    CONSTRAINT chk_showtimes_status CHECK (status IN ('SCHEDULED', 'CANCELLED')),
    CONSTRAINT ex_showtimes_auditorium_period EXCLUDE USING gist (
        auditorium_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    ) WHERE (status = 'SCHEDULED')
);

CREATE INDEX idx_showtimes_movie_start_at ON showtimes (movie_id, start_at);
CREATE INDEX idx_showtimes_auditorium_start_at ON showtimes (auditorium_id, start_at);

CREATE TABLE showtime_prices (
    id UUID PRIMARY KEY,
    showtime_id UUID NOT NULL REFERENCES showtimes (id) ON DELETE RESTRICT,
    seat_type VARCHAR(32) NOT NULL,
    price BIGINT NOT NULL,
    source VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_showtime_prices_type UNIQUE (showtime_id, seat_type),
    CONSTRAINT chk_showtime_prices_price_positive CHECK (price > 0),
    CONSTRAINT chk_showtime_prices_source CHECK (source IN ('SHOWTIME_OVERRIDE', 'CINEMA_PROFILE', 'SYSTEM_PROFILE'))
);

CREATE TABLE showtime_seats (
    id UUID PRIMARY KEY,
    showtime_id UUID NOT NULL REFERENCES showtimes (id) ON DELETE RESTRICT,
    seat_id UUID NOT NULL REFERENCES seats (id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL,
    current_hold_id UUID,
    current_booking_id UUID,
    hold_expires_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_showtime_seats_seat UNIQUE (showtime_id, seat_id),
    CONSTRAINT chk_showtime_seats_status CHECK (status IN ('AVAILABLE', 'HELD', 'PAYMENT_PENDING', 'SOLD', 'BLOCKED')),
    CONSTRAINT chk_showtime_seats_version_non_negative CHECK (version >= 0)
);

CREATE INDEX idx_showtime_seats_showtime_status ON showtime_seats (showtime_id, status);
CREATE INDEX idx_showtime_seats_hold_expires_at ON showtime_seats (hold_expires_at) WHERE hold_expires_at IS NOT NULL;
