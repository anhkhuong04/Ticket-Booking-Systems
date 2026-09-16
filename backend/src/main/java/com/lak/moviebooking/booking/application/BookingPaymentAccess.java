package com.lak.moviebooking.booking.application;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

/** Transactional cross-module contract for payment state transitions. */
public interface BookingPaymentAccess {

    PaymentBooking lockForPayment(UUID bookingId, Instant now);

    PaymentBooking lockForPayment(String bookingCode, UUID userId, Instant now);

    void markPaid(UUID bookingId, Instant now);

    void markExpired(UUID bookingId, Instant now);

    void markPaymentReview(UUID bookingId, Instant now);

    void markRefundPending(UUID bookingId, Instant now);

    void markRefunded(UUID bookingId, Instant now);

    List<UUID> findBookingIdsForShowtimeCancellation(UUID showtimeId);

    PaymentBooking findForShowtimeCancellation(UUID bookingId);

    boolean expirePendingForShowtimeCancellation(UUID bookingId, Instant now);
}
