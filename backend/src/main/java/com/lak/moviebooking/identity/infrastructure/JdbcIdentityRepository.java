package com.lak.moviebooking.identity.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.lak.moviebooking.identity.application.IdentityAccount;
import com.lak.moviebooking.identity.application.IdentityRepository;
import com.lak.moviebooking.identity.application.StoredPasswordResetToken;
import com.lak.moviebooking.identity.application.StoredRefreshToken;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcIdentityRepository implements IdentityRepository {

    private static final UUID CUSTOMER_ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final JdbcTemplate jdbcTemplate;

    JdbcIdentityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<IdentityAccount> findByEmail(String normalizedEmail) {
        return findAccount("SELECT id, email, full_name, password_hash, status FROM users WHERE email = ?", normalizedEmail);
    }

    @Override
    public Optional<IdentityAccount> findById(UUID userId) {
        return findAccount("SELECT id, email, full_name, password_hash, status FROM users WHERE id = ?", userId);
    }

    @Override
    public boolean createCustomer(UUID userId, String email, String fullName, String passwordHash, Instant now) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO users (id, email, password_hash, full_name, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'ACTIVE', ?, ?)
                    """, userId, email, passwordHash, fullName, atUtc(now), atUtc(now));
            jdbcTemplate.update("""
                    INSERT INTO user_roles (user_id, role_id, assigned_at)
                    VALUES (?, ?, ?)
                    """, userId, CUSTOMER_ROLE_ID, atUtc(now));
            return true;
        }
        catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    @Override
    public Optional<StoredRefreshToken> findRefreshTokenForUpdate(String tokenHash) {
        List<StoredRefreshToken> tokens = jdbcTemplate.query("""
                SELECT id, user_id, expires_at, revoked_at
                FROM refresh_tokens
                WHERE token_hash = ?
                FOR UPDATE
                """, this::mapRefreshToken, tokenHash);
        return tokens.stream().findFirst();
    }

    @Override
    public void createRefreshToken(UUID tokenId, UUID userId, String tokenHash, Instant expiresAt, Instant now) {
        jdbcTemplate.update("""
                INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, tokenId, userId, tokenHash, atUtc(expiresAt), atUtc(now), atUtc(now));
    }

    @Override
    public void rotateRefreshToken(UUID tokenId, UUID replacementTokenId, Instant now) {
        jdbcTemplate.update("""
                UPDATE refresh_tokens
                SET revoked_at = ?, revocation_reason = 'ROTATED', replaced_by_token_id = ?, updated_at = ?
                WHERE id = ? AND revoked_at IS NULL
                """, atUtc(now), replacementTokenId, atUtc(now), tokenId);
    }

    @Override
    public void revokeRefreshToken(String tokenHash, String reason, Instant now) {
        jdbcTemplate.update("""
                UPDATE refresh_tokens
                SET revoked_at = ?, revocation_reason = ?, updated_at = ?
                WHERE token_hash = ? AND revoked_at IS NULL
                """, atUtc(now), reason, atUtc(now), tokenHash);
    }

    @Override
    public void revokeActiveRefreshTokens(UUID userId, String reason, Instant now) {
        jdbcTemplate.update("""
                UPDATE refresh_tokens
                SET revoked_at = ?, revocation_reason = ?, updated_at = ?
                WHERE user_id = ? AND revoked_at IS NULL
                """, atUtc(now), reason, atUtc(now), userId);
    }

    @Override
    public Optional<StoredPasswordResetToken> findPasswordResetTokenForUpdate(String tokenHash) {
        List<StoredPasswordResetToken> tokens = jdbcTemplate.query("""
                SELECT id, user_id, expires_at, used_at
                FROM password_reset_tokens
                WHERE token_hash = ?
                FOR UPDATE
                """, this::mapPasswordResetToken, tokenHash);
        return tokens.stream().findFirst();
    }

    @Override
    public void createPasswordResetToken(UUID tokenId, UUID userId, String tokenHash, Instant expiresAt, Instant now) {
        jdbcTemplate.update("""
                INSERT INTO password_reset_tokens (id, user_id, token_hash, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, tokenId, userId, tokenHash, atUtc(expiresAt), atUtc(now), atUtc(now));
    }

    @Override
    public void consumeOtherPasswordResetTokens(UUID userId, Instant now) {
        jdbcTemplate.update("""
                UPDATE password_reset_tokens
                SET used_at = ?, updated_at = ?
                WHERE user_id = ? AND used_at IS NULL
                """, atUtc(now), atUtc(now), userId);
    }

    @Override
    public void consumePasswordResetToken(UUID tokenId, Instant now) {
        jdbcTemplate.update("""
                UPDATE password_reset_tokens
                SET used_at = ?, updated_at = ?
                WHERE id = ? AND used_at IS NULL
                """, atUtc(now), atUtc(now), tokenId);
    }

    @Override
    public void updatePassword(UUID userId, String passwordHash, Instant now) {
        jdbcTemplate.update("""
                UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?
                """, passwordHash, atUtc(now), userId);
    }

    private Optional<IdentityAccount> findAccount(String sql, Object parameter) {
        List<IdentityAccount> accounts = jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapAccount(resultSet), parameter);
        return accounts.stream().findFirst();
    }

    private IdentityAccount mapAccount(ResultSet resultSet) throws SQLException {
        UUID userId = resultSet.getObject("id", UUID.class);
        return new IdentityAccount(
                userId,
                resultSet.getString("email"),
                resultSet.getString("full_name"),
                resultSet.getString("password_hash"),
                "ACTIVE".equals(resultSet.getString("status")),
                rolesFor(userId));
    }

    private Set<String> rolesFor(UUID userId) {
        return Set.copyOf(jdbcTemplate.queryForList("""
                SELECT role.code FROM roles role
                JOIN user_roles user_role ON user_role.role_id = role.id
                WHERE user_role.user_id = ?
                """, String.class, userId));
    }

    private StoredRefreshToken mapRefreshToken(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StoredRefreshToken(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getObject("expires_at", OffsetDateTime.class).toInstant(),
                optionalInstant(resultSet, "revoked_at"));
    }

    private StoredPasswordResetToken mapPasswordResetToken(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StoredPasswordResetToken(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getObject("expires_at", OffsetDateTime.class).toInstant(),
                optionalInstant(resultSet, "used_at"));
    }

    private Instant optionalInstant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static OffsetDateTime atUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
