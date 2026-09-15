package com.lak.moviebooking.ticketing.application;

import java.util.Optional;

/** The raw QR token exists only for a newly issued ticket and must never be persisted or logged. */
public record TicketIssuance(TicketView ticket, Optional<String> rawQrToken) {

    public TicketIssuance {
        rawQrToken = rawQrToken == null ? Optional.empty() : rawQrToken;
    }
}
