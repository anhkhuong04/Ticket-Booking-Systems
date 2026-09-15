package com.lak.moviebooking.booking.application;

import java.time.Instant;
import java.util.UUID;

/** Minimal booking snapshot locked by the payment module. */
public record PaymentBooking(
        UUID id, UUID userId, String bookingCode, String status, long totalAmount, Instant hardDeadline) {
}
