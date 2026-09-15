package com.lak.moviebooking.payment.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.payment")
public record PaymentProperties(Duration reconciliationPollInterval, int reconciliationBatchSize, Sandbox sandbox) {

    public PaymentProperties {
        if (reconciliationPollInterval == null || reconciliationPollInterval.isNegative() || reconciliationPollInterval.isZero()
                || reconciliationBatchSize < 1 || sandbox == null) {
            throw new IllegalArgumentException("Payment configuration is invalid");
        }
    }

    public record Sandbox(String checkoutBaseUrl, String webhookSecret) {
        public Sandbox {
            if (checkoutBaseUrl == null || checkoutBaseUrl.isBlank() || webhookSecret == null || webhookSecret.length() < 32) {
                throw new IllegalArgumentException("Sandbox payment configuration is invalid");
            }
        }
    }
}
