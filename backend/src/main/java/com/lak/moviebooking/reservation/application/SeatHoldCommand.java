package com.lak.moviebooking.reservation.application;

import java.util.List;
import java.util.UUID;

public record SeatHoldCommand(UUID showtimeId, List<UUID> showtimeSeatIds, String idempotencyKey) {

    public SeatHoldCommand {
        showtimeSeatIds = List.copyOf(showtimeSeatIds);
    }
}
