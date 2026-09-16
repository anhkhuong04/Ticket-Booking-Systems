package com.lak.moviebooking.authorization.infrastructure;

import java.io.IOException;

import com.lak.moviebooking.audit.application.DeniedAccessAudit;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import com.lak.moviebooking.identity.application.AccessTokenVerifier;
import com.lak.moviebooking.identity.application.IdentityUserQuery;
import com.lak.moviebooking.common.config.AuthProperties;
import com.lak.moviebooking.common.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;

@Configuration
class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AccessTokenVerifier accessTokenVerifier,
            IdentityUserQuery identityUserQuery,
            ApiSecurityResponseWriter responseWriter,
            DeniedAccessAudit deniedAccessAudit,
            AuthProperties properties,
            RateLimitProperties rateLimitProperties,
            StringRedisTemplate redisTemplate,
            java.time.Clock clock) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(
                accessTokenVerifier, identityUserQuery, clock, responseWriter);
        ApiCsrfFilter csrfFilter = new ApiCsrfFilter(properties, responseWriter);
        RedisApiRateLimitFilter rateLimitFilter = new RedisApiRateLimitFilter(redisTemplate, rateLimitProperties, responseWriter);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'; base-uri 'self'; object-src 'none'"))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> responseWriter.write(
                                request, response, HttpServletResponse.SC_UNAUTHORIZED,
                                "AUTH_REQUIRED", "Authentication is required"))
                        .accessDeniedHandler((request, response, exception) -> {
                            recordDeniedAccess(request, request.getUserPrincipal(), deniedAccessAudit);
                            responseWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN,
                                    "ACCESS_DENIED", "You do not have permission to perform this action");
                        }))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/health", "/api/auth/register", "/api/auth/login",
                                "/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/csrf")
                        .permitAll()
                        .requestMatchers("/api/auth/session").authenticated()
                        .requestMatchers("/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers("/api/payments/*/webhook").permitAll()
                        .requestMatchers("/api/movies/**", "/api/cinemas/**", "/api/genres").permitAll()
                        .requestMatchers("/api/showtimes/**").permitAll()
                        .requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "CINEMA_MANAGER")
                        .requestMatchers("/api/staff/**").hasAnyRole("SUPER_ADMIN", "TICKET_STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/validate").hasAnyRole("SUPER_ADMIN", "TICKET_STAFF")
                        .requestMatchers("/api/me/**", "/api/seat-holds/**", "/api/bookings/**", "/api/payments/**", "/api/refunds/**")
                        .hasRole("CUSTOMER")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(csrfFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    private void recordDeniedAccess(HttpServletRequest request, java.security.Principal principal, DeniedAccessAudit audit) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthenticatedPrincipal actor) {
            audit.record(actor.userId(), request.getRequestURI(), "ROLE_REQUIRED");
        }
    }
}
