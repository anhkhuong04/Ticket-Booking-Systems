package com.lak.moviebooking.showtime.application;

import java.util.UUID;

/** Immutable server-side seat and price snapshot used while creating a booking. */
public record ShowtimeSeatForBooking(
        UUID id,
        String seatLabel,
        String seatType,
        long unitPrice) {
}
