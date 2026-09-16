package com.lak.moviebooking.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.support.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class CustomerProfileIT extends AbstractIntegrationTest {
    @Autowired private IdentityAuthenticationService authentication;
    @Autowired private CustomerProfile profile;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void savesOnlyOwnerProfileAndBillingPreferences() {
        var owner = authentication.register("Profile Customer", "profile@example.com", "a-secure-password").session().user().id();
        var other = authentication.register("Other Customer", "other@example.com", "a-secure-password").session().user().id();
        var updated = profile.update(owner, "Updated Name", "0912345678", LocalDate.of(2005, 2, 1));
        assertThat(updated.fullName()).isEqualTo("Updated Name");
        assertThat(updated.birthDate()).isEqualTo(LocalDate.of(2005, 2, 1));
        assertThat(profile.find(other).birthDate()).isNull();

        var billing = new BillingPreferencesView("PERSONAL", "Invoice Recipient", null, null, "invoice@example.com");
        assertThat(profile.updateBilling(owner, billing)).isEqualTo(billing);
        assertThat(profile.findBilling(other)).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE actor_id=? AND action='CUSTOMER_PROFILE_UPDATED'", Integer.class, owner)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE actor_id=? AND action='CUSTOMER_BILLING_UPDATED'", Integer.class, owner)).isEqualTo(1);
        assertThatThrownBy(() -> profile.update(owner, "Name", null, LocalDate.of(2099, 1, 1)))
                .isInstanceOf(ApplicationException.class);
    }
}
