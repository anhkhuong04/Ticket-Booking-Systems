package com.lak.moviebooking.notification.infrastructure;

import java.util.UUID;

import com.lak.moviebooking.common.outbox.application.NewOutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventWriter;
import com.lak.moviebooking.notification.application.EmailNotificationQueue;
import com.lak.moviebooking.notification.application.EmailOutboxPayload;
import org.springframework.stereotype.Component;

@Component
class TransactionalEmailNotificationQueue implements EmailNotificationQueue {

	static final String EMAIL_REQUESTED_EVENT_TYPE = "notification.email.requested";
	private final OutboxEventWriter outboxEventWriter;

	TransactionalEmailNotificationQueue(OutboxEventWriter outboxEventWriter) {
		this.outboxEventWriter = outboxEventWriter;
	}

	@Override
	public UUID enqueue(UUID aggregateId, EmailOutboxPayload payload) {
		return outboxEventWriter.append(new NewOutboxEvent(
				EMAIL_REQUESTED_EVENT_TYPE,
				"notification",
				aggregateId,
				payload));
	}
}
