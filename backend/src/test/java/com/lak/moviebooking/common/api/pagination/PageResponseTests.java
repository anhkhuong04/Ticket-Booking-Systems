package com.lak.moviebooking.common.api.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PageResponseTests {

	@Test
	void snapshotsContent() {
		List<String> source = new ArrayList<>(List.of("first"));
		PageResponse<String> response = new PageResponse<>(source, 0, 20, 1, 1);

		source.add("second");

		assertThat(response.content()).containsExactly("first");
	}

	@Test
	void rejectsInvalidMetadata() {
		assertThatThrownBy(() -> new PageResponse<>(List.of(), -1, 20, 0, 0))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
