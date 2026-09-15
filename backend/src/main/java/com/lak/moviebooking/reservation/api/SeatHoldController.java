package com.lak.moviebooking.reservation.api;

import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import com.lak.moviebooking.reservation.application.SeatHoldCommand;
import com.lak.moviebooking.reservation.application.SeatHoldManagement;
import com.lak.moviebooking.reservation.application.SeatHoldView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/seat-holds")
public class SeatHoldController {

    private final SeatHoldManagement seatHoldManagement;

    public SeatHoldController(SeatHoldManagement seatHoldManagement) {
        this.seatHoldManagement = seatHoldManagement;
    }

    @PostMapping
    ResponseEntity<SeatHoldView> create(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
            @Valid @RequestBody CreateSeatHoldRequest request) {
        SeatHoldView hold = seatHoldManagement.create(principal.userId(), new SeatHoldCommand(
                request.showtimeId(), request.showtimeSeatIds(), idempotencyKey.trim()));
        return ResponseEntity.status(HttpStatus.CREATED).body(hold);
    }

    @GetMapping("/{holdId}")
    SeatHoldView find(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID holdId) {
        return seatHoldManagement.find(principal.userId(), holdId);
    }

    @DeleteMapping("/{holdId}")
    ResponseEntity<Void> release(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable UUID holdId) {
        seatHoldManagement.release(principal.userId(), holdId);
        return ResponseEntity.noContent().build();
    }

    record CreateSeatHoldRequest(
            @NotNull UUID showtimeId,
            @NotEmpty @Size(max = 12) List<@NotNull UUID> showtimeSeatIds) {
    }
}
