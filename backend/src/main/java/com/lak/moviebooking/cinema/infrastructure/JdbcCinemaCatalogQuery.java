package com.lak.moviebooking.cinema.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.lak.moviebooking.cinema.application.CinemaCatalogQuery;
import com.lak.moviebooking.cinema.application.CinemaDetail;
import com.lak.moviebooking.cinema.application.CinemaSummary;
import com.lak.moviebooking.common.application.error.ApplicationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcCinemaCatalogQuery implements CinemaCatalogQuery {

    private final JdbcTemplate jdbcTemplate;

    JdbcCinemaCatalogQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CinemaSummary> findActiveCinemas(String city) {
        if (city == null || city.isBlank()) {
            return jdbcTemplate.query("""
                    SELECT id, name, address, city, timezone FROM cinemas
                    WHERE status = 'ACTIVE' ORDER BY city, name
                    """, (rs, row) -> new CinemaSummary(rs.getObject("id", UUID.class), rs.getString("name"),
                    rs.getString("address"), rs.getString("city"), rs.getString("timezone")));
        }
        return jdbcTemplate.query("""
                SELECT id, name, address, city, timezone FROM cinemas
                WHERE status = 'ACTIVE' AND LOWER(city) = LOWER(?) ORDER BY name
                """, (rs, row) -> new CinemaSummary(rs.getObject("id", UUID.class), rs.getString("name"),
                rs.getString("address"), rs.getString("city"), rs.getString("timezone")), city.trim());
    }

    @Override
    public CinemaDetail findCinema(UUID cinemaId) {
        Optional<CinemaDetail> cinema = jdbcTemplate.query("""
                SELECT id, name, address, city, timezone, status FROM cinemas WHERE id = ?
                """, (rs, row) -> new CinemaDetail(rs.getObject("id", UUID.class), rs.getString("name"),
                rs.getString("address"), rs.getString("city"), rs.getString("timezone"), rs.getString("status")), cinemaId)
                .stream().findFirst();
        return cinema.orElseThrow(() -> ApplicationException.notFound("CINEMA_NOT_FOUND", "Cinema was not found"));
    }
}
