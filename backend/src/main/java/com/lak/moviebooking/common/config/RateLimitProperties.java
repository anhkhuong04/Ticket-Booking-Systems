package com.lak.moviebooking.common.config;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @Min(1) int holdLimit,
        Duration holdWindow,
        @Min(1) int checkoutLimit,
        Duration checkoutWindow,
        @Min(1) int webhookLimit,
        Duration webhookWindow,
        @Min(1) int scanLimit,
        Duration scanWindow) {

	public RateLimitProperties {
		if (!positive(holdWindow) || !positive(checkoutWindow) || !positive(webhookWindow) || !positive(scanWindow)) {
			throw new IllegalArgumentException("Rate limit windows must be positive");
		}
	}

	private static boolean positive(Duration value) {
		return value != null && !value.isZero() && !value.isNegative();
	}
}
