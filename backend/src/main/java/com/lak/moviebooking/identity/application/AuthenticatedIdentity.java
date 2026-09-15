package com.lak.moviebooking.identity.application;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedIdentity(UUID id, String email, String fullName, Set<String> roles) {

    public AuthenticatedIdentity {
        roles = Set.copyOf(roles);
    }
}
