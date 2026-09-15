package com.lak.moviebooking.payment.application;

import java.time.Instant;
import java.util.UUID;

public record PaymentView(
        UUID id, String bookingCode, String bookingStatus, String provider, long amount, String currency, String status,
        String paymentUrl, Instant expiresAt, Instant paidAt, Instant serverNow) {
}
