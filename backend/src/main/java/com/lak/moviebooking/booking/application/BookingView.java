package com.lak.moviebooking.booking.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingView(
        UUID id,
        String bookingCode,
        UUID holdId,
        UUID showtimeId,
        String movieTitle,
        String cinemaName,
        String auditoriumName,
        Instant startAt,
        String status,
        long subtotal,
        String voucherCode,
        long discountAmount,
        long serviceFee,
        long totalAmount,
        Instant paymentDeadline,
        Instant hardDeadline,
        Instant serverNow,
        List<BookingItemView> items) {

    public BookingView {
        items = List.copyOf(items);
    }
}
