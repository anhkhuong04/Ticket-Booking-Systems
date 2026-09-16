package com.lak.moviebooking.catalog.infrastructure;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.lak.moviebooking.audit.application.AuditLogWriter;
import com.lak.moviebooking.catalog.application.CatalogAdministration;
import com.lak.moviebooking.catalog.application.GenreSummary;
import com.lak.moviebooking.catalog.application.GenreWriteCommand;
import com.lak.moviebooking.catalog.application.MovieDetail;
import com.lak.moviebooking.catalog.application.MovieOpenSalesQuery;
import com.lak.moviebooking.catalog.application.MovieWriteCommand;
import com.lak.moviebooking.common.application.error.ApplicationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JdbcCatalogAdministration implements CatalogAdministration {

    private static final Set<String> MOVIE_STATUSES = Set.of("NOW_SHOWING", "COMING_SOON");
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogWriter auditLogWriter;
    private final MovieOpenSalesQuery movieOpenSalesQuery;
    private final Clock clock;

    JdbcCatalogAdministration(JdbcTemplate jdbcTemplate, AuditLogWriter auditLogWriter,
            MovieOpenSalesQuery movieOpenSalesQuery, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogWriter = auditLogWriter;
        this.movieOpenSalesQuery = movieOpenSalesQuery;
        this.clock = clock;
    }

    @Override
    public List<MovieDetail> movies() {
        return jdbcTemplate.query("SELECT id FROM movies ORDER BY release_date DESC, title", (rs, row) ->
                findMovie(rs.getObject("id", UUID.class)));
    }

    @Override
    @Transactional
    public MovieDetail createMovie(UUID actorId, MovieWriteCommand command) {
        validate(command);
        UUID movieId = UUID.randomUUID();
        OffsetDateTime now = now();
        jdbcTemplate.update("""
                INSERT INTO movies (id, title, description, duration_minutes, age_rating, release_date, poster_url, trailer_url, status, country, director, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, movieId, command.title().trim(), nullable(command.description()), command.durationMinutes(), command.ageRating().trim(),
                command.releaseDate(), nullable(command.posterUrl()), nullable(command.trailerUrl()), command.status(),
                nullable(command.country()), nullable(command.director()), now, now);
        replaceGenres(movieId, command.genreIds());
        replaceCast(movieId, command.castMembers());
        auditLogWriter.record(actorId, "MOVIE_CREATED", "movie", movieId, Map.of("status", command.status()));
        return findMovie(movieId);
    }

    @Override
    @Transactional
    public MovieDetail updateMovie(UUID actorId, UUID movieId, MovieWriteCommand command) {
        validate(command);
        requireMovie(movieId);
        int changed = jdbcTemplate.update("""
                UPDATE movies SET title = ?, description = ?, duration_minutes = ?, age_rating = ?, release_date = ?,
                    poster_url = ?, trailer_url = ?, status = ?, country = ?, director = ?, updated_at = ? WHERE id = ?
                """, command.title().trim(), nullable(command.description()), command.durationMinutes(), command.ageRating().trim(),
                command.releaseDate(), nullable(command.posterUrl()), nullable(command.trailerUrl()), command.status(),
                nullable(command.country()), nullable(command.director()), now(), movieId);
        if (changed != 1) {
            throw ApplicationException.notFound("MOVIE_NOT_FOUND", "Movie was not found");
        }
        replaceGenres(movieId, command.genreIds());
        replaceCast(movieId, command.castMembers());
        auditLogWriter.record(actorId, "MOVIE_UPDATED", "movie", movieId, Map.of("status", command.status()));
        return findMovie(movieId);
    }

    @Override
    @Transactional
    public void archiveMovie(UUID actorId, UUID movieId) {
        lockMovie(movieId);
        if (movieOpenSalesQuery.hasOpenSales(movieId, clock.instant())) {
            throw ApplicationException.conflict("MOVIE_HAS_OPEN_SHOWTIMES", "Close or cancel open showtimes before archiving this movie");
        }
        jdbcTemplate.update("UPDATE movies SET status = 'ARCHIVED', updated_at = ? WHERE id = ?", now(), movieId);
        auditLogWriter.record(actorId, "MOVIE_ARCHIVED", "movie", movieId, Map.of());
    }

    @Override
    @Transactional
    public GenreSummary createGenre(UUID actorId, GenreWriteCommand command) {
        validate(command);
        UUID genreId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO genres (id, name, slug, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                genreId, command.name().trim(), command.slug().trim(), now(), now());
        auditLogWriter.record(actorId, "GENRE_CREATED", "genre", genreId, Map.of());
        return new GenreSummary(genreId, command.name().trim(), command.slug().trim());
    }

    @Override
    @Transactional
    public GenreSummary updateGenre(UUID actorId, UUID genreId, GenreWriteCommand command) {
        validate(command);
        int changed = jdbcTemplate.update("UPDATE genres SET name = ?, slug = ?, updated_at = ? WHERE id = ?",
                command.name().trim(), command.slug().trim(), now(), genreId);
        if (changed != 1) {
            throw ApplicationException.notFound("GENRE_NOT_FOUND", "Genre was not found");
        }
        auditLogWriter.record(actorId, "GENRE_UPDATED", "genre", genreId, Map.of());
        return new GenreSummary(genreId, command.name().trim(), command.slug().trim());
    }

    @Override
    @Transactional
    public void deleteGenre(UUID actorId, UUID genreId) {
        Long usage = jdbcTemplate.queryForObject("SELECT count(*) FROM movie_genres WHERE genre_id = ?", Long.class, genreId);
        if (usage != null && usage > 0) {
            throw ApplicationException.conflict("GENRE_IN_USE", "Genre is assigned to one or more movies");
        }
        if (jdbcTemplate.update("DELETE FROM genres WHERE id = ?", genreId) != 1) {
            throw ApplicationException.notFound("GENRE_NOT_FOUND", "Genre was not found");
        }
        auditLogWriter.record(actorId, "GENRE_DELETED", "genre", genreId, Map.of());
    }

    private MovieDetail findMovie(UUID movieId) {
        return jdbcTemplate.query("""
                SELECT m.id, m.title, m.description, m.duration_minutes, m.age_rating, m.release_date,
                       m.poster_url, m.trailer_url, m.status, m.country, m.director,
                       COALESCE((SELECT array_agg(cm.name ORDER BY cm.display_order) FROM movie_cast_members cm WHERE cm.movie_id=m.id), ARRAY[]::varchar[]) AS cast_members,
                       COALESCE(array_agg(g.name ORDER BY g.name) FILTER (WHERE g.id IS NOT NULL), '{}') AS genres
                FROM movies m LEFT JOIN movie_genres mg ON mg.movie_id = m.id LEFT JOIN genres g ON g.id = mg.genre_id
                WHERE m.id = ? GROUP BY m.id
                """, (rs, row) -> new MovieDetail(rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("description"),
                rs.getInt("duration_minutes"), rs.getString("age_rating"), rs.getObject("release_date", java.time.LocalDate.class),
                rs.getString("poster_url"), rs.getString("trailer_url"), rs.getString("status"),
                List.of((String[]) rs.getArray("genres").getArray()), rs.getString("country"), rs.getString("director"),
                List.of((String[]) rs.getArray("cast_members").getArray())), movieId).stream().findFirst()
                .orElseThrow(() -> ApplicationException.notFound("MOVIE_NOT_FOUND", "Movie was not found"));
    }

    private void replaceGenres(UUID movieId, List<UUID> genreIds) {
        jdbcTemplate.update("DELETE FROM movie_genres WHERE movie_id = ?", movieId);
        for (UUID genreId : genreIds) {
            int inserted = jdbcTemplate.update("""
                    INSERT INTO movie_genres (movie_id, genre_id)
                    SELECT ?, id FROM genres WHERE id = ?
                    """, movieId, genreId);
            if (inserted != 1) {
                throw ApplicationException.businessRule("INVALID_GENRE", "One or more genres do not exist");
            }
        }
    }

    private void replaceCast(UUID movieId, List<String> members) {
        jdbcTemplate.update("DELETE FROM movie_cast_members WHERE movie_id=?", movieId);
        for (int order = 0; order < members.size(); order++) {
            jdbcTemplate.update("INSERT INTO movie_cast_members (movie_id,display_order,name) VALUES (?,?,?)",
                    movieId, order, members.get(order).trim());
        }
    }

    private void requireMovie(UUID movieId) {
        if (!Boolean.TRUE.equals(jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM movies WHERE id = ?)", Boolean.class, movieId))) {
            throw ApplicationException.notFound("MOVIE_NOT_FOUND", "Movie was not found");
        }
    }

    private void lockMovie(UUID movieId) {
        if (jdbcTemplate.query("SELECT id FROM movies WHERE id=? FOR UPDATE", (rs, row) -> rs.getObject("id", UUID.class), movieId).isEmpty()) {
            throw ApplicationException.notFound("MOVIE_NOT_FOUND", "Movie was not found");
        }
    }

    private void validate(MovieWriteCommand command) {
        if (command.durationMinutes() <= 0 || command.title() == null || command.title().isBlank()
                || command.ageRating() == null || command.ageRating().isBlank() || command.releaseDate() == null
                || !MOVIE_STATUSES.contains(command.status()) || command.genreIds().stream().distinct().count() != command.genreIds().size()
                || command.country() != null && command.country().length() > 100
                || command.director() != null && command.director().length() > 255
                || command.castMembers().size() > 30
                || command.castMembers().stream().anyMatch(member -> member == null || member.isBlank() || member.length() > 150)) {
            throw ApplicationException.businessRule("INVALID_MOVIE", "Movie data is invalid");
        }
    }

    private void validate(GenreWriteCommand command) {
        if (command.name() == null || command.name().isBlank() || command.slug() == null
                || !command.slug().matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw ApplicationException.businessRule("INVALID_GENRE", "Genre data is invalid");
        }
    }

    private OffsetDateTime now() { return clock.instant().atOffset(ZoneOffset.UTC); }
    private String nullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
