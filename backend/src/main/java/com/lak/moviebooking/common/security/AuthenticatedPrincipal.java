package com.lak.moviebooking.common.security;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedPrincipal(UUID userId, String email, String fullName, Set<String> roles) {
    public AuthenticatedPrincipal {
        roles = Set.copyOf(roles);
    }
}
