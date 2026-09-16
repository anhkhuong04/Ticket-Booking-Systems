ALTER TABLE movies
    ADD COLUMN country VARCHAR(100),
    ADD COLUMN director VARCHAR(255),
    ADD CONSTRAINT chk_movies_country_not_blank CHECK (country IS NULL OR btrim(country) <> ''),
    ADD CONSTRAINT chk_movies_director_not_blank CHECK (director IS NULL OR btrim(director) <> '');

CREATE TABLE movie_cast_members (
    movie_id UUID NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    display_order SMALLINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    PRIMARY KEY (movie_id, display_order),
    CONSTRAINT chk_movie_cast_order CHECK (display_order >= 0),
    CONSTRAINT chk_movie_cast_name CHECK (btrim(name) <> '')
);
