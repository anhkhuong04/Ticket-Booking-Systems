package com.lak.moviebooking.booking.application;

import java.util.UUID;

/** Locked booking snapshot used by customer refund workflows. */
public interface BookingRefundAccess {

    CustomerRefundBooking lockForCustomerRefund(UUID bookingId);
}
