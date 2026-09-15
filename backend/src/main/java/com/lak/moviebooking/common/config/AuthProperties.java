package com.lak.moviebooking.common.config;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        @NotBlank String jwtHmacSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration passwordResetTokenTtl,
        @NotBlank String passwordResetUrl,
        @NotBlank String refreshCookieName,
        @NotBlank String csrfCookieName,
        boolean cookieSecure,
        @NotBlank String cookieSameSite,
        @Min(1) int loginRateLimit,
        Duration loginRateLimitWindow) {

    public AuthProperties {
        if (jwtHmacSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.auth.jwt-hmac-secret must contain at least 32 bytes");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()
                || refreshTokenTtl == null || refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()
                || passwordResetTokenTtl == null || passwordResetTokenTtl.isNegative() || passwordResetTokenTtl.isZero()
                || loginRateLimitWindow == null || loginRateLimitWindow.isNegative() || loginRateLimitWindow.isZero()) {
            throw new IllegalArgumentException("Authentication durations must be positive");
        }
        if (!(cookieSameSite.equalsIgnoreCase("Lax") || cookieSameSite.equalsIgnoreCase("Strict")
                || cookieSameSite.equalsIgnoreCase("None"))) {
            throw new IllegalArgumentException("app.auth.cookie-same-site must be Lax, Strict, or None");
        }
        if (cookieSameSite.equalsIgnoreCase("None") && !cookieSecure) {
            throw new IllegalArgumentException("SameSite=None requires a secure cookie");
        }
    }
}
