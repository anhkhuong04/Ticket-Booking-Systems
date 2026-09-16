package com.lak.moviebooking.showtime.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ShowtimeCinemaAvailability(
        UUID cinemaId,
        String cinemaName,
        String cinemaAddress,
        List<LocalDate> dates
) {
    public ShowtimeCinemaAvailability {
        dates = List.copyOf(dates);
    }
}
