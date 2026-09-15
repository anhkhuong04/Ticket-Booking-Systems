package com.lak.moviebooking.reservation.infrastructure;

import java.util.List;
import java.util.UUID;

record SeatAvailabilityEvent(String type, UUID showtimeId, UUID resourceId, List<UUID> showtimeSeatIds) {

    SeatAvailabilityEvent {
        showtimeSeatIds = List.copyOf(showtimeSeatIds);
    }
}
