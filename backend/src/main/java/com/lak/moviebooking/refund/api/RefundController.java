package com.lak.moviebooking.refund.api;

import java.util.UUID;

import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import com.lak.moviebooking.refund.application.CustomerRefundCommand;
import com.lak.moviebooking.refund.application.RefundManagement;
import com.lak.moviebooking.refund.application.RefundView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
public class RefundController {

    private final RefundManagement refunds;

    public RefundController(RefundManagement refunds) {
        this.refunds = refunds;
    }

    @PostMapping("/bookings/{bookingId}/refunds")
    ResponseEntity<RefundView> request(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID bookingId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey) {
        RefundView refund = refunds.requestCustomerRefund(new CustomerRefundCommand(
                principal.userId(), bookingId, idempotencyKey.trim()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(refund);
    }

    @GetMapping("/refunds/{refundId}")
    RefundView find(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID refundId) {
        return refunds.findCustomerRefund(principal.userId(), refundId);
    }
}
