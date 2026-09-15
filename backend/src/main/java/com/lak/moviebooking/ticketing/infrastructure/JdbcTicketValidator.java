package com.lak.moviebooking.ticketing.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.authorization.application.CinemaScopeAuthorizer;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import com.lak.moviebooking.ticketing.application.TicketValidation;
import com.lak.moviebooking.ticketing.application.TicketValidator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JdbcTicketValidator implements TicketValidator {

    private final JdbcTemplate jdbcTemplate;
    private final CinemaScopeAuthorizer cinemaScopeAuthorizer;
    private final Clock clock;

    JdbcTicketValidator(JdbcTemplate jdbcTemplate, CinemaScopeAuthorizer cinemaScopeAuthorizer, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.cinemaScopeAuthorizer = cinemaScopeAuthorizer;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TicketValidation validate(AuthenticatedPrincipal scanner, String scannedValue) {
        String value = scannedValue == null ? "" : scannedValue.trim();
        if (value.isBlank() || value.length() > 256) {
            throw ApplicationException.businessRule("TICKET_CODE_INVALID", "Ticket code is invalid");
        }
        TicketRow ticket = ticket(value);
        cinemaScopeAuthorizer.requireAccess(scanner, ticket.cinemaId());
        if ("USED".equals(ticket.ticketStatus())) return previousScan(ticket);
        if (!"VALID".equals(ticket.ticketStatus())) {
            throw ApplicationException.businessRule("TICKET_NOT_VALID", "Ticket is not valid");
        }

        Instant now = clock.instant();
        if (!"SCHEDULED".equals(ticket.showtimeStatus()) || now.isBefore(ticket.startAt()) || !now.isBefore(ticket.endAt())) {
            throw ApplicationException.businessRule("TICKET_SHOWTIME_UNAVAILABLE", "Ticket cannot be used at this time");
        }
        int updated = jdbcTemplate.update("""
                UPDATE tickets SET status='USED',used_at=?,updated_at=?
                WHERE id=? AND status='VALID'
                """, atUtc(now), atUtc(now), ticket.ticketId());
        if (updated != 1) return previousScan(ticket(value));
        jdbcTemplate.update("""
                INSERT INTO ticket_scan_logs (id,ticket_id,scanner_user_id,cinema_id,result,scanned_at)
                VALUES (?,?,?,?, 'VALID',?)
                """, UUID.randomUUID(), ticket.ticketId(), scanner.userId(), ticket.cinemaId(), atUtc(now));
        return view("VALID", ticket, now);
    }

    private TicketValidation previousScan(TicketRow ticket) {
        Instant firstScannedAt = jdbcTemplate.query("""
                SELECT scanned_at FROM ticket_scan_logs WHERE ticket_id=? AND result='VALID'
                """, (resultSet, rowNumber) -> resultSet.getObject("scanned_at", OffsetDateTime.class).toInstant(), ticket.ticketId())
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Used ticket has no successful scan log"));
        return view("USED", ticket, firstScannedAt);
    }

    private TicketValidation view(String result, TicketRow ticket, Instant firstScannedAt) {
        return new TicketValidation(result, ticket.ticketCode(), ticket.movieTitle(), ticket.cinemaName(), ticket.auditoriumName(),
                ticket.startAt(), seats(ticket.bookingId()), firstScannedAt);
    }

    private TicketRow ticket(String scannedValue) {
        return jdbcTemplate.query("""
                SELECT ticket.id AS ticket_id,ticket.status AS ticket_status,ticket.ticket_code,
                       booking.id AS booking_id,cinema.id AS cinema_id,cinema.name AS cinema_name,
                       movie.title AS movie_title,auditorium.name AS auditorium_name,
                       showtime.start_at,showtime.end_at,showtime.status AS showtime_status
                FROM tickets ticket
                JOIN bookings booking ON booking.id=ticket.booking_id
                JOIN showtimes showtime ON showtime.id=booking.showtime_id
                JOIN movies movie ON movie.id=showtime.movie_id
                JOIN auditoriums auditorium ON auditorium.id=showtime.auditorium_id
                JOIN cinemas cinema ON cinema.id=auditorium.cinema_id
                WHERE ticket.ticket_code=? OR ticket.qr_token_hash=?
                FOR UPDATE OF ticket
                """, (resultSet, rowNumber) -> new TicketRow(
                resultSet.getObject("ticket_id", UUID.class),
                resultSet.getString("ticket_status"),
                resultSet.getString("ticket_code"),
                resultSet.getObject("booking_id", UUID.class),
                resultSet.getObject("cinema_id", UUID.class),
                resultSet.getString("cinema_name"),
                resultSet.getString("movie_title"),
                resultSet.getString("auditorium_name"),
                resultSet.getObject("start_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("end_at", OffsetDateTime.class).toInstant(),
                resultSet.getString("showtime_status")), scannedValue, sha256(scannedValue))
                .stream().findFirst()
                .orElseThrow(() -> ApplicationException.notFound("TICKET_NOT_FOUND", "Ticket was not found"));
    }

    private List<String> seats(UUID bookingId) {
        return jdbcTemplate.query("""
                SELECT seat_label_snapshot FROM booking_items
                WHERE booking_id=? ORDER BY seat_label_snapshot,showtime_seat_id
                """, (resultSet, rowNumber) -> resultSet.getString("seat_label_snapshot"), bookingId);
    }

    private String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private OffsetDateTime atUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private record TicketRow(
            UUID ticketId, String ticketStatus, String ticketCode, UUID bookingId, UUID cinemaId, String cinemaName,
            String movieTitle, String auditoriumName, Instant startAt, Instant endAt, String showtimeStatus) {
    }
}
