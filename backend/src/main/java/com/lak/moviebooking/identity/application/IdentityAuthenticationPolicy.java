package com.lak.moviebooking.identity.application;

import java.time.Duration;

public record IdentityAuthenticationPolicy(
        Duration refreshTokenTtl,
        Duration passwordResetTokenTtl,
        String passwordResetUrl) {
}
