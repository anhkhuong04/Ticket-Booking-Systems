package com.lak.moviebooking.showtime.application;

import java.time.Instant;
import java.util.UUID;

public record AdminShowtimeView(
        UUID id,
        UUID cinemaId,
        String cinemaName,
        UUID auditoriumId,
        String auditoriumName,
        UUID movieId,
        String movieTitle,
        Instant startAt,
        Instant endAt,
        Instant salesCloseAt,
        String status,
        boolean cancellationBlocked) {
}
