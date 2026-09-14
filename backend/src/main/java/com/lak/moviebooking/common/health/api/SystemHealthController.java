package com.lak.moviebooking.common.health.api;

import com.lak.moviebooking.common.health.application.SystemHealthResponse;
import com.lak.moviebooking.common.health.application.SystemHealthQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class SystemHealthController {

	private final SystemHealthQuery systemHealthQuery;

	public SystemHealthController(SystemHealthQuery systemHealthQuery) {
		this.systemHealthQuery = systemHealthQuery;
	}

	@GetMapping
	public ResponseEntity<SystemHealthResponse> health() {
		SystemHealthResponse response = systemHealthQuery.check();
		return ResponseEntity.status(response.matchesUpStatus() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
				.body(response);
	}
}
