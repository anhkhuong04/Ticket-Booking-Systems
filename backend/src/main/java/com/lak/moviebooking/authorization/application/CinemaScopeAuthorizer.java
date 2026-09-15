package com.lak.moviebooking.authorization.application;

import java.util.UUID;

import com.lak.moviebooking.common.security.AuthenticatedPrincipal;

public interface CinemaScopeAuthorizer {

    void requireAccess(AuthenticatedPrincipal actor, UUID cinemaId);
}
