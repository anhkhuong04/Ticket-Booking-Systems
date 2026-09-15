package com.lak.moviebooking.common.security;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

import com.lak.moviebooking.common.config.AuthProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CsrfCookieService {
    private final AuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public CsrfCookieService(AuthProperties properties) { this.properties = properties; }

    public void issue(HttpServletResponse response) {
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        response.addHeader("Set-Cookie", ResponseCookie.from(properties.csrfCookieName(),
                        Base64.getUrlEncoder().withoutPadding().encodeToString(value))
                .path("/").secure(properties.cookieSecure()).httpOnly(false)
                .sameSite(properties.cookieSameSite()).maxAge(Duration.ofDays(1)).build().toString());
    }
}
