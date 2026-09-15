package com.lak.moviebooking.common.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class DependencyHealthMetrics {

	private static final String METER_NAME = "lak.dependency.health.checks";
	private final MeterRegistry meterRegistry;

	public DependencyHealthMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void record(String dependency, boolean healthy) {
		Counter.builder(METER_NAME)
				.description("Number of dependency health checks")
				.tag("dependency", dependency)
				.tag("status", healthy ? "up" : "down")
				.register(meterRegistry)
				.increment();
	}
}
