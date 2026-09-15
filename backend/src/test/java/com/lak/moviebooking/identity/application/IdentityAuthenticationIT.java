package com.lak.moviebooking.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.support.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class IdentityAuthenticationIT extends AbstractIntegrationTest {

    @Autowired
    private IdentityAuthenticationService authenticationService;

    @Autowired
    private AccessTokenVerifier accessTokenVerifier;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rotatesRefreshTokensAndRevokesEverySessionWhenAnOldTokenIsReused() {
        RefreshSessionResult first = authenticationService.register("Customer One", "customer-one@example.com", "a-secure-password");
        RefreshSessionResult second = authenticationService.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThatThrownBy(() -> authenticationService.refresh(first.refreshToken()))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).code())
                .isEqualTo("REFRESH_TOKEN_REUSED");
        assertThatThrownBy(() -> authenticationService.refresh(second.refreshToken()))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).code())
                .isEqualTo("REFRESH_TOKEN_REUSED");
    }

    @Test
    void rejectsAnExpiredAccessToken() {
        RefreshSessionResult session = authenticationService.register("Customer Two", "customer-two@example.com", "a-secure-password");

        assertThatThrownBy(() -> accessTokenVerifier.verify(session.session().accessToken(), Instant.parse("2099-01-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesLoginForLockedAccounts() {
        authenticationService.register("Customer Three", "customer-three@example.com", "a-secure-password");
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE email = 'customer-three@example.com'");

        assertThatThrownBy(() -> authenticationService.login("customer-three@example.com", "a-secure-password"))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).code())
                .isEqualTo("ACCOUNT_LOCKED");
    }

    @Test
    void storesOnlyTheHashOfResetTokensAndInvalidatesThemAfterUse() {
        authenticationService.register("Customer Four", "customer-four@example.com", "a-secure-password");
        authenticationService.requestPasswordReset("customer-four@example.com");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM password_reset_tokens WHERE token_hash ~ '^[0-9a-f]{64}$'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE event_type = 'notification.email.requested'", Integer.class)).isEqualTo(1);
    }
}
