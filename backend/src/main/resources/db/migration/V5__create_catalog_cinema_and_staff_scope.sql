CREATE TABLE movies (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration_minutes INTEGER NOT NULL,
    age_rating VARCHAR(20) NOT NULL,
    release_date DATE NOT NULL,
    poster_url VARCHAR(2048),
    trailer_url VARCHAR(2048),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_movies_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT chk_movies_duration_positive CHECK (duration_minutes > 0),
    CONSTRAINT chk_movies_status CHECK (status IN ('NOW_SHOWING', 'COMING_SOON'))
);

CREATE TABLE genres (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_genres_name UNIQUE (name),
    CONSTRAINT uq_genres_slug UNIQUE (slug),
    CONSTRAINT chk_genres_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT chk_genres_slug_not_blank CHECK (btrim(slug) <> '')
);

CREATE TABLE movie_genres (
    movie_id UUID NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    genre_id UUID NOT NULL REFERENCES genres (id) ON DELETE RESTRICT,
    PRIMARY KEY (movie_id, genre_id)
);

CREATE INDEX idx_movies_status_release_date ON movies (status, release_date);
CREATE INDEX idx_movie_genres_genre_id ON movie_genres (genre_id, movie_id);

CREATE TABLE cinemas (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    city VARCHAR(120) NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_cinemas_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT chk_cinemas_address_not_blank CHECK (btrim(address) <> ''),
    CONSTRAINT chk_cinemas_city_not_blank CHECK (btrim(city) <> ''),
    CONSTRAINT chk_cinemas_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE auditoriums (
    id UUID PRIMARY KEY,
    cinema_id UUID NOT NULL REFERENCES cinemas (id) ON DELETE RESTRICT,
    name VARCHAR(120) NOT NULL,
    screen_format VARCHAR(32) NOT NULL,
    cleanup_minutes INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_auditoriums_cinema_name UNIQUE (cinema_id, name),
    CONSTRAINT chk_auditoriums_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT chk_auditoriums_screen_format_not_blank CHECK (btrim(screen_format) <> ''),
    CONSTRAINT chk_auditoriums_cleanup_non_negative CHECK (cleanup_minutes >= 0),
    CONSTRAINT chk_auditoriums_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE seats (
    id UUID PRIMARY KEY,
    auditorium_id UUID NOT NULL REFERENCES auditoriums (id) ON DELETE RESTRICT,
    row_label VARCHAR(12) NOT NULL,
    seat_number INTEGER NOT NULL,
    seat_type VARCHAR(32) NOT NULL,
    pair_key VARCHAR(120),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_seats_auditorium_position UNIQUE (auditorium_id, row_label, seat_number),
    CONSTRAINT chk_seats_row_label_not_blank CHECK (btrim(row_label) <> ''),
    CONSTRAINT chk_seats_number_positive CHECK (seat_number > 0),
    CONSTRAINT chk_seats_type CHECK (seat_type IN ('STANDARD', 'VIP', 'COUPLE')),
    CONSTRAINT chk_seats_status CHECK (status IN ('ACTIVE', 'LOCKED')),
    CONSTRAINT chk_seats_couple_pair_key CHECK (
        (seat_type = 'COUPLE' AND pair_key IS NOT NULL AND btrim(pair_key) <> '')
        OR (seat_type <> 'COUPLE' AND pair_key IS NULL)
    )
);

CREATE INDEX idx_auditoriums_cinema_id ON auditoriums (cinema_id);
CREATE INDEX idx_seats_auditorium_id ON seats (auditorium_id);

CREATE OR REPLACE FUNCTION assert_couple_seat_pair(pair_auditorium_id UUID, expected_pair_key VARCHAR)
RETURNS VOID AS $$
DECLARE
    pair_count INTEGER;
BEGIN
    IF expected_pair_key IS NULL THEN
        RETURN;
    END IF;

    SELECT count(*) INTO pair_count
    FROM seats
    WHERE auditorium_id = pair_auditorium_id AND pair_key = expected_pair_key;

    IF pair_count <> 2 THEN
        RAISE EXCEPTION 'A couple-seat pair must contain exactly two seats';
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION enforce_couple_seat_pair()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        PERFORM assert_couple_seat_pair(NEW.auditorium_id, NEW.pair_key);
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM assert_couple_seat_pair(OLD.auditorium_id, OLD.pair_key);
    ELSE
        PERFORM assert_couple_seat_pair(OLD.auditorium_id, OLD.pair_key);
        PERFORM assert_couple_seat_pair(NEW.auditorium_id, NEW.pair_key);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_seats_enforce_couple_pair
AFTER INSERT OR UPDATE OR DELETE ON seats
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION enforce_couple_seat_pair();

CREATE TABLE staff_cinema_assignments (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    cinema_id UUID NOT NULL REFERENCES cinemas (id) ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, cinema_id)
);

CREATE INDEX idx_staff_cinema_assignments_cinema_id ON staff_cinema_assignments (cinema_id, user_id);
