package com.lak.moviebooking.ticketing.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.ticketing.application.TicketBookingSummary;
import com.lak.moviebooking.ticketing.application.TicketLookup;
import com.lak.moviebooking.ticketing.application.TicketQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
class JdbcTicketQuery implements TicketQuery {

    private final JdbcTemplate jdbcTemplate;

    JdbcTicketQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TicketLookup findByCode(String ticketCode) {
        if (ticketCode == null || ticketCode.isBlank()) {
            throw ApplicationException.notFound("TICKET_NOT_FOUND", "Ticket was not found");
        }
        TicketRow row = jdbcTemplate.query("""
                SELECT ticket.id AS ticket_id,ticket.ticket_code,ticket.status AS ticket_status,ticket.issued_at,ticket.used_at,
                       booking.id AS booking_id,booking.booking_code,booking.status AS booking_status,booking.user_id,booking.created_at,
                       (SELECT payment.status FROM payments payment WHERE payment.booking_id=booking.id ORDER BY payment.created_at DESC LIMIT 1) AS payment_status,
                       (SELECT refund.status FROM refunds refund WHERE refund.booking_id=booking.id ORDER BY refund.created_at DESC LIMIT 1) AS refund_status,
                       movie.title AS movie_title,movie.poster_url,cinema.id AS cinema_id,cinema.name AS cinema_name,
                       auditorium.name AS auditorium_name,showtime.start_at
                FROM tickets ticket
                JOIN bookings booking ON booking.id=ticket.booking_id
                JOIN showtimes showtime ON showtime.id=booking.showtime_id
                JOIN movies movie ON movie.id=showtime.movie_id
                JOIN auditoriums auditorium ON auditorium.id=showtime.auditorium_id
                JOIN cinemas cinema ON cinema.id=auditorium.cinema_id
                WHERE ticket.ticket_code=?
                """, this::mapRow, ticketCode.trim()).stream().findFirst()
                .orElseThrow(() -> ApplicationException.notFound("TICKET_NOT_FOUND", "Ticket was not found"));
        return new TicketLookup(row.ownerId(), row.cinemaId(), detail(row));
    }

    @Override
    public List<TicketBookingSummary> findBookingsForOwner(UUID userId) {
        List<TicketRow> rows = jdbcTemplate.query("""
                SELECT ticket.id AS ticket_id,ticket.ticket_code,ticket.status AS ticket_status,ticket.issued_at,ticket.used_at,
                       booking.id AS booking_id,booking.booking_code,booking.status AS booking_status,booking.user_id,booking.created_at,
                       (SELECT payment.status FROM payments payment WHERE payment.booking_id=booking.id ORDER BY payment.created_at DESC LIMIT 1) AS payment_status,
                       (SELECT refund.status FROM refunds refund WHERE refund.booking_id=booking.id ORDER BY refund.created_at DESC LIMIT 1) AS refund_status,
                       movie.title AS movie_title,movie.poster_url,cinema.id AS cinema_id,cinema.name AS cinema_name,
                       auditorium.name AS auditorium_name,showtime.start_at
                FROM bookings booking
                JOIN showtimes showtime ON showtime.id=booking.showtime_id
                JOIN movies movie ON movie.id=showtime.movie_id
                JOIN auditoriums auditorium ON auditorium.id=showtime.auditorium_id
                JOIN cinemas cinema ON cinema.id=auditorium.cinema_id
                LEFT JOIN tickets ticket ON ticket.booking_id=booking.id
                WHERE booking.user_id=?
                ORDER BY showtime.start_at DESC,booking.created_at DESC
                """, this::mapRow, userId);
        return rows.stream().map(row -> new TicketBookingSummary(row.bookingId(), row.bookingCode(), row.bookingStatus(),
                row.movieTitle(), row.posterUrl(), row.cinemaName(), row.auditoriumName(), row.startAt(), seats(row.bookingId()),
                row.ticketCode(), row.ticketStatus(), row.createdAt(), row.paymentStatus(), row.refundStatus())).toList();
    }

    private TicketLookup.TicketDetailView detail(TicketRow row) {
        return new TicketLookup.TicketDetailView(row.ticketId(), row.ticketCode(), row.ticketStatus(), row.issuedAt(), row.usedAt(),
                row.bookingCode(), row.bookingStatus(), row.movieTitle(), row.posterUrl(), row.cinemaName(), row.auditoriumName(),
                row.startAt(), seats(row.bookingId()));
    }

    private List<String> seats(UUID bookingId) {
        return jdbcTemplate.query("""
                SELECT seat_label_snapshot FROM booking_items
                WHERE booking_id=? ORDER BY seat_label_snapshot,showtime_seat_id
                """, (resultSet, rowNumber) -> resultSet.getString("seat_label_snapshot"), bookingId);
    }

    private TicketRow mapRow(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        OffsetDateTime issuedAt = resultSet.getObject("issued_at", OffsetDateTime.class);
        OffsetDateTime usedAt = resultSet.getObject("used_at", OffsetDateTime.class);
        return new TicketRow(resultSet.getObject("ticket_id", UUID.class), resultSet.getString("ticket_code"),
                resultSet.getString("ticket_status"), issuedAt == null ? null : issuedAt.toInstant(), usedAt == null ? null : usedAt.toInstant(),
                resultSet.getObject("booking_id", UUID.class), resultSet.getString("booking_code"), resultSet.getString("booking_status"),
                resultSet.getObject("user_id", UUID.class), resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                resultSet.getString("payment_status"), resultSet.getString("refund_status"), resultSet.getString("movie_title"), resultSet.getString("poster_url"),
                resultSet.getObject("cinema_id", UUID.class), resultSet.getString("cinema_name"), resultSet.getString("auditorium_name"),
                resultSet.getObject("start_at", OffsetDateTime.class).toInstant());
    }

    private record TicketRow(UUID ticketId, String ticketCode, String ticketStatus, Instant issuedAt, Instant usedAt,
                             UUID bookingId, String bookingCode, String bookingStatus, UUID ownerId, Instant createdAt,
                             String paymentStatus, String refundStatus, String movieTitle,
                             String posterUrl, UUID cinemaId, String cinemaName, String auditoriumName, Instant startAt) { }
}
