package com.lak.moviebooking.refund.application;

import java.util.UUID;

public record RefundProviderRequest(
        UUID refundId, UUID paymentId, String paymentTransactionId, long amount, String currency) {
}
