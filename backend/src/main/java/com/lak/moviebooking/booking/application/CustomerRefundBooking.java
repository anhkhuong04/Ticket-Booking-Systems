package com.lak.moviebooking.booking.application;

import java.time.Instant;
import java.util.UUID;

/** Minimum immutable facts needed to decide a customer-initiated full refund. */
public record CustomerRefundBooking(
        UUID id, UUID userId, String status, long totalAmount, Instant showtimeStartAt) {
}
