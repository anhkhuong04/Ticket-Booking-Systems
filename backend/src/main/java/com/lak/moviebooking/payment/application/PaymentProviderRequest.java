package com.lak.moviebooking.payment.application;

import java.time.Instant;
import java.util.UUID;

public record PaymentProviderRequest(
        UUID paymentId, String providerTransactionId, String bookingCode, long amount, String currency, Instant expiresAt) {
}
