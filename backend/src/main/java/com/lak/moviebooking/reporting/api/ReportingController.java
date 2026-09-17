package com.lak.moviebooking.reporting.api;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.lak.moviebooking.authorization.application.CinemaScopeAuthorizer;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import com.lak.moviebooking.reporting.application.ReportSummary;
import com.lak.moviebooking.reporting.application.ReportingQuery;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class ReportingController {
    private final ReportingQuery reports;
    private final CinemaScopeAuthorizer scope;

    public ReportingController(ReportingQuery reports, CinemaScopeAuthorizer scope) {
        this.reports = reports;
        this.scope = scope;
    }

    @GetMapping("/reports/summary")
    public ReportSummary summary(@AuthenticationPrincipal AuthenticatedPrincipal actor,
                                 @RequestParam LocalDate from, @RequestParam LocalDate to,
                                 @RequestParam(required = false) UUID cinemaId) {
        if (to.isBefore(from) || to.isAfter(from.plusDays(365))) {
            throw new IllegalArgumentException("Invalid report date range");
        }
        if (cinemaId != null) scope.requireAccess(actor, cinemaId);
        boolean allCinemas = actor.roles().contains("SUPER_ADMIN");
        Set<UUID> cinemaScope = scope.accessibleCinemaIds(actor);
        return reports.summary(from, to, cinemaId, cinemaScope, allCinemas);
    }
}
