package com.lak.moviebooking.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.lak.moviebooking.common.outbox.application.NewOutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventWriter;
import com.lak.moviebooking.notification.application.EmailOutboxPayload;
import com.lak.moviebooking.notification.application.EmailTemplate;
import org.junit.jupiter.api.Test;

class TransactionalEmailNotificationQueueTests {

	@Test
	void appendsTheNotificationRequestToTheCallerTransactionOutbox() {
		AtomicReference<NewOutboxEvent> appended = new AtomicReference<>();
		UUID eventId = UUID.randomUUID();
		OutboxEventWriter writer = event -> {
			appended.set(event);
			return eventId;
		};
		TransactionalEmailNotificationQueue queue = new TransactionalEmailNotificationQueue(writer);
		UUID aggregateId = UUID.randomUUID();
		EmailOutboxPayload payload = new EmailOutboxPayload(
				"alice@example.com",
				EmailTemplate.BOOKING_CONFIRMATION,
				Map.of("bookingCode", "LAK-20260915-001"));

		assertThat(queue.enqueue(aggregateId, payload)).isEqualTo(eventId);
		assertThat(appended.get().eventType())
				.isEqualTo(TransactionalEmailNotificationQueue.EMAIL_REQUESTED_EVENT_TYPE);
		assertThat(appended.get().aggregateId()).isEqualTo(aggregateId);
		assertThat(appended.get().payload()).isEqualTo(payload);
	}
}
