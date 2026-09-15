package com.lak.moviebooking.booking.application;

import java.time.Instant;
import java.util.UUID;

/** Transactional cross-module contract for payment state transitions. */
public interface BookingPaymentAccess {

    PaymentBooking lockForPayment(UUID bookingId, Instant now);

    PaymentBooking lockForPayment(String bookingCode, UUID userId, Instant now);

    void markPaid(UUID bookingId, Instant now);

    void markExpired(UUID bookingId, Instant now);

    void markPaymentReview(UUID bookingId, Instant now);
}
