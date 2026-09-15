package com.lak.moviebooking.notification.application;

import java.util.UUID;

/** Cross-module contract for appending an email request to the current transaction's outbox. */
public interface EmailNotificationQueue {

	UUID enqueue(UUID aggregateId, EmailOutboxPayload payload);
}
