package com.lak.moviebooking.notification.application;

import java.util.Objects;
import java.util.UUID;

/** The outbox event ID is the provider idempotency key. */
public record EmailDeliveryCommand(UUID outboxEventId, String recipient, EmailContent content) {

	public EmailDeliveryCommand {
		Objects.requireNonNull(outboxEventId, "outboxEventId is required");
		Objects.requireNonNull(recipient, "recipient is required");
		Objects.requireNonNull(content, "content is required");
	}
}
