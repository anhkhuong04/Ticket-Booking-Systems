package com.lak.moviebooking.reporting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import com.lak.moviebooking.operations.application.AdminOperations;
import com.lak.moviebooking.reporting.api.ReportingController;
import com.lak.moviebooking.reporting.application.ReportSummary;
import com.lak.moviebooking.reservation.infrastructure.SeatHoldManagementIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class ReportingSummaryIT extends SeatHoldManagementIT {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired private ReportingController controller;
    @Autowired private AdminOperations operations;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @Transactional
    void countsPaidSeatsByPaymentDateReconcilesRefundsAndExcludesBlockedCapacity() {
        Fixture first = fixture();
        Fixture second = fixture();
        Instant now = Instant.now();
        LocalDate today = now.atZone(ZONE).toLocalDate();
        UUID firstCinema = cinema(first.showtimeId());
        UUID secondCinema = cinema(second.showtimeId());
        UUID firstBooking = booking(first, List.of(first.standardSeatId(), first.coupleSeatOneId(),
                first.coupleSeatTwoId()), "PAID", now.minusSeconds(86_400), 300_000);
        payment(firstBooking, 300_000, now);
        ticket(firstBooking, now);
        jdbc.update("UPDATE showtime_seats SET status='SOLD' WHERE showtime_id=?", first.showtimeId());
        jdbc.update("UPDATE showtimes SET start_at=?,end_at=?,sales_close_at=? WHERE id=?",
                at(today.atStartOfDay(ZONE).toInstant().plusSeconds(36_000)),
                at(today.atStartOfDay(ZONE).toInstant().plusSeconds(41_400)),
                at(today.atStartOfDay(ZONE).toInstant().plusSeconds(35_700)), first.showtimeId());
        jdbc.update("UPDATE showtimes SET start_at=?,end_at=?,sales_close_at=? WHERE id=?",
                at(today.atStartOfDay(ZONE).toInstant().plusSeconds(50_000)),
                at(today.atStartOfDay(ZONE).toInstant().plusSeconds(55_400)),
                at(today.atStartOfDay(ZONE).toInstant().plusSeconds(49_700)), second.showtimeId());
        jdbc.update("UPDATE showtime_seats SET status='BLOCKED' WHERE id=?", second.standardSeatId());

        UUID secondBooking = booking(second, List.of(second.coupleSeatOneId(), second.coupleSeatTwoId()),
                "CANCELLED", now.minusSeconds(172_800), 200_000);
        payment(secondBooking, 200_000, now.minusSeconds(86_400));
        refund(secondBooking, 200_000, now);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM refunds WHERE status='REFUNDED'
                  AND (refunded_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date=?
                """, Long.class, today)).isEqualTo(1);

        AuthenticatedPrincipal admin = principal(UUID.randomUUID(), "SUPER_ADMIN");
        AuthenticatedPrincipal manager = principal(first.firstUserId(), "CINEMA_MANAGER");
        jdbc.update("INSERT INTO staff_cinema_assignments (user_id,cinema_id,assigned_at) VALUES (?,?,?)",
                first.firstUserId(), firstCinema, at(now));

        ReportSummary managerReport = controller.summary(manager, today, today, null);
        assertThat(managerReport.bookings()).isEqualTo(1);
        assertThat(managerReport.seatsSold()).isEqualTo(3);
        assertThat(managerReport.netRevenue()).isEqualTo(300_000);
        assertThat(managerReport.occupancyPercent()).isEqualTo(100);
        assertThat(managerReport.daily()).extracting(ReportSummary.DailyMetric::netRevenue)
                .containsExactly(300_000L);
        assertThat(managerReport.topMovies()).singleElement().satisfies(movie -> {
            assertThat(movie.seatsSold()).isEqualTo(3);
            assertThat(movie.netRevenue()).isEqualTo(300_000);
        });

        ReportSummary global = controller.summary(admin, today, today, null);
        assertThat(global.netRevenue()).isEqualTo(100_000);
        assertThat(global.bookings()).isEqualTo(1);
        assertThat(global.seatsSold()).isEqualTo(3);
        assertThat(global.previousPeriod().netRevenue()).isEqualTo(200_000);
        assertThat(global.previousPeriod().bookings()).isEqualTo(1);
        assertThat(global.previousPeriod().seatsSold()).isEqualTo(2);
        assertThat(global.occupancyPercent()).isEqualTo(60);
        assertThat(global.daily().stream().mapToLong(ReportSummary.DailyMetric::netRevenue).sum())
                .isEqualTo(global.netRevenue());
        assertThat(global.topMovies()).hasSize(2);
        assertThatThrownBy(() -> controller.summary(manager, today, today, secondCinema))
                .isInstanceOf(ApplicationException.class);
        assertThatThrownBy(() -> controller.summary(
                principal(second.firstUserId(), "CINEMA_MANAGER"), today, today, null))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code())
                .isEqualTo("CINEMA_SCOPE_DENIED");
    }

    @Test
    @Transactional
    void distinguishesNormalPaymentFromOperationalExceptionsOutsideDateFilter() {
        Fixture fixture = fixture();
        Instant now = Instant.now();
        LocalDate today = now.atZone(ZONE).toLocalDate();
        booking(fixture, List.of(fixture.standardSeatId()), "PENDING_PAYMENT", now, 100_000);
        ReportSummary normal = controller.summary(principal(UUID.randomUUID(), "SUPER_ADMIN"),
                today.minusDays(30), today.minusDays(30), cinema(fixture.showtimeId()));
        assertThat(normal.alerts().totalBookings()).isZero();

        UUID overdue = booking(fixture, List.of(fixture.coupleSeatOneId()), "PENDING_PAYMENT",
                now.minusSeconds(86_400), 100_000);
        jdbc.update("UPDATE bookings SET payment_deadline=?,hard_deadline=? WHERE id=?",
                at(now.minusSeconds(60)), at(now.minusSeconds(60)), overdue);
        booking(fixture, List.of(fixture.coupleSeatTwoId()), "PAYMENT_REVIEW", now, 100_000);
        booking(fixture, List.of(fixture.standardSeatId()), "PAID", now, 100_000);
        UUID overdueRefund = booking(fixture, List.of(fixture.standardSeatId()), "REFUND_PENDING", now, 100_000);
        payment(overdueRefund, 100_000, now);
        refundInProgress(overdueRefund, "REQUESTED", now.minusSeconds(25 * 3_600));
        UUID failedRefund = booking(fixture, List.of(fixture.standardSeatId()), "CANCELLED", now, 100_000);
        payment(failedRefund, 100_000, now);
        refundInProgress(failedRefund, "REFUND_FAILED", now.minusSeconds(26 * 3_600));
        Fixture cancelled = fixture();
        booking(cancelled, List.of(cancelled.standardSeatId()), "PAYMENT_REVIEW", now, 100_000);
        jdbc.update("UPDATE showtimes SET status='CANCELLED' WHERE id=?", cancelled.showtimeId());
        ReportSummary flagged = controller.summary(principal(UUID.randomUUID(), "SUPER_ADMIN"),
                today.minusDays(30), today.minusDays(30), null);
        assertThat(flagged.alerts().totalBookings()).isEqualTo(6);
        assertThat(flagged.alerts().overduePayments()).isEqualTo(1);
        assertThat(flagged.alerts().paymentReview()).isEqualTo(2);
        assertThat(flagged.alerts().overdueRefunds()).isEqualTo(1);
        assertThat(flagged.alerts().failedRefunds()).isEqualTo(1);
        assertThat(flagged.alerts().cancelledShowtimeBookings()).isEqualTo(1);
        assertThat(flagged.alerts().paidWithoutTicket()).isEqualTo(1);
        Set<UUID> cinemas = Set.of(cinema(fixture.showtimeId()), cinema(cancelled.showtimeId()));
        assertThat(operations.bookings(null, null, "OVERDUE_PAYMENT", null, null, cinemas)).hasSize(1);
        assertThat(operations.bookings(null, null, "OVERDUE_REFUND", null, null, cinemas)).hasSize(1);
        assertThat(operations.bookings(null, null, "CANCELLED_SHOWTIME", null, null, cinemas)).hasSize(1);
        assertThat(operations.bookings(null, null, "PAID_WITHOUT_TICKET", null, null, cinemas)).hasSize(1);
        assertThat(operations.refunds("REQUESTED", true, null, null, cinemas)).hasSize(1);
    }

    private UUID booking(Fixture fixture, List<UUID> seats, String status, Instant created, long amount) {
        UUID hold = UUID.randomUUID();
        UUID booking = UUID.randomUUID();
        OffsetDateTime atCreated = at(created);
        jdbc.update("""
                INSERT INTO seat_holds (id,user_id,showtime_id,status,expires_at,hard_expires_at,created_at,updated_at)
                VALUES (?,?,?,'CONSUMED',?,?,?,?)
                """, hold, fixture.firstUserId(), fixture.showtimeId(), at(created.plusSeconds(600)),
                at(created.plusSeconds(600)), atCreated, atCreated);
        jdbc.update("""
                INSERT INTO bookings (id,booking_code,user_id,showtime_id,hold_id,subtotal,total_amount,status,
                                      payment_deadline,hard_deadline,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """, booking, "LAK-" + booking, fixture.firstUserId(), fixture.showtimeId(), hold,
                amount, amount, status, at(created.plusSeconds(600)), at(created.plusSeconds(600)),
                atCreated, atCreated);
        for (UUID seat : seats) {
            jdbc.update("""
                    INSERT INTO booking_items (id,booking_id,showtime_seat_id,seat_label_snapshot,seat_type_snapshot,
                                               unit_price,created_at)
                    VALUES (?,?,?,'A1','STANDARD',?,?)
                    """, UUID.randomUUID(), booking, seat, amount / seats.size(), atCreated);
        }
        return booking;
    }

    private void payment(UUID booking, long amount, Instant paidAt) {
        UUID payment = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO payments (id,booking_id,provider,provider_transaction_id,amount,currency,status,
                                      paid_at,expires_at,created_at,updated_at)
                VALUES (?,?,'MOCK',?,?,'VND','SUCCESS',?,?,?,?)
                """, payment, booking, payment.toString(), amount, at(paidAt), at(paidAt.plusSeconds(600)),
                at(paidAt), at(paidAt));
    }

    private void refund(UUID booking, long amount, Instant refundedAt) {
        UUID payment = jdbc.queryForObject("SELECT id FROM payments WHERE booking_id=?", UUID.class, booking);
        jdbc.update("""
                INSERT INTO refunds (id,booking_id,payment_id,provider,provider_refund_id,amount,reason,status,
                                     next_attempt_at,requested_at,refunded_at,created_at,updated_at)
                VALUES (?,?,?,'MOCK',?,?,'CUSTOMER_REQUEST','REFUNDED',?,?,?,?,?)
                """, UUID.randomUUID(), booking, payment, UUID.randomUUID().toString(), amount,
                at(refundedAt), at(refundedAt.minusSeconds(60)), at(refundedAt),
                at(refundedAt.minusSeconds(60)), at(refundedAt));
    }

    private void refundInProgress(UUID booking, String status, Instant requestedAt) {
        UUID payment = jdbc.queryForObject("SELECT id FROM payments WHERE booking_id=?", UUID.class, booking);
        jdbc.update("""
                INSERT INTO refunds (id,booking_id,payment_id,provider,amount,reason,status,
                                     next_attempt_at,requested_at,created_at,updated_at)
                VALUES (?,?,?,'MOCK',100000,'CUSTOMER_REQUEST',?,?,?,?,?)
                """, UUID.randomUUID(), booking, payment, status, at(requestedAt), at(requestedAt),
                at(requestedAt), at(requestedAt));
    }

    private void ticket(UUID booking, Instant issuedAt) {
        jdbc.update("""
                INSERT INTO tickets (id,booking_id,ticket_code,qr_token_hash,status,issued_at,created_at,updated_at)
                VALUES (?,?,?,?,'VALID',?,?,?)
                """, UUID.randomUUID(), booking, "TKT-" + booking, "a".repeat(64), at(issuedAt),
                at(issuedAt), at(issuedAt));
    }

    private UUID cinema(UUID showtime) {
        return jdbc.queryForObject("""
                SELECT a.cinema_id FROM showtimes st JOIN auditoriums a ON a.id=st.auditorium_id WHERE st.id=?
                """, UUID.class, showtime);
    }

    private AuthenticatedPrincipal principal(UUID user, String role) {
        return new AuthenticatedPrincipal(user, "report@example.com", "Reporter", Set.of(role));
    }

    private OffsetDateTime at(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
