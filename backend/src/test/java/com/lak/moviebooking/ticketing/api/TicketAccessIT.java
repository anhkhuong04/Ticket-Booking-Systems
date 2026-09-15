package com.lak.moviebooking.ticketing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.lak.moviebooking.booking.application.BookingCheckout;
import com.lak.moviebooking.booking.application.BookingCheckoutCommand;
import com.lak.moviebooking.booking.application.BookingView;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import com.lak.moviebooking.reservation.application.SeatHoldCommand;
import com.lak.moviebooking.reservation.application.SeatHoldManagement;
import com.lak.moviebooking.reservation.application.SeatHoldView;
import com.lak.moviebooking.reservation.infrastructure.SeatHoldManagementIT;
import com.lak.moviebooking.ticketing.application.TicketIssuance;
import com.lak.moviebooking.ticketing.application.TicketIssuer;
import com.lak.moviebooking.ticketing.application.TicketBookingSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class TicketAccessIT extends SeatHoldManagementIT {

    @Autowired private TicketController ticketController;
    @Autowired private SeatHoldManagement seatHolds;
    @Autowired private BookingCheckout bookings;
    @Autowired private TicketIssuer ticketIssuer;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void returnsQrOnlyToTheBookingOwnerAndEnforcesStaffCinemaScope() {
        IssuedTicket issued = issueTicket("ticket-access");
        AuthenticatedPrincipal owner = principal(issued.fixture().firstUserId(), Set.of("CUSTOMER"));

        TicketController.TicketResponse ownerTicket = ticketController.ticket(owner, issued.ticket().ticket().ticketCode());
        assertThat(ownerTicket.qrPayload()).isNotBlank().isNotEqualTo(issued.ticket().ticket().ticketCode());
        assertThat(ownerTicket.seatLabels()).containsExactly("A1");

        UUID strangerId = createUser("stranger");
        assertThatThrownBy(() -> ticketController.ticket(principal(strangerId, Set.of("CUSTOMER")), issued.ticket().ticket().ticketCode()))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code()).isEqualTo("TICKET_FORBIDDEN");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM audit_logs
                WHERE actor_id=? AND action='AUTHORIZATION_DENIED'
                  AND metadata->>'requiredAuthority'='TICKET_OWNER_OR_CINEMA_SCOPE'
                """, Integer.class, strangerId)).isEqualTo(1);

        UUID unassignedStaffId = createUser("unassigned-staff");
        assertThatThrownBy(() -> ticketController.ticket(principal(unassignedStaffId, Set.of("TICKET_STAFF")), issued.ticket().ticket().ticketCode()))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code()).isEqualTo("CINEMA_SCOPE_DENIED");

        UUID staffId = createUser("staff");
        jdbcTemplate.update("INSERT INTO staff_cinema_assignments (user_id,cinema_id,assigned_at) VALUES (?,?,?)",
                staffId, cinemaId(issued.booking().id()), OffsetDateTime.now(ZoneOffset.UTC));
        TicketController.TicketResponse staffTicket = ticketController.ticket(principal(staffId, Set.of("TICKET_STAFF")),
                issued.ticket().ticket().ticketCode());
        assertThat(staffTicket.ticketCode()).isEqualTo(issued.ticket().ticket().ticketCode());
        assertThat(staffTicket.qrPayload()).isNull();
    }

    @Test
    void returnsBookingHistoryWithoutQrPayload() {
        IssuedTicket issued = issueTicket("ticket-history");
        List<TicketBookingSummary> history = ticketController.myBookings(principal(issued.fixture().firstUserId(), Set.of("CUSTOMER")));

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().bookingCode()).isEqualTo(issued.booking().bookingCode());
        assertThat(history.getFirst().ticketCode()).isEqualTo(issued.ticket().ticket().ticketCode());
    }

    private IssuedTicket issueTicket(String key) {
        Fixture fixture = fixtureWithPrice();
        SeatHoldView hold = seatHolds.create(fixture.firstUserId(), new SeatHoldCommand(
                fixture.showtimeId(), List.of(fixture.standardSeatId()), "hold-" + key));
        BookingView booking = bookings.checkout(fixture.firstUserId(), new BookingCheckoutCommand(hold.id(), "checkout-" + key));
        jdbcTemplate.update("UPDATE bookings SET status='PAID' WHERE id=?", booking.id());
        return new IssuedTicket(fixture, booking, ticketIssuer.issueForPaidBooking(booking.id()));
    }

    private Fixture fixtureWithPrice() {
        Fixture fixture = fixture();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO showtime_prices (id,showtime_id,seat_type,price,source,created_at,updated_at)
                VALUES (?,?, 'STANDARD',90000,'SYSTEM_PROFILE',?,?)
                """, UUID.randomUUID(), fixture.showtimeId(), now, now);
        return fixture;
    }

    private UUID cinemaId(UUID bookingId) {
        return jdbcTemplate.queryForObject("""
                SELECT cinema.id FROM bookings booking
                JOIN showtimes showtime ON showtime.id=booking.showtime_id
                JOIN auditoriums auditorium ON auditorium.id=showtime.auditorium_id
                JOIN cinemas cinema ON cinema.id=auditorium.cinema_id
                WHERE booking.id=?
                """, UUID.class, bookingId);
    }

    private UUID createUser(String prefix) {
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO users (id,email,password_hash,full_name,status,created_at,updated_at)
                VALUES (?,?, '$2a$10$abcdefghijklmnopqrstuu9c5O0wceg6aKzZ0AtD.GFkX0D3enrkFvC', 'Ticket user','ACTIVE',?,?)
                """, userId, prefix + "-" + userId + "@example.com", now, now);
        return userId;
    }

    private AuthenticatedPrincipal principal(UUID userId, Set<String> roles) {
        return new AuthenticatedPrincipal(userId, userId + "@example.com", "Ticket User", roles);
    }

    private record IssuedTicket(Fixture fixture, BookingView booking, TicketIssuance ticket) { }
}
