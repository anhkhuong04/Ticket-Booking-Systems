package com.lak.moviebooking.common.security;

import com.lak.moviebooking.common.config.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieService {
    private final AuthProperties properties;

    public RefreshTokenCookieService(AuthProperties properties) { this.properties = properties; }

    public void write(HttpServletResponse response, String token) {
        response.addHeader("Set-Cookie", ResponseCookie.from(properties.refreshCookieName(), token)
                .path("/api/auth").secure(properties.cookieSecure()).httpOnly(true)
                .sameSite(properties.cookieSameSite()).maxAge(properties.refreshTokenTtl()).build().toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader("Set-Cookie", ResponseCookie.from(properties.refreshCookieName(), "")
                .path("/api/auth").secure(properties.cookieSecure()).httpOnly(true)
                .sameSite(properties.cookieSameSite()).maxAge(0).build().toString());
    }

    public String read(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (jakarta.servlet.http.Cookie cookie : cookies) {
            if (properties.refreshCookieName().equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
