package com.lak.moviebooking.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class CorsPropertiesTests {

	@Test
	void acceptsExplicitHttpOriginsOnly() {
		CorsProperties properties = new CorsProperties(List.of("https://app.example.com", "http://localhost:5173"));

		assertThat(properties.allowedOrigins()).containsExactly("https://app.example.com", "http://localhost:5173");
	}

	@Test
	void rejectsWildcardPathAndNonHttpOrigins() {
		assertThatThrownBy(() -> new CorsProperties(List.of("*")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new CorsProperties(List.of("https://app.example.com/client")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new CorsProperties(List.of("file:///tmp/client")))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
