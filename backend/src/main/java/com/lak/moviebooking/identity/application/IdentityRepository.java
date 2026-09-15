package com.lak.moviebooking.identity.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdentityRepository {

    Optional<IdentityAccount> findByEmail(String normalizedEmail);

    Optional<IdentityAccount> findById(UUID userId);

    boolean createCustomer(UUID userId, String email, String fullName, String passwordHash, Instant now);

    Optional<StoredRefreshToken> findRefreshTokenForUpdate(String tokenHash);

    void createRefreshToken(UUID tokenId, UUID userId, String tokenHash, Instant expiresAt, Instant now);

    void rotateRefreshToken(UUID tokenId, UUID replacementTokenId, Instant now);

    void revokeRefreshToken(String tokenHash, String reason, Instant now);

    void revokeActiveRefreshTokens(UUID userId, String reason, Instant now);

    Optional<StoredPasswordResetToken> findPasswordResetTokenForUpdate(String tokenHash);

    void createPasswordResetToken(UUID tokenId, UUID userId, String tokenHash, Instant expiresAt, Instant now);

    void consumeOtherPasswordResetTokens(UUID userId, Instant now);

    void consumePasswordResetToken(UUID tokenId, Instant now);

    void updatePassword(UUID userId, String passwordHash, Instant now);
}
