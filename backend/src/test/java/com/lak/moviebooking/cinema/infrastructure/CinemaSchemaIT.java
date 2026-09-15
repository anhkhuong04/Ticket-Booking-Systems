package com.lak.moviebooking.cinema.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import com.lak.moviebooking.support.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class CinemaSchemaIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void enforcesUniqueSeatPositionsAndExactlyTwoSeatsInEachCouplePair() {
        UUID auditoriumId = createAuditorium();
        Timestamp now = Timestamp.from(Instant.now());
        UUID firstSeat = UUID.randomUUID();
        UUID secondSeat = UUID.randomUUID();

        transactionTemplate.executeWithoutResult(status -> {
            insertSeat(firstSeat, auditoriumId, "A", 1, "COUPLE", "A-PAIR-1", now);
            insertSeat(secondSeat, auditoriumId, "A", 2, "COUPLE", "A-PAIR-1", now);
        });

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status ->
                insertSeat(UUID.randomUUID(), auditoriumId, "A", 3, "COUPLE", "ORPHAN", now)))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(PSQLException.class)
                .hasStackTraceContaining("A couple-seat pair must contain exactly two seats");
        assertThatThrownBy(() -> insertSeat(UUID.randomUUID(), auditoriumId, "A", 1, "STANDARD", null, now))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM seats WHERE auditorium_id = ?", Integer.class, auditoriumId))
                .isEqualTo(2);
    }

    private UUID createAuditorium() {
        UUID cinemaId = UUID.randomUUID();
        UUID auditoriumId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO cinemas (id, name, address, city, timezone, status, created_at, updated_at)
                VALUES (?, 'Schema Cinema', '1 Test Street', 'Ho Chi Minh City', 'Asia/Ho_Chi_Minh', 'ACTIVE', ?, ?)
                """, cinemaId, now, now);
        jdbcTemplate.update("""
                INSERT INTO auditoriums (id, cinema_id, name, screen_format, cleanup_minutes, status, created_at, updated_at)
                VALUES (?, ?, 'Room 1', '2D', 15, 'ACTIVE', ?, ?)
                """, auditoriumId, cinemaId, now, now);
        return auditoriumId;
    }

    private void insertSeat(UUID seatId, UUID auditoriumId, String row, int number, String type, String pairKey, Timestamp now) {
        jdbcTemplate.update("""
                INSERT INTO seats (id, auditorium_id, row_label, seat_number, seat_type, pair_key, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                """, seatId, auditoriumId, row, number, type, pairKey, now, now);
    }
}
