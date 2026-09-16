package com.lak.moviebooking.booking.api;

import com.lak.moviebooking.booking.application.BookingBilling;
import com.lak.moviebooking.booking.application.BookingBillingRequest;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings/{bookingCode}/billing")
public class BookingBillingController {
    private final BookingBilling billing;
    public BookingBillingController(BookingBilling billing) { this.billing = billing; }

    @GetMapping
    BookingBillingRequest find(@AuthenticationPrincipal AuthenticatedPrincipal user, @PathVariable String bookingCode) {
        return billing.find(user.userId(), bookingCode);
    }

    @PutMapping
    BookingBillingRequest request(@AuthenticationPrincipal AuthenticatedPrincipal user, @PathVariable String bookingCode,
                                  @Valid @RequestBody Request value) {
        return billing.request(user.userId(), bookingCode, new BookingBillingRequest(value.recipientType(), value.recipientName(),
                value.taxCode(), value.address(), value.email()));
    }

    record Request(@NotBlank String recipientType, @NotBlank @Size(max = 150) String recipientName,
                   @Size(max = 32) String taxCode, @Size(max = 500) String address,
                   @NotBlank @Email @Size(max = 320) String email) { }
}
