package com.lak.moviebooking.showtime.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Transactional boundary used by reservation to lock and change showtime-seat snapshots. */
public interface ShowtimeSeatInventory {

    List<ShowtimeSeatForHold> lockForHold(UUID showtimeId, List<UUID> requestedSeatIds, Instant now);

    void markHeld(List<UUID> showtimeSeatIds, UUID holdId, Instant expiresAt, Instant now);

    List<UUID> releaseHold(UUID holdId, Instant now);

    List<ShowtimeSeatForBooking> lockForCheckout(
            UUID showtimeId, List<UUID> showtimeSeatIds, UUID holdId, Instant now);

    void markPaymentPending(List<UUID> showtimeSeatIds, UUID holdId, UUID bookingId, Instant now);

    List<UUID> markSoldForBooking(UUID bookingId, Instant now);

    List<UUID> releaseBooking(UUID bookingId, Instant now);

    List<UUID> releaseSoldBookingForRefund(UUID bookingId, Instant now);
}
