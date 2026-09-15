package com.lak.moviebooking.identity.application;

import java.util.UUID;

/** Internal-to-API result: the opaque token is written only to an HttpOnly cookie. */
public record RefreshSessionResult(AuthenticatedSession session, String refreshToken, UUID refreshTokenId) {
}
