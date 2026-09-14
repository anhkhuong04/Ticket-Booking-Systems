package com.lak.moviebooking.common.health.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import com.lak.moviebooking.common.health.application.ServiceHealth;
import com.lak.moviebooking.common.health.application.SystemHealthQuery;
import com.lak.moviebooking.common.health.application.SystemHealthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SystemHealthControllerTests {

	private static final Instant CHECKED_AT = Instant.parse("2026-01-01T00:00:00Z");

	@Test
	void returnsOkWhenEveryServiceIsUp() {
		SystemHealthResponse response = response("UP", ServiceHealth.up());
		SystemHealthQuery query = () -> response;

		ResponseEntity<SystemHealthResponse> result = new SystemHealthController(query).health();

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo(response);
	}

	@Test
	void returnsServiceUnavailableWhenADependencyIsDown() {
		SystemHealthResponse response = response("DOWN", ServiceHealth.down());
		SystemHealthQuery query = () -> response;

		ResponseEntity<SystemHealthResponse> result = new SystemHealthController(query).health();

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(result.getBody()).isEqualTo(response);
	}

	private SystemHealthResponse response(String status, ServiceHealth redisHealth) {
		return new SystemHealthResponse(
				status,
				CHECKED_AT,
				Map.of(
						"backend", ServiceHealth.up(),
						"database", ServiceHealth.up(),
						"redis", redisHealth));
	}
}
