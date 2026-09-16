package com.lak.moviebooking.identity.api;

import java.time.LocalDate;

import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import com.lak.moviebooking.identity.application.BillingPreferencesView;
import com.lak.moviebooking.identity.application.CustomerProfile;
import com.lak.moviebooking.identity.application.CustomerProfileView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class CustomerProfileController {
    private final CustomerProfile profile;

    public CustomerProfileController(CustomerProfile profile) { this.profile = profile; }

    @GetMapping("/profile")
    CustomerProfileView find(@AuthenticationPrincipal AuthenticatedPrincipal user) { return profile.find(user.userId()); }

    @PutMapping("/profile")
    CustomerProfileView update(@AuthenticationPrincipal AuthenticatedPrincipal user, @Valid @RequestBody ProfileRequest request) {
        return profile.update(user.userId(), request.fullName(), request.phone(), request.birthDate());
    }

    @GetMapping("/billing-preferences")
    BillingPreferencesView findBilling(@AuthenticationPrincipal AuthenticatedPrincipal user) { return profile.findBilling(user.userId()); }

    @PutMapping("/billing-preferences")
    BillingPreferencesView updateBilling(@AuthenticationPrincipal AuthenticatedPrincipal user, @Valid @RequestBody BillingRequest request) {
        return profile.updateBilling(user.userId(), new BillingPreferencesView(request.recipientType(), request.recipientName(),
                request.taxCode(), request.address(), request.email()));
    }

    record ProfileRequest(@NotBlank @Size(max = 150) String fullName,
                          @Size(max = 32) String phone, LocalDate birthDate) { }

    record BillingRequest(@NotBlank String recipientType, @NotBlank @Size(max = 150) String recipientName,
                          @Size(max = 32) String taxCode, @Size(max = 500) String address,
                          @NotBlank @Email @Size(max = 320) String email) { }
}
