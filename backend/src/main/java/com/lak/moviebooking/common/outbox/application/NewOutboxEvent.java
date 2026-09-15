package com.lak.moviebooking.common.outbox.application;

import java.util.Objects;
import java.util.UUID;

public record NewOutboxEvent(
		String eventType,
		String aggregateType,
		UUID aggregateId,
		Object payload) {

	public NewOutboxEvent {
		eventType = requireText(eventType, "eventType");
		aggregateType = requireText(aggregateType, "aggregateType");
		Objects.requireNonNull(aggregateId, "aggregateId is required");
		Objects.requireNonNull(payload, "payload is required");
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		return value;
	}
}
