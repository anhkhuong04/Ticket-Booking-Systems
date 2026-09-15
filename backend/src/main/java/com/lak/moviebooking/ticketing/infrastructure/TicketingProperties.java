package com.lak.moviebooking.ticketing.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.ticketing")
record TicketingProperties(String qrSigningSecret, int resendRateLimit, Duration resendRateLimitWindow) {

    TicketingProperties {
        if (qrSigningSecret == null || qrSigningSecret.length() < 32 || resendRateLimit < 1
                || resendRateLimitWindow == null || resendRateLimitWindow.isNegative() || resendRateLimitWindow.isZero()) {
            throw new IllegalArgumentException("Ticketing configuration is invalid");
        }
    }
}
