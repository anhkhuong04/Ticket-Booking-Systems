package com.lak.moviebooking.ticketing.application;

import com.lak.moviebooking.common.security.AuthenticatedPrincipal;

public interface TicketValidator {

    TicketValidation validate(AuthenticatedPrincipal scanner, String scannedValue);
}
