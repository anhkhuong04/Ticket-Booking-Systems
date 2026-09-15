package com.lak.moviebooking.payment.application;

import java.util.UUID;

/** Locked payment snapshot made available to the refund module. */
public record RefundablePayment(
        UUID id, UUID bookingId, String provider, String providerTransactionId, long amount, String currency, String status) {
}
