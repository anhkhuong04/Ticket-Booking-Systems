package com.lak.moviebooking.common.outbox.application;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
		UUID id,
		String eventType,
		String aggregateType,
		UUID aggregateId,
		String payload,
		int retryCount,
		Instant occurredAt) {
}
