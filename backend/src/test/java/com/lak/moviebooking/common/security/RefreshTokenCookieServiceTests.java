package com.lak.moviebooking.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import com.lak.moviebooking.common.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class RefreshTokenCookieServiceTests {

	@Test
	void writesHttpOnlySecureAndSameSiteRefreshCookie() {
		RefreshTokenCookieService service = new RefreshTokenCookieService(properties(true, "Strict"));
		MockHttpServletResponse response = new MockHttpServletResponse();

		service.write(response, "refresh-token-value");

		assertThat(response.getHeader("Set-Cookie"))
				.contains("Path=/api/auth", "HttpOnly", "Secure", "SameSite=Strict")
				.doesNotContain("refresh-token-value; Domain");
	}

	@Test
	void refusesCrossSiteCookieWithoutSecureFlag() {
		assertThatThrownBy(() -> properties(false, "None"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("SameSite=None requires a secure cookie");
	}

	private AuthProperties properties(boolean secure, String sameSite) {
		return new AuthProperties("a-test-secret-that-is-longer-than-thirty-two-bytes", Duration.ofMinutes(15),
				Duration.ofDays(7), Duration.ofMinutes(30), "https://app.example.com/reset", "refresh", "csrf",
				secure, sameSite, 5, Duration.ofMinutes(1));
	}
}
