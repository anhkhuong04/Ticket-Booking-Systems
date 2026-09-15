package com.lak.moviebooking.cinema.api;

import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.authorization.application.CinemaScopeAuthorizer;
import com.lak.moviebooking.cinema.application.CinemaCatalogQuery;
import com.lak.moviebooking.cinema.application.CinemaDetail;
import com.lak.moviebooking.cinema.application.CinemaSummary;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CinemaController {

    private final CinemaCatalogQuery cinemaCatalogQuery;
    private final CinemaScopeAuthorizer cinemaScopeAuthorizer;

    public CinemaController(CinemaCatalogQuery cinemaCatalogQuery, CinemaScopeAuthorizer cinemaScopeAuthorizer) {
        this.cinemaCatalogQuery = cinemaCatalogQuery;
        this.cinemaScopeAuthorizer = cinemaScopeAuthorizer;
    }

    @GetMapping("/cinemas")
    public List<CinemaSummary> cinemas(@RequestParam(required = false) String city) {
        return cinemaCatalogQuery.findActiveCinemas(city);
    }

    @GetMapping("/admin/cinemas/{cinemaId}")
    public CinemaDetail managedCinema(
            @PathVariable UUID cinemaId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        cinemaScopeAuthorizer.requireAccess(principal, cinemaId);
        return cinemaCatalogQuery.findCinema(cinemaId);
    }

    @GetMapping("/staff/cinemas/{cinemaId}")
    public CinemaDetail staffedCinema(
            @PathVariable UUID cinemaId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        cinemaScopeAuthorizer.requireAccess(principal, cinemaId);
        return cinemaCatalogQuery.findCinema(cinemaId);
    }
}
