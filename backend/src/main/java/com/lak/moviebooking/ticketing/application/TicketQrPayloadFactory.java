package com.lak.moviebooking.ticketing.application;

/**
 * Derives the opaque QR payload for a ticket without persisting the raw value.
 */
public interface TicketQrPayloadFactory {

    String create(String ticketCode);
}
