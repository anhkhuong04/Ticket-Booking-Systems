package com.lak.moviebooking.authorization.application;

import java.util.UUID;
import java.util.Set;

import com.lak.moviebooking.common.security.AuthenticatedPrincipal;

public interface CinemaScopeAuthorizer {

    void requireAccess(AuthenticatedPrincipal actor, UUID cinemaId);

    void requireSuperAdmin(AuthenticatedPrincipal actor, String path);

    Set<UUID> accessibleCinemaIds(AuthenticatedPrincipal actor);
}
