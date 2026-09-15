package com.lak.moviebooking.ticketing.application;

import java.time.Instant;
import java.util.UUID;

public record TicketView(
        UUID id,
        UUID bookingId,
        String ticketCode,
        String status,
        Instant issuedAt,
        Instant usedAt) {
}
