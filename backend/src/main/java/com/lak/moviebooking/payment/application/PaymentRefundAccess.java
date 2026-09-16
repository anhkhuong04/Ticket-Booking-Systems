package com.lak.moviebooking.payment.application;

import java.util.UUID;

/** Transactional boundary for refund processing; implementations lock the payment row. */
public interface PaymentRefundAccess {

    RefundablePayment lockForRefund(UUID paymentId);

    RefundablePayment lockSuccessfulForBooking(UUID bookingId);
}
