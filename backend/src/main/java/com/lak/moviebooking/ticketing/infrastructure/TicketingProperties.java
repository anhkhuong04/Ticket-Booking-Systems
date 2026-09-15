package com.lak.moviebooking.ticketing.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.ticketing")
record TicketingProperties(String qrSigningSecret) {

    TicketingProperties {
        if (qrSigningSecret == null || qrSigningSecret.length() < 32) {
            throw new IllegalArgumentException("Ticket QR signing secret must be at least 32 characters");
        }
    }
}
