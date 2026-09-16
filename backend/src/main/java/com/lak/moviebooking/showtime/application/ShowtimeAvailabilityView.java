package com.lak.moviebooking.showtime.application;

import java.util.List;
import java.util.UUID;

public record ShowtimeAvailabilityView(UUID movieId, List<ShowtimeCinemaAvailability> cinemas) {
    public ShowtimeAvailabilityView {
        cinemas = List.copyOf(cinemas);
    }
}
