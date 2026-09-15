package com.lak.moviebooking.common.api.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message,
		String path,
		String requestId,
		List<ApiFieldViolation> violations) {

	public ApiErrorResponse {
		violations = List.copyOf(violations);
	}
}
