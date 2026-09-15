package com.lak.moviebooking.ticketing.application;

import java.util.UUID;

/** Queues a ticket email in the caller's transaction; it never sends email directly. */
public interface TicketEmailDelivery {

    void enqueueIssuedTicket(UUID ticketId);

    void resend(UUID ownerId, UUID ticketId);
}
