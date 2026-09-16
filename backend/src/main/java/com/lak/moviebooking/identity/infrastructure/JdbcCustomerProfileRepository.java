package com.lak.moviebooking.identity.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.identity.application.BillingPreferencesView;
import com.lak.moviebooking.identity.application.CustomerProfileRepository;
import com.lak.moviebooking.identity.application.CustomerProfileView;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcCustomerProfileRepository implements CustomerProfileRepository {
    private final JdbcTemplate jdbc;

    JdbcCustomerProfileRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public CustomerProfileView find(UUID userId) {
        return jdbc.query("SELECT full_name,email,phone,birth_date FROM users WHERE id=?",
                (rs, row) -> new CustomerProfileView(rs.getString("full_name"), rs.getString("email"),
                        rs.getString("phone"), rs.getObject("birth_date", LocalDate.class)), userId)
                .stream().findFirst().orElseThrow(() -> ApplicationException.notFound("USER_NOT_FOUND", "Account was not found"));
    }

    @Override
    public CustomerProfileView update(UUID userId, String fullName, String phone, LocalDate birthDate, Instant now) {
        try {
            jdbc.update("UPDATE users SET full_name=?,phone=?,birth_date=?,updated_at=? WHERE id=?",
                    fullName, phone == null || phone.isBlank() ? null : phone, birthDate, atUtc(now), userId);
        } catch (DataIntegrityViolationException exception) {
            throw ApplicationException.conflict("PROFILE_CONTACT_CONFLICT", "Phone number is already in use or invalid");
        }
        return find(userId);
    }

    @Override
    public BillingPreferencesView findBilling(UUID userId) {
        return jdbc.query("SELECT recipient_type,recipient_name,tax_code,address,email FROM customer_billing_preferences WHERE user_id=?",
                (rs, row) -> new BillingPreferencesView(rs.getString("recipient_type"), rs.getString("recipient_name"),
                        rs.getString("tax_code"), rs.getString("address"), rs.getString("email")), userId)
                .stream().findFirst().orElse(null);
    }

    @Override
    public BillingPreferencesView updateBilling(UUID userId, BillingPreferencesView value, Instant now) {
        jdbc.update("""
                INSERT INTO customer_billing_preferences (user_id,recipient_type,recipient_name,tax_code,address,email,updated_at)
                VALUES (?,?,?,?,?,?,?) ON CONFLICT (user_id) DO UPDATE SET recipient_type=EXCLUDED.recipient_type,
                recipient_name=EXCLUDED.recipient_name,tax_code=EXCLUDED.tax_code,address=EXCLUDED.address,
                email=EXCLUDED.email,updated_at=EXCLUDED.updated_at
                """, userId, value.recipientType(), value.recipientName().trim(),
                "BUSINESS".equals(value.recipientType()) ? value.taxCode().trim() : null,
                value.address() == null || value.address().isBlank() ? null : value.address().trim(),
                value.email().trim().toLowerCase(java.util.Locale.ROOT), atUtc(now));
        return findBilling(userId);
    }

    private OffsetDateTime atUtc(Instant value) { return value.atOffset(ZoneOffset.UTC); }
}
