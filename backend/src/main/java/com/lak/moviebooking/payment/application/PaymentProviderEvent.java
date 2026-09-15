package com.lak.moviebooking.payment.application;

import java.time.Instant;
import java.util.UUID;

public record PaymentProviderEvent(
        UUID paymentId, String providerEventId, String providerTransactionId,
        long amount, String currency, String status, Instant paidAt) {
}
