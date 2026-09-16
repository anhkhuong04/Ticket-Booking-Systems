package com.lak.moviebooking.reservation.application;

import java.util.List;
import java.util.UUID;

/** Minimal, non-personal realtime event payload shared across booking workflows. */
public record SeatAvailabilityEvent(String type, UUID showtimeId, UUID resourceId, List<UUID> showtimeSeatIds) {

    public SeatAvailabilityEvent {
        showtimeSeatIds = List.copyOf(showtimeSeatIds);
    }
}
