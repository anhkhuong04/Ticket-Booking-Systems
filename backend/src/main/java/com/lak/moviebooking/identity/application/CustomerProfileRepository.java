package com.lak.moviebooking.identity.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface CustomerProfileRepository {
    CustomerProfileView find(UUID userId);
    CustomerProfileView update(UUID userId, String fullName, String phone, LocalDate birthDate, Instant now);
    BillingPreferencesView findBilling(UUID userId);
    BillingPreferencesView updateBilling(UUID userId, BillingPreferencesView value, Instant now);
}
