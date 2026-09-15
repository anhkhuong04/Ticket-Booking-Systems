package com.lak.moviebooking.identity.application;

import java.util.Set;
import java.util.UUID;

public record IdentityAccount(
        UUID id,
        String email,
        String fullName,
        String passwordHash,
        boolean active,
        Set<String> roles) {

    public IdentityAccount {
        roles = Set.copyOf(roles);
    }
}
