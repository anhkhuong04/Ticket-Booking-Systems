package com.lak.moviebooking.ticketing.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.ticketing.application.TicketIssuance;
import com.lak.moviebooking.ticketing.application.TicketIssuer;
import com.lak.moviebooking.ticketing.application.TicketEmailDelivery;
import com.lak.moviebooking.ticketing.application.TicketQrPayloadFactory;
import com.lak.moviebooking.ticketing.application.TicketView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JdbcTicketIssuer implements TicketIssuer {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final TicketQrPayloadFactory qrPayloadFactory;
    private final TicketEmailDelivery ticketEmailDelivery;

    JdbcTicketIssuer(JdbcTemplate jdbcTemplate, Clock clock, TicketQrPayloadFactory qrPayloadFactory,
            TicketEmailDelivery ticketEmailDelivery) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.qrPayloadFactory = qrPayloadFactory;
        this.ticketEmailDelivery = ticketEmailDelivery;
    }

    @Override
    @Transactional
    public TicketIssuance issueForPaidBooking(UUID bookingId) {
        String status = jdbcTemplate.query("SELECT status FROM bookings WHERE id=? FOR UPDATE",
                (resultSet, rowNumber) -> resultSet.getString("status"), bookingId).stream().findFirst()
                .orElseThrow(() -> ApplicationException.notFound("BOOKING_NOT_FOUND", "Booking was not found"));
        if (!"PAID".equals(status)) {
            throw ApplicationException.businessRule("BOOKING_PAYMENT_NOT_VERIFIED", "Ticket can only be issued after verified payment");
        }
        TicketView existing = findByBookingId(bookingId);
        if (existing != null) {
            return new TicketIssuance(existing, Optional.empty());
        }

        Instant now = clock.instant();
        TicketView ticket = new TicketView(UUID.randomUUID(), bookingId, ticketCode(), "VALID", now, null);
        String rawQrToken = qrPayloadFactory.create(ticket.ticketCode());
        jdbcTemplate.update("""
                INSERT INTO tickets (id,booking_id,ticket_code,qr_token_hash,status,issued_at,used_at,created_at,updated_at)
                VALUES (?,?,?,?,?,?,NULL,?,?)
                """, ticket.id(), ticket.bookingId(), ticket.ticketCode(), sha256(rawQrToken), ticket.status(),
                atUtc(ticket.issuedAt()), atUtc(now), atUtc(now));
        ticketEmailDelivery.enqueueIssuedTicket(ticket.id());
        return new TicketIssuance(ticket, Optional.of(rawQrToken));
    }

    private TicketView findByBookingId(UUID bookingId) {
        return jdbcTemplate.query("""
                SELECT id,booking_id,ticket_code,status,issued_at,used_at
                FROM tickets WHERE booking_id=?
                """, (resultSet, rowNumber) -> new TicketView(
                resultSet.getObject("id", UUID.class), resultSet.getObject("booking_id", UUID.class),
                resultSet.getString("ticket_code"), resultSet.getString("status"),
                instant(resultSet, "issued_at"), nullableInstant(resultSet, "used_at")), bookingId)
                .stream().findFirst().orElse(null);
    }

    private String ticketCode() {
        return "TKT-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(java.util.Locale.ROOT);
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

    private Instant instant(java.sql.ResultSet resultSet, String column) throws java.sql.SQLException {
        return resultSet.getObject(column, OffsetDateTime.class).toInstant();
    }

    private Instant nullableInstant(java.sql.ResultSet resultSet, String column) throws java.sql.SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private OffsetDateTime atUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
