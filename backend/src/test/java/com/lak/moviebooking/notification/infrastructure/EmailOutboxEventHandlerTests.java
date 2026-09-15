package com.lak.moviebooking.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.lak.moviebooking.common.outbox.application.OutboxEvent;
import com.lak.moviebooking.notification.application.EmailContent;
import com.lak.moviebooking.notification.application.EmailDeliveryCommand;
import com.lak.moviebooking.notification.application.EmailDeliveryProvider;
import com.lak.moviebooking.notification.application.EmailOutboxPayload;
import com.lak.moviebooking.notification.application.EmailTemplate;
import com.lak.moviebooking.notification.application.EmailTemplateRenderer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class EmailOutboxEventHandlerTests {

	private final ObjectMapper objectMapper = JsonMapper.builder().build();

	@Test
	void rendersTemplateAndPassesOutboxIdAsTheProviderIdempotencyKey() throws Exception {
		AtomicReference<EmailDeliveryCommand> delivered = new AtomicReference<>();
		EmailDeliveryProvider provider = new EmailDeliveryProvider() {
			@Override
			public String providerName() {
				return "fake";
			}

			@Override
			public void send(EmailDeliveryCommand command) {
				delivered.set(command);
			}
		};
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		EmailOutboxEventHandler handler = handler(provider, registry);
		UUID eventId = UUID.randomUUID();

		handler.handle(event(eventId, payload("alice@example.com", "<script>movie</script>")));

		assertThat(delivered.get().outboxEventId()).isEqualTo(eventId);
		assertThat(delivered.get().recipient()).isEqualTo("alice@example.com");
		assertThat(delivered.get().content().subject()).isEqualTo("Xác nhận đặt vé LAK-20260915-001");
		assertThat(delivered.get().content().htmlBody()).contains("&lt;script&gt;movie&lt;/script&gt;");
		assertThat(deliveryCount(registry, "fake", "sent")).isOne();
	}

	@Test
	void doesNotLogRecipientTokenOrEmailBodyWhenProviderFails() throws Exception {
		String recipient = "alice@example.com";
		String token = "provider-token-should-not-appear";
		String sensitiveBody = "sensitive-ticket-content";
		EmailDeliveryProvider provider = new EmailDeliveryProvider() {
			@Override
			public String providerName() {
				return "fake";
			}

			@Override
			public void send(EmailDeliveryCommand command) {
				throw new IllegalStateException("recipient=" + recipient + " token=" + token + " body=" + sensitiveBody);
			}
		};
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		EmailOutboxEventHandler handler = handler(provider, registry);
		Logger logger = (Logger) LoggerFactory.getLogger(EmailOutboxEventHandler.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);

		try {
			assertThatThrownBy(() -> handler.handle(event(UUID.randomUUID(), payload(recipient, sensitiveBody))))
					.isInstanceOf(IllegalStateException.class);
		}
		finally {
			logger.detachAppender(appender);
			appender.stop();
		}

		assertThat(appender.list)
				.extracting(ILoggingEvent::getFormattedMessage)
				.noneMatch(message -> message.contains(recipient)
						|| message.contains(token)
						|| message.contains(sensitiveBody));
		assertThat(deliveryCount(registry, "fake", "failed")).isOne();
	}

	private EmailOutboxEventHandler handler(EmailDeliveryProvider provider, SimpleMeterRegistry registry) {
		return new EmailOutboxEventHandler(
				provider,
				new EmailTemplateRenderer(),
				objectMapper,
				new EmailDeliveryMetrics(registry));
	}

	private OutboxEvent event(UUID eventId, EmailOutboxPayload payload) throws Exception {
		return new OutboxEvent(
				eventId,
				TransactionalEmailNotificationQueue.EMAIL_REQUESTED_EVENT_TYPE,
				"notification",
				UUID.randomUUID(),
				objectMapper.writeValueAsString(payload),
				0,
				Instant.now());
	}

	private EmailOutboxPayload payload(String recipient, String movieTitle) {
		return new EmailOutboxPayload(
				recipient,
				EmailTemplate.BOOKING_CONFIRMATION,
				Map.of(
						"bookingCode", "LAK-20260915-001",
						"movieTitle", movieTitle,
						"showtime", "2026-09-15 19:00",
						"seats", "A1, A2"));
	}

	private double deliveryCount(SimpleMeterRegistry registry, String provider, String outcome) {
		Counter counter = registry.find("lak.notification.email.deliveries")
				.tags("provider", provider, "outcome", outcome)
				.counter();
		return counter == null ? 0 : counter.count();
	}
}
