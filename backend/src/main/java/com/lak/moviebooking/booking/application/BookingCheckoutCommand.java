package com.lak.moviebooking.booking.application;

import java.util.UUID;

public record BookingCheckoutCommand(UUID holdId, String idempotencyKey, String voucherCode) {

    public BookingCheckoutCommand(UUID holdId, String idempotencyKey) {
        this(holdId, idempotencyKey, null);
    }

    public BookingCheckoutCommand {
        voucherCode = voucherCode == null || voucherCode.isBlank()
                ? null : voucherCode.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
