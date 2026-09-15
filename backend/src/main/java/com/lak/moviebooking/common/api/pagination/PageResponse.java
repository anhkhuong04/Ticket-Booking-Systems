package com.lak.moviebooking.common.api.pagination;

import java.util.List;

public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public PageResponse {
		content = List.copyOf(content);
		if (page < 0 || size < 1 || totalElements < 0 || totalPages < 0) {
			throw new IllegalArgumentException("Page metadata must not be negative and size must be positive");
		}
	}
}
