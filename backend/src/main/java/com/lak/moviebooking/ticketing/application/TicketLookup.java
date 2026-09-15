package com.lak.moviebooking.ticketing.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read model used by the ticket API; access fields must never be serialized directly. */
public record TicketLookup(
        UUID bookingOwnerId,
        UUID cinemaId,
        TicketDetailView ticket) {

    public record TicketDetailView(
            UUID id, String ticketCode, String status, Instant issuedAt, Instant usedAt,
            String bookingCode, String bookingStatus, String movieTitle, String posterUrl,
            String cinemaName, String auditoriumName, Instant startAt, List<String> seatLabels) {
        public TicketDetailView {
            seatLabels = List.copyOf(seatLabels);
        }
    }
}
