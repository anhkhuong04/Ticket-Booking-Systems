package com.lak.moviebooking.identity.application;

import java.time.Instant;
import java.util.UUID;

public record StoredRefreshToken(UUID id, UUID userId, Instant expiresAt, Instant revokedAt) {
}
