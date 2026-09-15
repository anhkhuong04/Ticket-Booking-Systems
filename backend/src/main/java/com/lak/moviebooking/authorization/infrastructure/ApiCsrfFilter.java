package com.lak.moviebooking.authorization.infrastructure;

import java.io.IOException;
import java.security.MessageDigest;

import com.lak.moviebooking.common.config.AuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/** Protects the two endpoints whose authority comes from the refresh-token cookie. */
class ApiCsrfFilter extends OncePerRequestFilter {

    private static final String CSRF_HEADER = "X-CSRF-Token";
    private final AuthProperties properties;
    private final ApiSecurityResponseWriter responseWriter;

    ApiCsrfFilter(AuthProperties properties, ApiSecurityResponseWriter responseWriter) {
        this.properties = properties;
        this.responseWriter = responseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !("/api/auth/refresh".equals(path) || "/api/auth/logout".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String cookieValue = cookieValue(request, properties.csrfCookieName());
        String headerValue = request.getHeader(CSRF_HEADER);
        if (cookieValue == null || headerValue == null
                || !MessageDigest.isEqual(cookieValue.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        headerValue.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            responseWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "CSRF_TOKEN_INVALID", "The security token is missing or invalid");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
