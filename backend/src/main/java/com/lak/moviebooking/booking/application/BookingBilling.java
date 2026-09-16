package com.lak.moviebooking.booking.application;

import java.util.UUID;

public interface BookingBilling {
    BookingBillingRequest find(UUID ownerId, String bookingCode);
    BookingBillingRequest request(UUID ownerId, String bookingCode, BookingBillingRequest value);
}
