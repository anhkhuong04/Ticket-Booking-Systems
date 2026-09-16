package com.lak.moviebooking.authorization.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;

import com.lak.moviebooking.common.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class RedisApiRateLimitFilterTests {

	private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
	private final ApiSecurityResponseWriter responseWriter = mock(ApiSecurityResponseWriter.class);
	private final RedisApiRateLimitFilter filter = new RedisApiRateLimitFilter(redis,
			new RateLimitProperties(2, Duration.ofSeconds(30), 2, Duration.ofSeconds(30),
					2, Duration.ofSeconds(30), 2, Duration.ofSeconds(30)), responseWriter);

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void rejectsOverLimitTicketScanWithoutCallingTheController() throws Exception {
		when(redis.execute(any(DefaultRedisScript.class), anyList(), anyString())).thenReturn(3L);
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tickets/validate");
		request.setRemoteAddr("198.51.100.24");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		assertThat(response.getHeader("Retry-After")).isEqualTo("30");
		verify(responseWriter).write(request, response, 429, "RATE_LIMITED", "Too many requests. Please try again later");
		verifyNoInteractions(chain);
	}

	@Test
	void doesNotRateLimitUnmatchedReadRequests() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets/validate");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		verifyNoInteractions(redis, responseWriter);
	}
}
