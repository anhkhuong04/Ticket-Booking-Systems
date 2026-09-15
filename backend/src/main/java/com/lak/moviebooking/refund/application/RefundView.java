package com.lak.moviebooking.refund.application;

import java.time.Instant;
import java.util.UUID;

public record RefundView(
        UUID id, UUID bookingId, UUID paymentId, long amount, String status, Instant requestedAt, Instant refundedAt) {
}
