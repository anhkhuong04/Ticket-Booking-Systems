package com.lak.moviebooking.identity.application;

import java.time.Instant;
import java.util.UUID;

public record StoredPasswordResetToken(UUID id, UUID userId, Instant expiresAt, Instant usedAt) {
}
