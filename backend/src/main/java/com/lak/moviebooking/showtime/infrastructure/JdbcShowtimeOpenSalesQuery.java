package com.lak.moviebooking.showtime.infrastructure;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.lak.moviebooking.catalog.application.MovieOpenSalesQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcShowtimeOpenSalesQuery implements MovieOpenSalesQuery {
    private final JdbcTemplate jdbcTemplate;

    JdbcShowtimeOpenSalesQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean hasOpenSales(UUID movieId, Instant now) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM showtimes WHERE movie_id=? AND status='SCHEDULED' AND sales_close_at>?)
                """, Boolean.class, movieId, now.atOffset(ZoneOffset.UTC)));
    }
}
