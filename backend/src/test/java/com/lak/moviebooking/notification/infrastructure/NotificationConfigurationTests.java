package com.lak.moviebooking.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.lak.moviebooking.notification.application.EmailDeliveryProvider;
import com.lak.moviebooking.notification.application.EmailTemplateRenderer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class NotificationConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(
					NotificationConfiguration.class,
					HttpEmailDeliveryProvider.class,
					EmailOutboxEventHandler.class,
					EmailDeliveryMetrics.class)
			.withBean(RestClient.Builder.class, RestClient::builder)
			.withBean(EmailTemplateRenderer.class)
			.withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
			.withBean(MeterRegistry.class, SimpleMeterRegistry::new);

	@Test
	void enablesHttpProviderAndOutboxHandlerOnlyWhenEmailIsEnabled() {
		contextRunner.withPropertyValues(
					"app.notification.email.enabled=true",
					"app.notification.email.provider=http",
					"app.notification.email.endpoint=https://email-provider.example/api/send",
					"app.notification.email.api-token=test-token",
					"app.notification.email.from-address=no-reply@example.com")
				.run(context -> {
					assertThat(context).hasSingleBean(EmailDeliveryProvider.class);
					assertThat(context).hasSingleBean(EmailOutboxEventHandler.class);
				});
	}

	@Test
	void leavesProviderAndOutboxHandlerAbsentWhenEmailIsDisabled() {
		contextRunner.withPropertyValues(
					"app.notification.email.enabled=false",
					"app.notification.email.provider=http")
				.run(context -> {
					assertThat(context).doesNotHaveBean(EmailDeliveryProvider.class);
					assertThat(context).doesNotHaveBean(EmailOutboxEventHandler.class);
				});
	}
}
