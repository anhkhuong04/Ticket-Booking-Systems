package com.lak.moviebooking.reservation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.reservation.application.SeatHoldCommand;
import com.lak.moviebooking.reservation.application.SeatHoldManagement;
import com.lak.moviebooking.reservation.application.SeatHoldView;
import com.lak.moviebooking.support.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class SeatHoldManagementIT extends AbstractIntegrationTest {

    @Autowired
    private SeatHoldManagement seatHoldManagement;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void holdsSeatsAtomicallyReplaysIdempotentlyAndExpiresThem() {
        Fixture fixture = fixture();
        SeatHoldCommand command = new SeatHoldCommand(fixture.showtimeId(), List.of(fixture.standardSeatId()), "request-1");

        SeatHoldView created = seatHoldManagement.create(fixture.firstUserId(), command);
        SeatHoldView replay = seatHoldManagement.create(fixture.firstUserId(), command);

        assertThat(replay.id()).isEqualTo(created.id());
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM seat_holds WHERE showtime_id=?", Integer.class, fixture.showtimeId())).isEqualTo(1);
        assertThat(status(fixture.standardSeatId())).isEqualTo("HELD");

        expireHold(created.id());
        assertThat(seatHoldManagement.expireDueHolds(Instant.now())).isEqualTo(1);
        assertThat(status(fixture.standardSeatId())).isEqualTo("AVAILABLE");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM seat_holds WHERE id=?", String.class, created.id())).isEqualTo("EXPIRED");
        assertThat(jdbcTemplate.queryForList("SELECT event_type FROM outbox_events WHERE aggregate_id=?", String.class, created.id()))
                .contains("reservation.seats_updated", "reservation.hold_expired");
    }

    @Test
    void doesNotReplayAnExpiredHoldAsActive() {
        Fixture fixture = fixture();
        SeatHoldCommand command = new SeatHoldCommand(fixture.showtimeId(), List.of(fixture.standardSeatId()), "expired-replay");
        SeatHoldView created = seatHoldManagement.create(fixture.firstUserId(), command);
        expireHold(created.id());

        assertThatThrownBy(() -> seatHoldManagement.create(fixture.firstUserId(), command))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code())
                .isEqualTo("SEAT_HOLD_EXPIRED");

        assertThat(status(fixture.standardSeatId())).isEqualTo("AVAILABLE");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM seat_holds WHERE id=?", String.class, created.id())).isEqualTo("EXPIRED");
    }

    @Test
    void rejectsAnIncompleteCoupleSeatPairWithoutHoldingEitherSeat() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> seatHoldManagement.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.coupleSeatOneId()), "couple-request")))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code())
                .isEqualTo("COUPLE_SEAT_PAIR_REQUIRED");

        assertThat(status(fixture.coupleSeatOneId())).isEqualTo("AVAILABLE");
        assertThat(status(fixture.coupleSeatTwoId())).isEqualTo("AVAILABLE");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM seat_holds WHERE showtime_id=?", Integer.class, fixture.showtimeId())).isZero();
    }

    @Test
    void rollsBackTheEntireSelectionWhenAnyRequestedSeatIsUnavailable() {
        Fixture fixture = fixture();
        seatHoldManagement.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId()), "first-hold"));

        assertThatThrownBy(() -> seatHoldManagement.create(fixture.secondUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId(), fixture.coupleSeatOneId(), fixture.coupleSeatTwoId()), "mixed-hold")))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code())
                .isEqualTo("SEAT_UNAVAILABLE");

        assertThat(status(fixture.coupleSeatOneId())).isEqualTo("AVAILABLE");
        assertThat(status(fixture.coupleSeatTwoId())).isEqualTo("AVAILABLE");
    }

    private String status(UUID showtimeSeatId) {
        return jdbcTemplate.queryForObject("SELECT status FROM showtime_seats WHERE id=?", String.class, showtimeSeatId);
    }

    protected void expireHold(UUID holdId) {
        OffsetDateTime expiredAt = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        jdbcTemplate.update("UPDATE seat_holds SET created_at=?, expires_at=?, updated_at=? WHERE id=?",
                expiredAt.minusMinutes(5), expiredAt, expiredAt, holdId);
    }

    protected Fixture fixture() {
        UUID cinemaId = UUID.randomUUID();
        UUID auditoriumId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        UUID masterStandardSeatId = UUID.randomUUID();
        UUID masterCoupleOneId = UUID.randomUUID();
        UUID masterCoupleTwoId = UUID.randomUUID();
        UUID standardSeatId = UUID.randomUUID();
        UUID coupleSeatOneId = UUID.randomUUID();
        UUID coupleSeatTwoId = UUID.randomUUID();
        UUID firstUserId = user("hold-user-" + UUID.randomUUID());
        UUID secondUserId = user("hold-user-" + UUID.randomUUID());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("INSERT INTO cinemas (id,name,address,city,timezone,status,created_at,updated_at) VALUES (?,?,?,?,?,'ACTIVE',?,?)",
                cinemaId, "Hold Cinema " + cinemaId, "1 Test", "HCM", "Asia/Ho_Chi_Minh", now, now);
        jdbcTemplate.update("INSERT INTO auditoriums (id,cinema_id,name,screen_format,cleanup_minutes,status,created_at,updated_at) VALUES (?,?,?,'2D',0,'ACTIVE',?,?)",
                auditoriumId, cinemaId, "Room " + auditoriumId, now, now);
        seat(masterStandardSeatId, auditoriumId, "A", 1, "STANDARD", null, now);
        coupleSeats(masterCoupleOneId, masterCoupleTwoId, auditoriumId, "B", 1, 2, "B-1-2", now);
        jdbcTemplate.update("INSERT INTO movies (id,title,duration_minutes,age_rating,release_date,status,created_at,updated_at) VALUES (?,?,90,'P',?,'NOW_SHOWING',?,?)",
                movieId, "Hold Movie " + movieId, LocalDate.now(), now, now);
        Instant startAt = Instant.now().plusSeconds(86_400);
        jdbcTemplate.update("INSERT INTO showtimes (id,movie_id,auditorium_id,start_at,end_at,sales_close_at,status,created_at,updated_at) VALUES (?,?,?,?,?,?,'SCHEDULED',?,?)",
                showtimeId, movieId, auditoriumId, atUtc(startAt), atUtc(startAt.plusSeconds(5_400)), atUtc(startAt.minusSeconds(300)), now, now);
        showtimeSeat(standardSeatId, showtimeId, masterStandardSeatId, now);
        showtimeSeat(coupleSeatOneId, showtimeId, masterCoupleOneId, now);
        showtimeSeat(coupleSeatTwoId, showtimeId, masterCoupleTwoId, now);
        return new Fixture(showtimeId, standardSeatId, coupleSeatOneId, coupleSeatTwoId, firstUserId, secondUserId);
    }

    private UUID user(String localPart) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("INSERT INTO users (id,email,password_hash,full_name,status,created_at,updated_at) VALUES (?,?,?,'Hold User','ACTIVE',?,?)",
                id, localPart + "@example.test", "not-used", now, now);
        return id;
    }

    private void seat(UUID id, UUID auditoriumId, String row, int number, String type, String pairKey, OffsetDateTime now) {
        jdbcTemplate.update("INSERT INTO seats (id,auditorium_id,row_label,seat_number,seat_type,pair_key,status,created_at,updated_at) VALUES (?,?,?,?,?,?,'ACTIVE',?,?)",
                id, auditoriumId, row, number, type, pairKey, now, now);
    }

    private void coupleSeats(UUID firstId, UUID secondId, UUID auditoriumId, String row, int firstNumber, int secondNumber,
                             String pairKey, OffsetDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO seats (id,auditorium_id,row_label,seat_number,seat_type,pair_key,status,created_at,updated_at)
                VALUES (?,?,?,?,? ,?,'ACTIVE',?,?), (?,?,?,?,? ,?,'ACTIVE',?,?)
                """, firstId, auditoriumId, row, firstNumber, "COUPLE", pairKey, now, now,
                secondId, auditoriumId, row, secondNumber, "COUPLE", pairKey, now, now);
    }

    private void showtimeSeat(UUID id, UUID showtimeId, UUID seatId, OffsetDateTime now) {
        jdbcTemplate.update("INSERT INTO showtime_seats (id,showtime_id,seat_id,status,created_at,updated_at) VALUES (?,?,?,'AVAILABLE',?,?)",
                id, showtimeId, seatId, now, now);
    }

    private OffsetDateTime atUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    protected record Fixture(UUID showtimeId, UUID standardSeatId, UUID coupleSeatOneId, UUID coupleSeatTwoId,
                             UUID firstUserId, UUID secondUserId) {
    }
}
