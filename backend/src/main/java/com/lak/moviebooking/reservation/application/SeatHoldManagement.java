package com.lak.moviebooking.reservation.application;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

public interface SeatHoldManagement {

    SeatHoldView create(UUID userId, SeatHoldCommand command);

    SeatHoldView find(UUID userId, UUID holdId);

    void release(UUID userId, UUID holdId);

    void consumeForCheckout(UUID userId, UUID holdId);

    int expireDueHolds(Instant now);

    List<UUID> cancelActiveForShowtime(UUID showtimeId, Instant now);
}
