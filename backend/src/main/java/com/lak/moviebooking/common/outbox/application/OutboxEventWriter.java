package com.lak.moviebooking.common.outbox.application;

import java.util.UUID;

public interface OutboxEventWriter {

	/**
	 * Adds an event to the caller's existing business transaction.
	 *
	 * @throws org.springframework.transaction.IllegalTransactionStateException if
	 * no transaction is active
	 */
	UUID append(NewOutboxEvent event);
}
