package com.lak.moviebooking.identity.application;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface AccessTokenIssuer {

    String issue(UUID userId, Set<String> roles, Instant issuedAt);
}
