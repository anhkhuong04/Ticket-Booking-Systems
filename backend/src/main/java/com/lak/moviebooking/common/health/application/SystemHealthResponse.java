package com.lak.moviebooking.common.health.application;

import java.time.Instant;
import java.util.Map;

public record SystemHealthResponse(
		String status,
		Instant timestamp,
		Map<String, ServiceHealth> services) {

	public boolean matchesUpStatus() {
		return "UP".equals(status);
	}
}
