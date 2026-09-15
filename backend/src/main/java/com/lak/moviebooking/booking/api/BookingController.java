package com.lak.moviebooking.booking.api;

import java.util.UUID;

import com.lak.moviebooking.booking.application.BookingCheckout;
import com.lak.moviebooking.booking.application.BookingCheckoutCommand;
import com.lak.moviebooking.booking.application.BookingView;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingCheckout bookingCheckout;

    public BookingController(BookingCheckout bookingCheckout) {
        this.bookingCheckout = bookingCheckout;
    }

    @PostMapping("/checkout")
    ResponseEntity<BookingView> checkout(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request) {
        BookingView booking = bookingCheckout.checkout(principal.userId(), new BookingCheckoutCommand(
                request.holdId(), idempotencyKey.trim(), request.voucherCode()));
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @GetMapping("/{bookingCode}")
    BookingView find(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable String bookingCode) {
        return bookingCheckout.findByCode(principal.userId(), bookingCode);
    }

    record CheckoutRequest(@NotNull UUID holdId, @Size(max = 64) String voucherCode) {
    }
}
