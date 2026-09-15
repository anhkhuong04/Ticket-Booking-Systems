package com.lak.moviebooking.common.api.error;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import com.lak.moviebooking.common.api.request.RequestIdFilter;
import com.lak.moviebooking.common.application.error.ApplicationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalApiExceptionHandlerTests {

	private static final Instant NOW = Instant.parse("2026-09-15T02:00:00Z");
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		mockMvc = MockMvcBuilders.standaloneSetup(new ErrorTestController())
				.setControllerAdvice(new GlobalApiExceptionHandler(clock))
				.addFilters(new RequestIdFilter())
				.build();
	}

	@ParameterizedTest
	@MethodSource("applicationErrors")
	void mapsApplicationErrorsToStableHttpSemantics(String errorType, int expectedStatus, String expectedCode)
			throws Exception {
		mockMvc.perform(get("/test/errors/{errorType}", errorType)
					.header(RequestIdFilter.HEADER_NAME, "request-123"))
				.andExpect(status().is(expectedStatus))
				.andExpect(header().string(RequestIdFilter.HEADER_NAME, "request-123"))
				.andExpect(jsonPath("$.timestamp").value(NOW.toString()))
				.andExpect(jsonPath("$.status").value(expectedStatus))
				.andExpect(jsonPath("$.code").value(expectedCode))
				.andExpect(jsonPath("$.path").value("/test/errors/" + errorType))
				.andExpect(jsonPath("$.requestId").value("request-123"))
				.andExpect(jsonPath("$.violations").isEmpty());
	}

	@Test
	void returnsFieldViolationsForInvalidRequestBody() throws Exception {
		mockMvc.perform(post("/test/errors/validation")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.violations[0].field").value("name"))
				.andExpect(jsonPath("$.violations[0].message").value("must not be blank"));
	}

	@Test
	void replacesUnsafeRequestId() throws Exception {
		mockMvc.perform(get("/test/errors/conflict")
					.header(RequestIdFilter.HEADER_NAME, "unsafe\nrequest"))
				.andExpect(status().isConflict())
				.andExpect(header().string(
						RequestIdFilter.HEADER_NAME,
						matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
				.andExpect(jsonPath("$.requestId", matchesPattern(
						"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));
	}

	@Test
	void hidesUnexpectedExceptionDetails() throws Exception {
		mockMvc.perform(get("/test/errors/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.message").value("An unexpected error occurred"));
	}

	private static Stream<Arguments> applicationErrors() {
		return Stream.of(
				Arguments.of("unauthenticated", 401, "AUTH_REQUIRED"),
				Arguments.of("forbidden", 403, "ACCESS_DENIED"),
				Arguments.of("conflict", 409, "SEAT_HOLD_CONFLICT"),
				Arguments.of("expired", 410, "HOLD_EXPIRED"),
				Arguments.of("business", 422, "BUSINESS_RULE_VIOLATION"),
				Arguments.of("rate-limited", 429, "RATE_LIMITED"));
	}

	@RestController
	@RequestMapping("/test/errors")
	static class ErrorTestController {

		@GetMapping("/{errorType}")
		void applicationError(@PathVariable String errorType) {
			throw switch (errorType) {
				case "unauthenticated" -> ApplicationException.unauthenticated("AUTH_REQUIRED", "Authentication required");
				case "forbidden" -> ApplicationException.forbidden("ACCESS_DENIED", "Access denied");
				case "conflict" -> ApplicationException.conflict("SEAT_HOLD_CONFLICT", "Seat is unavailable");
				case "expired" -> ApplicationException.expired("HOLD_EXPIRED", "Seat hold expired");
				case "business" -> ApplicationException.businessRule(
						"BUSINESS_RULE_VIOLATION", "Business rule rejected the request");
				case "rate-limited" -> ApplicationException.rateLimited("RATE_LIMITED", "Too many requests");
				case "unexpected" -> new IllegalStateException("sensitive database detail");
				default -> new IllegalArgumentException("Unknown test error type");
			};
		}

		@PostMapping("/validation")
		void validation(@Valid @RequestBody ValidationRequest request) {
		}
	}

	record ValidationRequest(@NotBlank String name) {
	}
}
