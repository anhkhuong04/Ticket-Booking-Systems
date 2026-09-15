package com.lak.moviebooking.identity.application;

public interface OpaqueTokenService {

    String generate();

    String hash(String rawToken);
}
