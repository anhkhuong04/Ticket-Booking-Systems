package com.lak.moviebooking.cinema.api;

import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.authorization.application.CinemaScopeAuthorizer;
import com.lak.moviebooking.cinema.application.AuditoriumView;
import com.lak.moviebooking.cinema.application.AuditoriumWriteCommand;
import com.lak.moviebooking.cinema.application.CinemaAdminView;
import com.lak.moviebooking.cinema.application.CinemaAdministration;
import com.lak.moviebooking.cinema.application.CinemaDetail;
import com.lak.moviebooking.cinema.application.CinemaWriteCommand;
import com.lak.moviebooking.cinema.application.SeatView;
import com.lak.moviebooking.cinema.application.SeatWriteCommand;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin")
public class CinemaAdministrationController {
    private final CinemaAdministration cinemaAdministration;
    private final CinemaScopeAuthorizer cinemaScopeAuthorizer;

    public CinemaAdministrationController(CinemaAdministration cinemaAdministration, CinemaScopeAuthorizer cinemaScopeAuthorizer) {
        this.cinemaAdministration = cinemaAdministration; this.cinemaScopeAuthorizer = cinemaScopeAuthorizer;
    }

    @GetMapping("/cinemas") public List<CinemaAdminView> cinemas(@AuthenticationPrincipal AuthenticatedPrincipal actor) {
        return cinemaAdministration.cinemas(actor.roles().contains("SUPER_ADMIN") ? null : cinemaScopeAuthorizer.accessibleCinemaIds(actor));
    }
    @PostMapping("/cinemas") public CinemaDetail createCinema(@AuthenticationPrincipal AuthenticatedPrincipal actor, @Valid @RequestBody CinemaRequest request) {
        cinemaScopeAuthorizer.requireSuperAdmin(actor, "/api/admin/cinemas"); return cinemaAdministration.createCinema(actor.userId(), request.toCommand());
    }
    @PutMapping("/cinemas/{cinemaId}") public CinemaDetail updateCinema(@AuthenticationPrincipal AuthenticatedPrincipal actor, @PathVariable UUID cinemaId, @Valid @RequestBody CinemaRequest request) {
        cinemaScopeAuthorizer.requireAccess(actor, cinemaId); return cinemaAdministration.updateCinema(actor.userId(), cinemaId, request.toCommand());
    }
    @DeleteMapping("/cinemas/{cinemaId}") public void deactivateCinema(@AuthenticationPrincipal AuthenticatedPrincipal actor, @PathVariable UUID cinemaId) {
        cinemaScopeAuthorizer.requireAccess(actor, cinemaId); cinemaAdministration.deactivateCinema(actor.userId(), cinemaId);
    }
    @GetMapping("/cinemas/{cinemaId}/auditoriums") public List<AuditoriumView> auditoriums(@AuthenticationPrincipal AuthenticatedPrincipal actor, @PathVariable UUID cinemaId) {
        cinemaScopeAuthorizer.requireAccess(actor, cinemaId); return cinemaAdministration.auditoriums(cinemaId);
    }
    @PostMapping("/cinemas/{cinemaId}/auditoriums") public AuditoriumView createAuditorium(@AuthenticationPrincipal AuthenticatedPrincipal actor, @PathVariable UUID cinemaId, @Valid @RequestBody AuditoriumRequest request) {
        cinemaScopeAuthorizer.requireAccess(actor, cinemaId); return cinemaAdministration.createAuditorium(actor.userId(), cinemaId, request.toCommand());
    }
    @PutMapping("/auditoriums/{auditoriumId}") public AuditoriumView updateAuditorium(@AuthenticationPrincipal AuthenticatedPrincipal actor, @PathVariable UUID auditoriumId, @Valid @RequestBody AuditoriumRequest request) {
        AuditoriumView auditorium = cinemaAdministration.auditorium(auditoriumId); cinemaScopeAuthorizer.requireAccess(actor, auditorium.cinemaId()); return cinemaAdministration.updateAuditorium(actor.userId(), auditoriumId, request.toCommand());
    }
    @DeleteMapping("/auditoriums/{auditoriumId}") public void deactivateAuditorium(@AuthenticationPrincipal AuthenticatedPrincipal actor, @PathVariable UUID auditoriumId) {
        AuditoriumView auditorium = cinemaAdministration.auditorium(auditoriumId); cinemaScopeAuthorizer.requireAccess(actor, auditorium.cinemaId()); cinemaAdministration.deactivateAuditorium(actor.userId(), auditoriumId);
    }
    @GetMapping("/auditoriums/{auditoriumId}/seats") public List<SeatView> seats(@AuthenticationPrincipal AuthenticatedPrincipal actor, @PathVariable UUID auditoriumId) {
        AuditoriumView auditorium = cinemaAdministration.auditorium(auditoriumId); cinemaScopeAuthorizer.requireAccess(actor, auditorium.cinemaId()); return cinemaAdministration.seats(auditoriumId);
    }
    @PutMapping("/auditoriums/{auditoriumId}/seats") public List<SeatView> replaceSeats(@AuthenticationPrincipal AuthenticatedPrincipal actor, @PathVariable UUID auditoriumId, @Valid @RequestBody List<@Valid SeatRequest> seats) {
        AuditoriumView auditorium = cinemaAdministration.auditorium(auditoriumId); cinemaScopeAuthorizer.requireAccess(actor, auditorium.cinemaId()); return cinemaAdministration.replaceSeats(actor.userId(), auditoriumId, seats.stream().map(SeatRequest::toCommand).toList());
    }

    public record CinemaRequest(@NotBlank String name, @NotBlank String address, @NotBlank String city, @NotBlank String timezone, @Pattern(regexp="ACTIVE|INACTIVE") String status) { CinemaWriteCommand toCommand(){return new CinemaWriteCommand(name,address,city,timezone,status);} }
    public record AuditoriumRequest(@NotBlank String name, @NotBlank String screenFormat, @Min(0) int cleanupMinutes, @Pattern(regexp="ACTIVE|INACTIVE") String status) { AuditoriumWriteCommand toCommand(){return new AuditoriumWriteCommand(name,screenFormat,cleanupMinutes,status);} }
    public record SeatRequest(@NotBlank String rowLabel, @Min(1) int seatNumber, @Pattern(regexp="STANDARD|VIP|COUPLE") String seatType, String pairKey, @Pattern(regexp="ACTIVE|LOCKED") String status) { SeatWriteCommand toCommand(){return new SeatWriteCommand(rowLabel,seatNumber,seatType,pairKey,status);} }
}
