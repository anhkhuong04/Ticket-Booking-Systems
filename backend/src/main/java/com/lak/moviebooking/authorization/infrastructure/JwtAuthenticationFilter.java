package com.lak.moviebooking.authorization.infrastructure;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.identity.application.AccessTokenVerifier;
import com.lak.moviebooking.identity.application.AuthenticatedIdentity;
import com.lak.moviebooking.identity.application.IdentityUserQuery;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AccessTokenVerifier accessTokenVerifier;
    private final IdentityUserQuery identityUserQuery;
    private final Clock clock;
    private final ApiSecurityResponseWriter responseWriter;

    JwtAuthenticationFilter(
            AccessTokenVerifier accessTokenVerifier,
            IdentityUserQuery identityUserQuery,
            Clock clock,
            ApiSecurityResponseWriter responseWriter) {
        this.accessTokenVerifier = accessTokenVerifier;
        this.identityUserQuery = identityUserQuery;
        this.clock = clock;
        this.responseWriter = responseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith("Bearer ") || authorization.length() <= "Bearer ".length()) {
            responseWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    "ACCESS_TOKEN_INVALID", "Authentication is required");
            return;
        }
        try {
            UUID userId = accessTokenVerifier.verify(authorization.substring("Bearer ".length()), clock.instant());
            AuthenticatedIdentity identity = identityUserQuery.findActiveById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Account is unavailable"));
            List<SimpleGrantedAuthority> authorities = identity.roles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                    identity.id(), identity.email(), identity.fullName(), identity.roles());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, authorities));
        }
        catch (IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            responseWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    "ACCESS_TOKEN_INVALID", "Authentication is required");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
