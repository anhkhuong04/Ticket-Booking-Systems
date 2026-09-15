package com.lak.moviebooking.common.api.request;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Request-Id";
	public static final String ATTRIBUTE_NAME = RequestIdFilter.class.getName() + ".requestId";
	private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String requestId = resolveRequestId(request.getHeader(HEADER_NAME));
		request.setAttribute(ATTRIBUTE_NAME, requestId);
		response.setHeader(HEADER_NAME, requestId);

		try (MDC.MDCCloseable ignored = MDC.putCloseable("request_id", requestId)) {
			filterChain.doFilter(request, response);
		}
	}

	private String resolveRequestId(String candidate) {
		if (candidate != null) {
			String value = candidate.trim();
			if (SAFE_REQUEST_ID.matcher(value).matches()) {
				return value;
			}
		}
		return UUID.randomUUID().toString();
	}
}
