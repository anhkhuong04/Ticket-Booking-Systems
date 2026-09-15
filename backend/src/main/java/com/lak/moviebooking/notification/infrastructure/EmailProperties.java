package com.lak.moviebooking.notification.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.notification.email")
public record EmailProperties(
		@DefaultValue("false") boolean enabled,
		@DefaultValue("disabled") String provider,
		@DefaultValue("") String endpoint,
		@DefaultValue("") String apiToken,
		@DefaultValue("") String fromAddress) {

	void requireHttpConfiguration() {
		if (!enabled || !"http".equals(provider)
				|| endpoint.isBlank() || apiToken.isBlank() || fromAddress.isBlank()) {
			throw new IllegalStateException("Email provider configuration is incomplete");
		}
	}
}
