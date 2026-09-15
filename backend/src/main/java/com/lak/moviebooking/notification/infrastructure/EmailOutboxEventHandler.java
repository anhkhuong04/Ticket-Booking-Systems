package com.lak.moviebooking.notification.infrastructure;

import com.lak.moviebooking.common.outbox.application.OutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventHandler;
import com.lak.moviebooking.notification.application.EmailContent;
import com.lak.moviebooking.notification.application.EmailDeliveryCommand;
import com.lak.moviebooking.notification.application.EmailDeliveryProvider;
import com.lak.moviebooking.notification.application.EmailOutboxPayload;
import com.lak.moviebooking.notification.application.EmailTemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "app.notification.email", name = "enabled", havingValue = "true")
class EmailOutboxEventHandler implements OutboxEventHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmailOutboxEventHandler.class);
	private final EmailDeliveryProvider provider;
	private final EmailTemplateRenderer templateRenderer;
	private final ObjectMapper objectMapper;
	private final EmailDeliveryMetrics metrics;

	EmailOutboxEventHandler(
			EmailDeliveryProvider provider,
			EmailTemplateRenderer templateRenderer,
			ObjectMapper objectMapper,
			EmailDeliveryMetrics metrics) {
		this.provider = provider;
		this.templateRenderer = templateRenderer;
		this.objectMapper = objectMapper;
		this.metrics = metrics;
	}

	@Override
	public boolean supports(String eventType) {
		return TransactionalEmailNotificationQueue.EMAIL_REQUESTED_EVENT_TYPE.equals(eventType);
	}

	@Override
	public void handle(OutboxEvent event) {
		EmailOutboxPayload payload = deserialize(event.payload());
		EmailContent content = templateRenderer.render(payload);

		try {
			provider.send(new EmailDeliveryCommand(event.id(), payload.recipient(), content));
			metrics.record(provider.providerName(), "sent");
			LOGGER.info("Email delivery succeeded outbox_event_id={} provider={}", event.id(), provider.providerName());
		}
		catch (RuntimeException exception) {
			metrics.record(provider.providerName(), "failed");
			LOGGER.warn("Email delivery failed outbox_event_id={} provider={} exception_type={}",
					event.id(), provider.providerName(), exception.getClass().getSimpleName());
			throw exception;
		}
	}

	private EmailOutboxPayload deserialize(String payload) {
		try {
			return objectMapper.readValue(payload, EmailOutboxPayload.class);
		}
		catch (JacksonException | IllegalArgumentException exception) {
			throw new IllegalArgumentException("Invalid email outbox event", exception);
		}
	}
}
