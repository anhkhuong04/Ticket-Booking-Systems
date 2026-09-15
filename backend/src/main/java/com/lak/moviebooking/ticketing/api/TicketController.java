package com.lak.moviebooking.ticketing.api;

import java.time.Instant;
import java.util.List;

import com.lak.moviebooking.audit.application.DeniedAccessAudit;
import com.lak.moviebooking.authorization.application.CinemaScopeAuthorizer;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import com.lak.moviebooking.ticketing.application.TicketBookingSummary;
import com.lak.moviebooking.ticketing.application.TicketLookup;
import com.lak.moviebooking.ticketing.application.TicketQuery;
import com.lak.moviebooking.ticketing.application.TicketQrPayloadFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TicketController {

    private final TicketQuery tickets;
    private final TicketQrPayloadFactory qrPayloadFactory;
    private final CinemaScopeAuthorizer cinemaScopeAuthorizer;
    private final DeniedAccessAudit deniedAccessAudit;

    TicketController(TicketQuery tickets, TicketQrPayloadFactory qrPayloadFactory,
            CinemaScopeAuthorizer cinemaScopeAuthorizer, DeniedAccessAudit deniedAccessAudit) {
        this.tickets = tickets;
        this.qrPayloadFactory = qrPayloadFactory;
        this.cinemaScopeAuthorizer = cinemaScopeAuthorizer;
        this.deniedAccessAudit = deniedAccessAudit;
    }

    @GetMapping("/api/me/bookings")
    List<TicketBookingSummary> myBookings(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return tickets.findBookingsForOwner(principal.userId());
    }

    @GetMapping("/api/tickets/{ticketCode}")
    TicketResponse ticket(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable String ticketCode) {
        TicketLookup lookup = tickets.findByCode(ticketCode);
        boolean owner = lookup.bookingOwnerId().equals(principal.userId());
        if (!owner) requireStaffScope(principal, lookup.cinemaId());
        TicketLookup.TicketDetailView ticket = lookup.ticket();
        return new TicketResponse(ticket.id(), ticket.ticketCode(), ticket.status(), ticket.issuedAt(), ticket.usedAt(), ticket.bookingCode(),
                ticket.bookingStatus(), ticket.movieTitle(), ticket.posterUrl(), ticket.cinemaName(), ticket.auditoriumName(), ticket.startAt(),
                ticket.seatLabels(), owner ? qrPayloadFactory.create(ticket.ticketCode()) : null);
    }

    private void requireStaffScope(AuthenticatedPrincipal principal, java.util.UUID cinemaId) {
        if (!principal.roles().contains("SUPER_ADMIN") && !principal.roles().contains("CINEMA_MANAGER")
                && !principal.roles().contains("TICKET_STAFF")) {
            deniedAccessAudit.record(principal.userId(), "/api/tickets", "TICKET_OWNER_OR_CINEMA_SCOPE");
            throw ApplicationException.forbidden("TICKET_FORBIDDEN", "You do not have access to this ticket");
        }
        cinemaScopeAuthorizer.requireAccess(principal, cinemaId);
    }

    record TicketResponse(
            java.util.UUID id, String ticketCode, String status, Instant issuedAt, Instant usedAt, String bookingCode,
            String bookingStatus, String movieTitle, String posterUrl, String cinemaName, String auditoriumName,
            Instant startAt, List<String> seatLabels, String qrPayload) {
        TicketResponse {
            seatLabels = List.copyOf(seatLabels);
        }
    }
}
