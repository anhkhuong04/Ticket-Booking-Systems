package com.lak.moviebooking.ticketing.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.notification.application.EmailNotificationQueue;
import com.lak.moviebooking.notification.application.EmailOutboxPayload;
import com.lak.moviebooking.notification.application.EmailTemplate;
import com.lak.moviebooking.ticketing.application.TicketEmailDelivery;
import com.lak.moviebooking.ticketing.application.TicketEmailResendRateLimit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JdbcTicketEmailDelivery implements TicketEmailDelivery {

    private static final DateTimeFormatter SHOWTIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private final JdbcTemplate jdbcTemplate;
    private final EmailNotificationQueue emailQueue;
    private final TicketEmailResendRateLimit resendRateLimit;

    JdbcTicketEmailDelivery(
            JdbcTemplate jdbcTemplate,
            EmailNotificationQueue emailQueue,
            TicketEmailResendRateLimit resendRateLimit) {
        this.jdbcTemplate = jdbcTemplate;
        this.emailQueue = emailQueue;
        this.resendRateLimit = resendRateLimit;
    }

    @Override
    public void enqueueIssuedTicket(UUID ticketId) {
        enqueue(ticketId, null, false);
    }

    @Override
    @Transactional
    public void resend(UUID ownerId, UUID ticketId) {
        resendRateLimit.check(ownerId, ticketId);
        enqueue(ticketId, ownerId, true);
    }

    private void enqueue(UUID ticketId, UUID ownerId, boolean lock) {
        TicketEmailRow row = ticket(ticketId, lock);
        if (ownerId != null && !ownerId.equals(row.ownerId())) {
            throw ApplicationException.forbidden("TICKET_EMAIL_FORBIDDEN", "You do not have access to this ticket");
        }
        if ("CANCELLED".equals(row.ticketStatus())) {
            throw ApplicationException.businessRule("TICKET_EMAIL_UNAVAILABLE", "A cancelled ticket cannot be emailed");
        }
        emailQueue.enqueue(row.ticketId(), new EmailOutboxPayload(row.recipient(), EmailTemplate.TICKET_ISSUED,
                Map.of(
                        "ticketCode", row.ticketCode(),
                        "bookingCode", row.bookingCode(),
                        "movieTitle", row.movieTitle(),
                        "cinema", row.cinemaName(),
                        "auditorium", row.auditoriumName(),
                        "showtime", SHOWTIME_FORMAT.format(row.startAt()),
                        "seats", String.join(", ", seats(row.bookingId())))));
    }

    private TicketEmailRow ticket(UUID ticketId, boolean lock) {
        String suffix = lock ? " FOR UPDATE OF ticket,booking" : "";
        return jdbcTemplate.query(("""
                SELECT ticket.id AS ticket_id,ticket.status AS ticket_status,ticket.ticket_code,
                       booking.id AS booking_id,booking.booking_code,booking.user_id,
                       customer.email,movie.title AS movie_title,cinema.name AS cinema_name,
                       auditorium.name AS auditorium_name,showtime.start_at
                FROM tickets ticket
                JOIN bookings booking ON booking.id=ticket.booking_id
                JOIN users customer ON customer.id=booking.user_id
                JOIN showtimes showtime ON showtime.id=booking.showtime_id
                JOIN movies movie ON movie.id=showtime.movie_id
                JOIN auditoriums auditorium ON auditorium.id=showtime.auditorium_id
                JOIN cinemas cinema ON cinema.id=auditorium.cinema_id
                WHERE ticket.id=?""" + suffix), (resultSet, rowNumber) -> new TicketEmailRow(
                resultSet.getObject("ticket_id", UUID.class),
                resultSet.getString("ticket_status"),
                resultSet.getString("ticket_code"),
                resultSet.getObject("booking_id", UUID.class),
                resultSet.getString("booking_code"),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getString("email"),
                resultSet.getString("movie_title"),
                resultSet.getString("cinema_name"),
                resultSet.getString("auditorium_name"),
                resultSet.getObject("start_at", OffsetDateTime.class).toInstant()), ticketId)
                .stream().findFirst()
                .orElseThrow(() -> ApplicationException.notFound("TICKET_NOT_FOUND", "Ticket was not found"));
    }

    private List<String> seats(UUID bookingId) {
        return jdbcTemplate.query("""
                SELECT seat_label_snapshot FROM booking_items
                WHERE booking_id=? ORDER BY seat_label_snapshot,showtime_seat_id
                """, (resultSet, rowNumber) -> resultSet.getString("seat_label_snapshot"), bookingId);
    }

    private record TicketEmailRow(
            UUID ticketId, String ticketStatus, String ticketCode, UUID bookingId, String bookingCode, UUID ownerId,
            String recipient, String movieTitle, String cinemaName, String auditoriumName, Instant startAt) {
    }
}
