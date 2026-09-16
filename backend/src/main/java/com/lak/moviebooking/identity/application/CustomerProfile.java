package com.lak.moviebooking.identity.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import com.lak.moviebooking.audit.application.AuditLogWriter;
import com.lak.moviebooking.common.application.error.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerProfile {
    private static final ZoneId CUSTOMER_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final CustomerProfileRepository repository;
    private final AuditLogWriter audit;
    private final Clock clock;

    public CustomerProfile(CustomerProfileRepository repository, AuditLogWriter audit, Clock clock) {
        this.repository = repository;
        this.audit = audit;
        this.clock = clock;
    }

    public CustomerProfileView find(UUID userId) { return repository.find(userId); }

    @Transactional
    public CustomerProfileView update(UUID userId, String fullName, String phone, LocalDate birthDate) {
        String name = fullName.trim();
        if (name.isEmpty() || birthDate != null && (birthDate.isBefore(LocalDate.of(1900, 1, 1))
                || birthDate.isAfter(LocalDate.now(clock.withZone(CUSTOMER_ZONE))))) {
            throw ApplicationException.businessRule("INVALID_PROFILE", "Profile data is invalid");
        }
        CustomerProfileView updated = repository.update(userId, name, phone, birthDate, clock.instant());
        audit.record(userId, "CUSTOMER_PROFILE_UPDATED", "user", userId, Map.of());
        return updated;
    }

    public BillingPreferencesView findBilling(UUID userId) { return repository.findBilling(userId); }

    @Transactional
    public BillingPreferencesView updateBilling(UUID userId, BillingPreferencesView value) {
        if (!"PERSONAL".equals(value.recipientType()) && !"BUSINESS".equals(value.recipientType())) {
            throw ApplicationException.businessRule("INVALID_BILLING", "Billing information is invalid");
        }
        if ("BUSINESS".equals(value.recipientType()) && (blank(value.taxCode()) || blank(value.address()))) {
            throw ApplicationException.businessRule("INVALID_BILLING", "Business tax code and address are required");
        }
        BillingPreferencesView updated = repository.updateBilling(userId, value, clock.instant());
        audit.record(userId, "CUSTOMER_BILLING_UPDATED", "user", userId, Map.of());
        return updated;
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
