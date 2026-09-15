package com.lak.moviebooking.payment.api;

import java.util.UUID;

import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import com.lak.moviebooking.payment.application.PaymentCreateCommand;
import com.lak.moviebooking.payment.application.PaymentManagement;
import com.lak.moviebooking.payment.application.PaymentView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentManagement payments;

    public PaymentController(PaymentManagement payments) {
        this.payments = payments;
    }

    @PostMapping
    ResponseEntity<PaymentView> create(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payments.create(principal.userId(),
                new PaymentCreateCommand(request.bookingCode(), request.provider())));
    }

    @GetMapping("/{paymentId}/status")
    PaymentView find(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID paymentId) {
        return payments.find(principal.userId(), paymentId);
    }

    @PostMapping("/{provider}/webhook")
    ResponseEntity<Void> webhook(
            @PathVariable String provider,
            @RequestHeader("X-Payment-Signature") @NotBlank @Size(max = 256) String signature,
            @RequestBody String rawPayload) {
        payments.receiveWebhook(provider, rawPayload, signature);
        return ResponseEntity.noContent().build();
    }

    record CreatePaymentRequest(@NotBlank @Size(max = 48) String bookingCode, @NotBlank @Size(max = 32) String provider) {
    }
}
