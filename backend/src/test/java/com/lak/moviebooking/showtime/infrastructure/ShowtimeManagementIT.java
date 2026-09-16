package com.lak.moviebooking.showtime.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.showtime.application.PriceProfileCommand;
import com.lak.moviebooking.showtime.application.PriceRuleCommand;
import com.lak.moviebooking.showtime.application.ShowtimeCreateCommand;
import com.lak.moviebooking.showtime.application.ShowtimeManagement;
import com.lak.moviebooking.showtime.application.ShowtimeCinemaAvailability;
import com.lak.moviebooking.showtime.application.ShowtimeView;
import com.lak.moviebooking.support.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ShowtimeManagementIT extends AbstractIntegrationTest {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private ShowtimeManagement showtimeManagement;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void snapshotsSeatsAndHighestPriorityCinemaPricesAndRejectsOverlap() {
        Fixture fixture = createFixture();
        Instant startAt = Instant.now().plusSeconds(86_400);
        UUID actorId = UUID.randomUUID();
        UUID profileId = showtimeManagement.createPriceProfile(actorId, new PriceProfileCommand(
                fixture.cinemaId(), "Cinema weekday", startAt.atZone(VIETNAM).toLocalDate(), null, "ACTIVE")).id();
        showtimeManagement.createPriceRule(actorId, profileId, new PriceRuleCommand(
                "ANY", null, null, "2D", "STANDARD", 90_000, 0));
        showtimeManagement.createPriceRule(actorId, profileId, new PriceRuleCommand(
                "ANY", null, null, "2D", "STANDARD", 110_000, 10));

        ShowtimeView showtime = showtimeManagement.createShowtime(actorId, new ShowtimeCreateCommand(
                fixture.movieId(), fixture.auditoriumId(), startAt, Map.of("VIP", 180_000L)));

        assertThat(showtime.endAt()).isEqualTo(startAt.plusSeconds(135 * 60));
        assertThat(showtime.salesCloseAt()).isEqualTo(startAt.minusSeconds(300));
        assertThat(showtime.prices()).containsEntry("STANDARD", 110_000L).containsEntry("VIP", 180_000L);
        assertThat(jdbcTemplate.queryForObject("SELECT source FROM showtime_prices WHERE showtime_id=? AND seat_type='STANDARD'", String.class, showtime.id()))
                .isEqualTo("CINEMA_PROFILE");
        assertThat(jdbcTemplate.queryForObject("SELECT source FROM showtime_prices WHERE showtime_id=? AND seat_type='VIP'", String.class, showtime.id()))
                .isEqualTo("SHOWTIME_OVERRIDE");
        assertThat(jdbcTemplate.queryForList("SELECT status FROM showtime_seats WHERE showtime_id=? ORDER BY status", String.class, showtime.id()))
                .containsExactly("AVAILABLE", "BLOCKED");
        assertThat(showtimeManagement.findOpenShowtimes(fixture.movieId(), startAt.atZone(VIETNAM).toLocalDate(), null, Instant.now()))
                .extracting(ShowtimeView::id).contains(showtime.id());
        assertThat(showtimeManagement.findAvailability(fixture.movieId(), Instant.now()).cinemas())
                .extracting(ShowtimeCinemaAvailability::cinemaId).containsExactly(fixture.cinemaId());
        assertThat(showtimeManagement.findAvailability(fixture.movieId(), Instant.now()).cinemas().getFirst().dates())
                .containsExactly(startAt.atZone(VIETNAM).toLocalDate());
        assertThat(showtimeManagement.findSeatMap(showtime.id(), Instant.now()).seats()).hasSize(2);

        assertThatThrownBy(() -> showtimeManagement.createShowtime(actorId, new ShowtimeCreateCommand(
                fixture.movieId(), fixture.auditoriumId(), startAt.plusSeconds(600), Map.of("STANDARD", 100_000L, "VIP", 180_000L))))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code())
                .isEqualTo("SHOWTIME_CONFLICT");

        assertThat(showtimeManagement.cancelShowtime(actorId, showtime.id()).status()).isEqualTo("CANCELLED");
        assertThat(showtimeManagement.findAvailability(fixture.movieId(), Instant.now()).cinemas()).isEmpty();
        ShowtimeView protectedShowtime = showtimeManagement.createShowtime(actorId, new ShowtimeCreateCommand(
                fixture.movieId(), fixture.auditoriumId(), startAt, Map.of("STANDARD", 100_000L, "VIP", 180_000L)));
        jdbcTemplate.update("UPDATE showtime_seats SET status='SOLD' WHERE showtime_id=? AND status='AVAILABLE'", protectedShowtime.id());
        assertThat(showtimeManagement.cancelShowtime(actorId, protectedShowtime.id()).status()).isEqualTo("CANCELLED");
    }

    private Fixture createFixture() {
        UUID cinemaId = UUID.randomUUID();
        UUID auditoriumId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update("INSERT INTO cinemas (id,name,address,city,timezone,status,created_at,updated_at) VALUES (?,?,?,?,?,'ACTIVE',?,?)",
                cinemaId, "Showtime Cinema", "1 Test Street", "Ho Chi Minh City", "Asia/Ho_Chi_Minh", now, now);
        jdbcTemplate.update("INSERT INTO auditoriums (id,cinema_id,name,screen_format,cleanup_minutes,status,created_at,updated_at) VALUES (?,?,?,'2D',15,'ACTIVE',?,?)",
                auditoriumId, cinemaId, "Room 1", now, now);
        jdbcTemplate.update("INSERT INTO seats (id,auditorium_id,row_label,seat_number,seat_type,pair_key,status,created_at,updated_at) VALUES (?,?,?,?,'STANDARD',NULL,'ACTIVE',?,?)",
                UUID.randomUUID(), auditoriumId, "A", 1, now, now);
        jdbcTemplate.update("INSERT INTO seats (id,auditorium_id,row_label,seat_number,seat_type,pair_key,status,created_at,updated_at) VALUES (?,?,?,?,'VIP',NULL,'LOCKED',?,?)",
                UUID.randomUUID(), auditoriumId, "A", 2, now, now);
        jdbcTemplate.update("INSERT INTO movies (id,title,duration_minutes,age_rating,release_date,status,created_at,updated_at) VALUES (?,?,120,'P',?,'NOW_SHOWING',?,?)",
                movieId, "Showtime Movie", LocalDate.now(VIETNAM), now, now);
        return new Fixture(cinemaId, auditoriumId, movieId);
    }

    private record Fixture(UUID cinemaId, UUID auditoriumId, UUID movieId) {
    }
}
