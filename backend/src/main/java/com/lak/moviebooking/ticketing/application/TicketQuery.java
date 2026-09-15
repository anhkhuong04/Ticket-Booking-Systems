package com.lak.moviebooking.ticketing.application;

import java.util.List;
import java.util.UUID;

public interface TicketQuery {

    TicketLookup findByCode(String ticketCode);

    List<TicketBookingSummary> findBookingsForOwner(UUID userId);
}
