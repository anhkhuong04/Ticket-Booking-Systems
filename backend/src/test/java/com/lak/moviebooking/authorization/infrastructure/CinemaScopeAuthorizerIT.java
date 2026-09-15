package com.lak.moviebooking.authorization.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.lak.moviebooking.authorization.application.CinemaScopeAuthorizer;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import com.lak.moviebooking.support.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class CinemaScopeAuthorizerIT extends AbstractIntegrationTest {

    @Autowired
    private CinemaScopeAuthorizer cinemaScopeAuthorizer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deniesManagerAccessToAnotherCinemaAndAuditsTheDecision() {
        UUID managerId = createUser();
        UUID assignedCinema = createCinema("Assigned Cinema");
        UUID otherCinema = createCinema("Other Cinema");
        jdbcTemplate.update("INSERT INTO staff_cinema_assignments (user_id, cinema_id, assigned_at) VALUES (?, ?, ?)",
                managerId, assignedCinema, Timestamp.from(Instant.now()));
        AuthenticatedPrincipal manager = new AuthenticatedPrincipal(managerId, "manager@example.com", "Manager",
                Set.of("CINEMA_MANAGER"));
        AuthenticatedPrincipal staff = new AuthenticatedPrincipal(managerId, "manager@example.com", "Staff",
                Set.of("TICKET_STAFF"));

        cinemaScopeAuthorizer.requireAccess(manager, assignedCinema);
        cinemaScopeAuthorizer.requireAccess(staff, assignedCinema);

        assertThatThrownBy(() -> cinemaScopeAuthorizer.requireAccess(manager, otherCinema))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).code())
                .isEqualTo("CINEMA_SCOPE_DENIED");
        assertThatThrownBy(() -> cinemaScopeAuthorizer.requireAccess(staff, otherCinema))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).code())
                .isEqualTo("CINEMA_SCOPE_DENIED");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM audit_logs
                WHERE actor_id = ? AND action = 'AUTHORIZATION_DENIED'
                """, Integer.class, managerId)).isEqualTo(2);
    }

    private UUID createUser() {
        UUID userId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, full_name, status, created_at, updated_at)
                VALUES (?, ?, '$2a$10$abcdefghijklmnopqrstuu9c5O0wceg6aKzZ0AtD.GFkX0D3enrkFvC', 'Manager', 'ACTIVE', ?, ?)
                """, userId, "manager-" + userId + "@example.com", now, now);
        return userId;
    }

    private UUID createCinema(String name) {
        UUID cinemaId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO cinemas (id, name, address, city, timezone, status, created_at, updated_at)
                VALUES (?, ?, '1 Test Street', 'Ho Chi Minh City', 'Asia/Ho_Chi_Minh', 'ACTIVE', ?, ?)
                """, cinemaId, name, now, now);
        return cinemaId;
    }
}
