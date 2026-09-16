package com.lak.moviebooking.ticketing.application;

import java.util.UUID;

/** Transactional interface consumed by the verified-payment workflow. */
public interface TicketIssuer {

    TicketIssuance issueForPaidBooking(UUID bookingId);

    void cancelForRefund(UUID bookingId);
}
