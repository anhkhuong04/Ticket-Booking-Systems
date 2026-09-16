package com.lak.moviebooking.catalog.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.lak.moviebooking.catalog.application.GenreSummary;
import com.lak.moviebooking.catalog.application.CatalogPage;
import com.lak.moviebooking.catalog.application.MovieCatalogQuery;
import com.lak.moviebooking.catalog.application.MovieDetail;
import com.lak.moviebooking.catalog.application.MovieSearchCriteria;
import com.lak.moviebooking.catalog.application.MovieSummary;
import com.lak.moviebooking.common.application.error.ApplicationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcMovieCatalogQuery implements MovieCatalogQuery {

    private final JdbcTemplate jdbcTemplate;

    JdbcMovieCatalogQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CatalogPage<MovieSummary> findMovies(MovieSearchCriteria criteria) {
        List<Object> parameters = new ArrayList<>();
        String where = whereClause(criteria, parameters);
        long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM movies m" + where, Long.class, parameters.toArray());
        List<MovieSummary> content = jdbcTemplate.query("""
                SELECT m.id, m.title, m.duration_minutes, m.age_rating, m.release_date, m.poster_url, m.status,
                       COALESCE(array_agg(g.name ORDER BY g.name) FILTER (WHERE g.id IS NOT NULL), '{}') AS genres
                FROM movies m
                LEFT JOIN movie_genres mg ON mg.movie_id = m.id
                LEFT JOIN genres g ON g.id = mg.genre_id
                """ + where + """
                GROUP BY m.id
                ORDER BY m.release_date DESC, m.title ASC
                LIMIT ? OFFSET ?
                """, this::mapSummary, withPageParameters(parameters, criteria));
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / criteria.size());
        return new CatalogPage<>(content, criteria.page(), criteria.size(), total, totalPages);
    }

    @Override
    public MovieDetail findMovie(UUID movieId) {
        Optional<MovieDetail> movie = jdbcTemplate.query("""
                SELECT m.id, m.title, m.description, m.duration_minutes, m.age_rating, m.release_date,
                       m.poster_url, m.trailer_url, m.status, m.country, m.director,
                       COALESCE((SELECT array_agg(cm.name ORDER BY cm.display_order) FROM movie_cast_members cm WHERE cm.movie_id=m.id), ARRAY[]::varchar[]) AS cast_members,
                       COALESCE(array_agg(g.name ORDER BY g.name) FILTER (WHERE g.id IS NOT NULL), '{}') AS genres
                FROM movies m
                LEFT JOIN movie_genres mg ON mg.movie_id = m.id
                LEFT JOIN genres g ON g.id = mg.genre_id
                WHERE m.id = ?
                  AND m.status <> 'ARCHIVED'
                GROUP BY m.id
                """, this::mapDetail, movieId).stream().findFirst();
        return movie.orElseThrow(() -> ApplicationException.notFound("MOVIE_NOT_FOUND", "Movie was not found"));
    }

    @Override
    public MovieDetail findMovieForShowtimeCreation(UUID movieId) {
        jdbcTemplate.query("SELECT id FROM movies WHERE id=? FOR UPDATE", (rs, row) -> rs.getObject("id", UUID.class), movieId);
        return findMovie(movieId);
    }

    @Override
    public List<GenreSummary> findGenres() {
        return jdbcTemplate.query("SELECT id, name, slug FROM genres ORDER BY name", (rs, row) ->
                new GenreSummary(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug")));
    }

    private String whereClause(MovieSearchCriteria criteria, List<Object> parameters) {
        List<String> predicates = new ArrayList<>();
        if (criteria.status() != null) {
            predicates.add("m.status = ?");
            parameters.add(criteria.status());
        }
        if (criteria.query() != null) {
            predicates.add("LOWER(m.title) LIKE ?");
            parameters.add("%" + criteria.query().toLowerCase() + "%");
        }
        if (criteria.genre() != null) {
            predicates.add("EXISTS (SELECT 1 FROM movie_genres search_mg JOIN genres search_g ON search_g.id = search_mg.genre_id WHERE search_mg.movie_id = m.id AND search_g.slug = ?)");
            parameters.add(criteria.genre());
        }
        predicates.add("m.status <> 'ARCHIVED'");
        return " WHERE " + String.join(" AND ", predicates);
    }

    private Object[] withPageParameters(List<Object> parameters, MovieSearchCriteria criteria) {
        List<Object> pageParameters = new ArrayList<>(parameters);
        pageParameters.add(criteria.size());
        pageParameters.add((long) criteria.page() * criteria.size());
        return pageParameters.toArray();
    }

    private MovieSummary mapSummary(ResultSet rs, int row) throws SQLException {
        return new MovieSummary(rs.getObject("id", UUID.class), rs.getString("title"), rs.getInt("duration_minutes"),
                rs.getString("age_rating"), rs.getObject("release_date", LocalDate.class), rs.getString("poster_url"),
                rs.getString("status"), genres(rs));
    }

    private MovieDetail mapDetail(ResultSet rs, int row) throws SQLException {
        return new MovieDetail(rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("description"),
                rs.getInt("duration_minutes"), rs.getString("age_rating"), rs.getObject("release_date", LocalDate.class),
                rs.getString("poster_url"), rs.getString("trailer_url"), rs.getString("status"), genres(rs),
                rs.getString("country"), rs.getString("director"), genresOrCast(rs, "cast_members"));
    }

    private List<String> genres(ResultSet rs) throws SQLException {
        return genresOrCast(rs, "genres");
    }

    private List<String> genresOrCast(ResultSet rs, String column) throws SQLException {
        return List.of((String[]) rs.getArray(column).getArray());
    }
}
