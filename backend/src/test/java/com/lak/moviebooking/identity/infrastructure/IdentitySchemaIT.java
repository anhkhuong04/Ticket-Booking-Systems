package com.lak.moviebooking.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.support.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class IdentitySchemaIT extends AbstractIntegrationTest {

	private static final String PASSWORD_HASH = "$2a$10$abcdefghijklmnopqrstuu9c5O0wceg6aKzZ0AtD.GFkX0D3enrkFvC";
	private static final String REFRESH_TOKEN_HASH = "a".repeat(64);
	private static final String PASSWORD_RESET_TOKEN_HASH = "b".repeat(64);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void seedsTheFourRolesDefinedBySystemDesign() {
		List<String> roleCodes = jdbcTemplate.queryForList(
				"SELECT code FROM roles ORDER BY code",
				String.class);

		assertThat(roleCodes).containsExactly(
				"CINEMA_MANAGER",
				"CUSTOMER",
				"SUPER_ADMIN",
				"TICKET_STAFF");
	}

	@Test
	void normalizesContactsBeforeStorageAndEnforcesTheirNormalizedUniqueness() {
		UUID firstUserId = UUID.randomUUID();
		insertUser(firstUserId, "  Customer@Example.com  ", "+84 912 345 678");

		assertThat(jdbcTemplate.queryForObject(
				"SELECT email FROM users WHERE id = ?",
				String.class,
				firstUserId)).isEqualTo("customer@example.com");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT phone FROM users WHERE id = ?",
				String.class,
				firstUserId)).isEqualTo("0912345678");

		assertThatThrownBy(() -> insertUser(UUID.randomUUID(), "customer@example.com", "0901000000"))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> insertUser(UUID.randomUUID(), "another@example.com", "0912-345-678"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void storesOnlySha256TokenHashesForRefreshAndPasswordResetTokens() {
		UUID userId = UUID.randomUUID();
		insertUser(userId, "tokens@example.com", null);
		Timestamp now = Timestamp.from(Instant.now());
		Timestamp expiresAt = Timestamp.from(now.toInstant().plusSeconds(600));

		jdbcTemplate.update(
				"""
					INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, created_at, updated_at)
					VALUES (?, ?, ?, ?, ?, ?)
					""",
				UUID.randomUUID(), userId, REFRESH_TOKEN_HASH, expiresAt, now, now);
		jdbcTemplate.update(
				"""
					INSERT INTO password_reset_tokens (id, user_id, token_hash, expires_at, created_at, updated_at)
					VALUES (?, ?, ?, ?, ?, ?)
					""",
				UUID.randomUUID(), userId, PASSWORD_RESET_TOKEN_HASH, expiresAt, now, now);

		assertThatThrownBy(() -> jdbcTemplate.update(
				"""
					INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, created_at, updated_at)
					VALUES (?, ?, 'raw-refresh-token', ?, ?, ?)
					""",
				UUID.randomUUID(), userId, expiresAt, now, now))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbcTemplate.update(
				"""
					INSERT INTO password_reset_tokens (id, user_id, token_hash, expires_at, created_at, updated_at)
					VALUES (?, ?, 'raw-reset-token', ?, ?, ?)
					""",
				UUID.randomUUID(), userId, expiresAt, now, now))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void insertUser(UUID userId, String email, String phone) {
		Timestamp now = Timestamp.from(Instant.now());
		jdbcTemplate.update(
				"""
					INSERT INTO users (id, email, phone, password_hash, full_name, status, created_at, updated_at)
					VALUES (?, ?, ?, ?, 'Identity Schema Test User', 'ACTIVE', ?, ?)
					""",
				userId, email, phone, PASSWORD_HASH, now, now);
	}
}
