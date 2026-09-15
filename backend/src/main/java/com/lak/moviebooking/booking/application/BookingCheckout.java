package com.lak.moviebooking.booking.application;

import java.util.UUID;

public interface BookingCheckout {

    BookingView checkout(UUID userId, BookingCheckoutCommand command);

    BookingView findByCode(UUID userId, String bookingCode);
}
