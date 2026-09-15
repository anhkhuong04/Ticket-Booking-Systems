ALTER TABLE movies DROP CONSTRAINT chk_movies_status;
ALTER TABLE movies ADD CONSTRAINT chk_movies_status CHECK (status IN ('NOW_SHOWING', 'COMING_SOON', 'ARCHIVED'));
