package com.lak.moviebooking.notification.infrastructure;

import com.lak.moviebooking.notification.application.EmailDeliveryCommand;
import com.lak.moviebooking.notification.application.EmailDeliveryProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Generic JSON-over-HTTP email adapter. The receiving provider must honor the
 * Idempotency-Key header because an outbox event can be delivered more than once.
 */
@Component
@ConditionalOnExpression("${app.notification.email.enabled:false} && '${app.notification.email.provider:disabled}' == 'http'")
class HttpEmailDeliveryProvider implements EmailDeliveryProvider {

	private final RestClient restClient;
	private final EmailProperties properties;

	HttpEmailDeliveryProvider(RestClient.Builder restClientBuilder, EmailProperties properties) {
		properties.requireHttpConfiguration();
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	@Override
	public String providerName() {
		return "http";
	}

	@Override
	public void send(EmailDeliveryCommand command) {
		restClient.post()
				.uri(properties.endpoint())
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiToken())
				.header("Idempotency-Key", command.outboxEventId().toString())
				.body(new EmailProviderRequest(
						properties.fromAddress(),
						command.recipient(),
						command.content().subject(),
						command.content().htmlBody()))
				.retrieve()
				.toBodilessEntity();
	}

	private record EmailProviderRequest(String from, String to, String subject, String html) {
	}
}
