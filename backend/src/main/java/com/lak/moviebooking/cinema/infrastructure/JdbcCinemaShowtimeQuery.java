package com.lak.moviebooking.cinema.infrastructure;

import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.cinema.application.AuditoriumForShowtime;
import com.lak.moviebooking.cinema.application.CinemaShowtimeQuery;
import com.lak.moviebooking.cinema.application.SeatForShowtime;
import com.lak.moviebooking.common.application.error.ApplicationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcCinemaShowtimeQuery implements CinemaShowtimeQuery {
    private final JdbcTemplate jdbcTemplate;
    JdbcCinemaShowtimeQuery(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    @Override public AuditoriumForShowtime findAuditorium(UUID auditoriumId) {
        return jdbcTemplate.query("""
                SELECT a.id, a.cinema_id, c.name AS cinema_name, c.address AS cinema_address, a.screen_format, a.cleanup_minutes
                FROM auditoriums a JOIN cinemas c ON c.id = a.cinema_id
                WHERE a.id = ? AND a.status = 'ACTIVE' AND c.status = 'ACTIVE'
                """, (rs, row) -> new AuditoriumForShowtime(rs.getObject("id", UUID.class), rs.getObject("cinema_id", UUID.class),
                rs.getString("cinema_name"), rs.getString("cinema_address"), rs.getString("screen_format"), rs.getInt("cleanup_minutes")), auditoriumId)
                .stream().findFirst().orElseThrow(() -> ApplicationException.businessRule("INACTIVE_AUDITORIUM", "Auditorium is not active"));
    }
    @Override public List<SeatForShowtime> findSeats(UUID auditoriumId) {
        return jdbcTemplate.query("SELECT id,row_label,seat_number,seat_type,pair_key,status FROM seats WHERE auditorium_id = ? ORDER BY row_label,seat_number",
                (rs, row) -> new SeatForShowtime(rs.getObject("id", UUID.class), rs.getString("row_label"), rs.getInt("seat_number"),
                        rs.getString("seat_type"), rs.getString("pair_key"), rs.getString("status")), auditoriumId);
    }
}
