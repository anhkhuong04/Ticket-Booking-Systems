package com.lak.moviebooking.ticketing.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Customer-safe booking history summary. QR data intentionally never appears in this list. */
public record TicketBookingSummary(
        UUID bookingId, String bookingCode, String bookingStatus, String movieTitle, String posterUrl,
        String cinemaName, String auditoriumName, Instant startAt, List<String> seatLabels,
        String ticketCode, String ticketStatus, Instant createdAt, String paymentStatus, String refundStatus,
        String showtimeStatus, boolean canResumePayment) {
    public TicketBookingSummary {
        seatLabels = List.copyOf(seatLabels);
    }
}
