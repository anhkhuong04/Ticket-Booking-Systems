package com.lak.moviebooking.common.outbox.application;

/**
 * Publishes one outbox event to an external channel. Implementations must be
 * idempotent by {@link OutboxEvent#id()} because delivery is at least once.
 */
public interface OutboxEventHandler {

	boolean supports(String eventType);

	void handle(OutboxEvent event);
}
