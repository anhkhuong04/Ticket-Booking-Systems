package com.lak.moviebooking.common.outbox.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.outbox")
public record OutboxProperties(
		@DefaultValue("true") boolean enabled,
		@DefaultValue("50") int batchSize,
		@DefaultValue("1s") Duration pollInterval,
		@DefaultValue("8") int maxAttempts,
		@DefaultValue("5s") Duration initialBackoff,
		@DefaultValue("15m") Duration maxBackoff,
		@DefaultValue("2m") Duration processingTimeout) {

	public OutboxProperties {
		if (batchSize < 1 || maxAttempts < 1) {
			throw new IllegalArgumentException("Outbox batchSize and maxAttempts must be positive");
		}
		if (pollInterval.isNegative() || pollInterval.isZero()
				|| initialBackoff.isNegative()
				|| maxBackoff.isNegative()
				|| processingTimeout.isNegative()
				|| processingTimeout.isZero()) {
			throw new IllegalArgumentException("Outbox durations must be valid");
		}
	}
}
