package com.lak.moviebooking.showtime.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.showtime.application.ShowtimeSeatForHold;
import com.lak.moviebooking.showtime.application.ShowtimeSeatForBooking;
import com.lak.moviebooking.showtime.application.ShowtimeSeatInventory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
class JdbcShowtimeSeatInventory implements ShowtimeSeatInventory {

    private final JdbcTemplate jdbcTemplate;

    JdbcShowtimeSeatInventory(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ShowtimeSeatForHold> lockForHold(UUID showtimeId, List<UUID> requestedSeatIds, Instant now) {
        if (!isOpen(showtimeId, now)) {
            throw ApplicationException.notFound("SHOWTIME_NOT_OPEN", "Showtime was not found or is no longer open");
        }

        List<SeatRow> requested = querySeats(showtimeId, requestedSeatIds, false);
        if (requested.size() != requestedSeatIds.size()) {
            throw ApplicationException.businessRule("INVALID_SEAT_SELECTION", "One or more selected seats are invalid");
        }

        Set<String> pairKeys = requested.stream()
                .map(SeatRow::pairKey)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> idsToLock = new LinkedHashSet<>(requestedSeatIds);
        idsToLock.addAll(pairSeatIds(showtimeId, pairKeys));

        List<SeatRow> locked = querySeats(showtimeId, List.copyOf(idsToLock), true);
        if (locked.size() != idsToLock.size()) {
            throw ApplicationException.businessRule("INVALID_SEAT_SELECTION", "One or more selected seats are invalid");
        }
        return locked.stream()
                .map(seat -> new ShowtimeSeatForHold(seat.id(), seat.pairKey(), seat.status(), seat.currentHoldId()))
                .toList();
    }

    @Override
    public void markHeld(List<UUID> showtimeSeatIds, UUID holdId, Instant expiresAt, Instant now) {
        String placeholders = placeholders(showtimeSeatIds.size());
        List<Object> arguments = new ArrayList<>();
        arguments.add(holdId);
        arguments.add(atUtc(expiresAt));
        arguments.add(atUtc(now));
        arguments.addAll(showtimeSeatIds);
        int updated = jdbcTemplate.update("""
                UPDATE showtime_seats
                SET status = 'HELD', current_hold_id = ?, hold_expires_at = ?, version = version + 1, updated_at = ?
                WHERE status = 'AVAILABLE' AND id IN (%s)
                """.formatted(placeholders), arguments.toArray());
        if (updated != showtimeSeatIds.size()) {
            throw ApplicationException.conflict("SEAT_UNAVAILABLE", "One or more selected seats are no longer available");
        }
    }

    @Override
    public List<UUID> releaseHold(UUID holdId, Instant now) {
        List<UUID> seatIds = jdbcTemplate.query("""
                SELECT id FROM showtime_seats
                WHERE current_hold_id = ? AND status = 'HELD'
                ORDER BY id FOR UPDATE
                """, (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class), holdId);
        if (seatIds.isEmpty()) {
            return List.of();
        }
        List<Object> arguments = new ArrayList<>();
        arguments.add(atUtc(now));
        arguments.addAll(seatIds);
        jdbcTemplate.update("""
                UPDATE showtime_seats
                SET status = 'AVAILABLE', current_hold_id = NULL, hold_expires_at = NULL,
                    version = version + 1, updated_at = ?
                WHERE id IN (%s)
                """.formatted(placeholders(seatIds.size())), arguments.toArray());
        return seatIds;
    }

    @Override
    public List<ShowtimeSeatForBooking> lockForCheckout(
            UUID showtimeId, List<UUID> showtimeSeatIds, UUID holdId, Instant now) {
        if (showtimeSeatIds.isEmpty()) {
            throw ApplicationException.businessRule("EMPTY_SEAT_HOLD", "Seat hold does not contain any seats");
        }
        List<UUID> sortedIds = showtimeSeatIds.stream().sorted(Comparator.naturalOrder()).toList();
        List<Object> arguments = new ArrayList<>();
        arguments.add(showtimeId);
        arguments.addAll(sortedIds);
        List<CheckoutSeatRow> seats = jdbcTemplate.query("""
                SELECT ss.id, seat.row_label, seat.seat_number, seat.seat_type, ss.status,
                       ss.current_hold_id, ss.hold_expires_at, price.price
                FROM showtime_seats ss
                JOIN seats seat ON seat.id = ss.seat_id
                JOIN showtime_prices price ON price.showtime_id = ss.showtime_id AND price.seat_type = seat.seat_type
                WHERE ss.showtime_id = ? AND ss.id IN (%s)
                ORDER BY ss.id FOR UPDATE OF ss
                """.formatted(placeholders(sortedIds.size())), (resultSet, rowNumber) -> new CheckoutSeatRow(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("row_label") + resultSet.getInt("seat_number"),
                resultSet.getString("seat_type"), resultSet.getString("status"),
                resultSet.getObject("current_hold_id", UUID.class),
                resultSet.getObject("hold_expires_at", OffsetDateTime.class), resultSet.getLong("price")), arguments.toArray());
        if (seats.size() != sortedIds.size()) {
            throw ApplicationException.conflict("SEAT_HOLD_INVALID", "Seat hold no longer matches this showtime");
        }
        boolean invalid = seats.stream().anyMatch(seat -> !"HELD".equals(seat.status())
                || !holdId.equals(seat.currentHoldId())
                || seat.holdExpiresAt() == null || !seat.holdExpiresAt().toInstant().isAfter(now));
        if (invalid) {
            throw ApplicationException.expired("SEAT_HOLD_EXPIRED", "Your seat hold has expired or is no longer available");
        }
        return seats.stream().map(seat -> new ShowtimeSeatForBooking(
                seat.id(), seat.seatLabel(), seat.seatType(), seat.price())).toList();
    }

    @Override
    public void markPaymentPending(List<UUID> showtimeSeatIds, UUID holdId, UUID bookingId, Instant now) {
        String placeholders = placeholders(showtimeSeatIds.size());
        List<Object> arguments = new ArrayList<>();
        arguments.add(bookingId);
        arguments.add(atUtc(now));
        arguments.addAll(showtimeSeatIds);
        arguments.add(holdId);
        int updated = jdbcTemplate.update("""
                UPDATE showtime_seats
                SET status = 'PAYMENT_PENDING', current_hold_id = NULL, current_booking_id = ?,
                    hold_expires_at = NULL, version = version + 1, updated_at = ?
                WHERE id IN (%s) AND status = 'HELD' AND current_hold_id = ?
                """.formatted(placeholders), arguments.toArray());
        if (updated != showtimeSeatIds.size()) {
            throw ApplicationException.conflict("SEAT_HOLD_INVALID", "Seat hold is no longer available for checkout");
        }
    }

    private boolean isOpen(UUID showtimeId, Instant now) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM showtimes
                    WHERE id = ? AND status = 'SCHEDULED' AND sales_close_at > ?
                )
                """, Boolean.class, showtimeId, atUtc(now)));
    }

    private List<UUID> pairSeatIds(UUID showtimeId, Set<String> pairKeys) {
        if (pairKeys.isEmpty()) {
            return List.of();
        }
        String placeholders = placeholders(pairKeys.size());
        List<Object> arguments = new ArrayList<>();
        arguments.add(showtimeId);
        arguments.addAll(pairKeys);
        return jdbcTemplate.query("""
                SELECT ss.id
                FROM showtime_seats ss
                JOIN seats s ON s.id = ss.seat_id
                WHERE ss.showtime_id = ? AND s.pair_key IN (%s)
                ORDER BY ss.id
                """.formatted(placeholders), (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class), arguments.toArray());
    }

    private List<SeatRow> querySeats(UUID showtimeId, List<UUID> ids, boolean lock) {
        if (ids.isEmpty()) {
            throw ApplicationException.businessRule("INVALID_SEAT_SELECTION", "At least one seat is required");
        }
        List<UUID> sortedIds = ids.stream().sorted(Comparator.naturalOrder()).toList();
        List<Object> arguments = new ArrayList<>();
        arguments.add(showtimeId);
        arguments.addAll(sortedIds);
        String lockClause = lock ? " FOR UPDATE OF ss" : "";
        return jdbcTemplate.query("""
                SELECT ss.id, s.pair_key, ss.status, ss.current_hold_id
                FROM showtime_seats ss
                JOIN seats s ON s.id = ss.seat_id
                WHERE ss.showtime_id = ? AND ss.id IN (%s)
                ORDER BY ss.id%s
                """.formatted(placeholders(sortedIds.size()), lockClause), (resultSet, rowNumber) -> new SeatRow(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("pair_key"),
                resultSet.getString("status"),
                resultSet.getObject("current_hold_id", UUID.class)), arguments.toArray());
    }

    private String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }

    private OffsetDateTime atUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private record SeatRow(UUID id, String pairKey, String status, UUID currentHoldId) {
    }

    private record CheckoutSeatRow(
            UUID id, String seatLabel, String seatType, String status,
            UUID currentHoldId, OffsetDateTime holdExpiresAt, long price) {
    }
}
