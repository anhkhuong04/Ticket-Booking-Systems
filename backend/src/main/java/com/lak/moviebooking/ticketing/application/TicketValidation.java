package com.lak.moviebooking.ticketing.application;

import java.time.Instant;
import java.util.List;

public record TicketValidation(
        String result,
        String ticketCode,
        String movieTitle,
        String cinemaName,
        String auditoriumName,
        Instant startAt,
        List<String> seatLabels,
        Instant firstScannedAt) {

    public TicketValidation {
        seatLabels = List.copyOf(seatLabels);
    }
}
