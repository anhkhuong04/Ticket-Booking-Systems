package com.lak.moviebooking.ticketing.application;

import java.util.UUID;

public interface TicketEmailResendRateLimit {

    void check(UUID ownerId, UUID ticketId);
}
