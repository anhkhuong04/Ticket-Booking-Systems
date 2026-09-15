package com.lak.moviebooking.identity.application;

import java.util.Optional;
import java.util.UUID;

/** Cross-module contract used by authorization to load the current account and roles. */
public interface IdentityUserQuery {

    Optional<AuthenticatedIdentity> findActiveById(UUID userId);
}
