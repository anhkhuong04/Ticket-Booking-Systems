package com.lak.moviebooking.identity.application;

public interface LoginRateLimit {

    void check(String email, String clientAddress);
}
