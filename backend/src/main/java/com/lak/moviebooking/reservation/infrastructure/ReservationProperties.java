package com.lak.moviebooking.reservation.infrastructure;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.reservation")
public record ReservationProperties(
        Duration holdTtl,
        Duration idempotencyTtl,
        Duration expiryPollInterval,
        @Min(1) int expiryBatchSize) {

    public ReservationProperties {
        if (holdTtl == null || holdTtl.isNegative() || holdTtl.isZero()
                || idempotencyTtl == null || idempotencyTtl.isNegative() || idempotencyTtl.isZero()
                || expiryPollInterval == null || expiryPollInterval.isNegative() || expiryPollInterval.isZero()) {
            throw new IllegalArgumentException("Reservation durations must be positive");
        }
    }
}
