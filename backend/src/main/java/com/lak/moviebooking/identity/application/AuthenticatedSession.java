package com.lak.moviebooking.identity.application;

public record AuthenticatedSession(String accessToken, AuthenticatedIdentity user) {
}
