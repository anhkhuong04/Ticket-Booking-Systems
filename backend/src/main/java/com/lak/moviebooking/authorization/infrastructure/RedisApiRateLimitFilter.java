package com.lak.moviebooking.authorization.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

import com.lak.moviebooking.common.config.RateLimitProperties;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Limits high-cost or brute-forceable API paths using an opaque actor/IP key. */
class RedisApiRateLimitFilter extends OncePerRequestFilter {
	private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>(
			"local current=redis.call('INCR',KEYS[1]); if current==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end; return current;",
			Long.class);

	private final StringRedisTemplate redis;
	private final RateLimitProperties properties;
	private final ApiSecurityResponseWriter responseWriter;

	RedisApiRateLimitFilter(
			StringRedisTemplate redis,
			RateLimitProperties properties,
			ApiSecurityResponseWriter responseWriter) {
		this.redis = redis;
		this.properties = properties;
		this.responseWriter = responseWriter;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Limit limit = limitFor(request);
		if (limit != null) {
			String actor = principalKey(request);
			Long count = redis.execute(INCREMENT, List.of("rate-limit:" + limit.name + ":" + hash(actor)),
					Long.toString(limit.window.toSeconds()));
			if (count == null || count > limit.max) {
				response.setHeader("Retry-After", Long.toString(Math.max(1, limit.window.toSeconds())));
				responseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS.value(),
						"RATE_LIMITED", "Too many requests. Please try again later");
				return;
			}
		}
		chain.doFilter(request, response);
	}

	private Limit limitFor(HttpServletRequest request) {
		if (!"POST".equalsIgnoreCase(request.getMethod())) {
			return null;
		}
		String path = request.getRequestURI();
		if ("/api/seat-holds".equals(path)) {
			return new Limit("hold", properties.holdLimit(), properties.holdWindow());
		}
		if ("/api/bookings/checkout".equals(path)) {
			return new Limit("checkout", properties.checkoutLimit(), properties.checkoutWindow());
		}
		if (path.matches("/api/payments/[^/]+/webhook")) {
			return new Limit("webhook", properties.webhookLimit(), properties.webhookWindow());
		}
		if ("/api/tickets/validate".equals(path)) {
			return new Limit("scan", properties.scanLimit(), properties.scanWindow());
		}
		return null;
	}

	private String principalKey(HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal actor) {
			return "user:" + actor.userId();
		}
		return "ip:" + request.getRemoteAddr();
	}

	private String hash(String value) {
		try {
			return java.util.HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 unavailable", exception);
		}
	}

	private record Limit(String name, int max, Duration window) { }
}
