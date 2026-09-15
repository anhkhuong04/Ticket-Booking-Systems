package com.lak.moviebooking.booking.application;

import java.util.UUID;

public record BookingItemView(
        UUID showtimeSeatId,
        String seatLabel,
        String seatType,
        long unitPrice) {
}
