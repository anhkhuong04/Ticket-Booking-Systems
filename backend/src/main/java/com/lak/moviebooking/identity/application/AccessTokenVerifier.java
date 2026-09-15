package com.lak.moviebooking.identity.application;

import java.time.Instant;
import java.util.UUID;

public interface AccessTokenVerifier {

    UUID verify(String token, Instant now);
}
