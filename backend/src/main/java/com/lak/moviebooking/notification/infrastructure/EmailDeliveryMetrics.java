package com.lak.moviebooking.notification.infrastructure;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
class EmailDeliveryMetrics {

	private final MeterRegistry meterRegistry;

	EmailDeliveryMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	void record(String provider, String outcome) {
		Counter.builder("lak.notification.email.deliveries")
				.description("Email delivery attempts from the transactional outbox")
				.tag("provider", provider)
				.tag("outcome", outcome)
				.register(meterRegistry)
				.increment();
	}
}
