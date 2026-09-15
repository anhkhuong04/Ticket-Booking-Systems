package com.lak.moviebooking.booking.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.booking")
public record BookingProperties(Duration checkoutIdempotencyTtl, Duration paymentGracePeriod) {

    public BookingProperties {
        if (checkoutIdempotencyTtl == null || checkoutIdempotencyTtl.isZero() || checkoutIdempotencyTtl.isNegative()
                || paymentGracePeriod == null || paymentGracePeriod.isZero() || paymentGracePeriod.isNegative()) {
            throw new IllegalArgumentException("Booking durations must be positive");
        }
    }
}
