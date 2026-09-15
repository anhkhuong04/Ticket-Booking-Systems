package com.lak.moviebooking.reservation.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SeatHoldView(
        UUID id,
        UUID showtimeId,
        String status,
        Instant serverNow,
        Instant expiresAt,
        Instant hardExpiresAt,
        List<UUID> showtimeSeatIds) {

    public SeatHoldView {
        showtimeSeatIds = List.copyOf(showtimeSeatIds);
    }
}
