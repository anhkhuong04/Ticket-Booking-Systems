package com.lak.moviebooking.refund.application;

import java.time.Instant;
import java.util.UUID;

/** Creates and retries refunds for verified payments that arrived after the booking deadline. */
public interface RefundManagement {

    RefundView requestLatePaymentRefund(UUID bookingId, UUID paymentId, long amount);

    RefundView requestShowtimeCancellationRefund(UUID bookingId);

    RefundView requestCustomerRefund(CustomerRefundCommand command);

    RefundView findCustomerRefund(UUID userId, UUID refundId);

    void processShowtimeCancellation(UUID showtimeId, Instant cancelledAt);

    int processRequestedRefunds(Instant now);
}
