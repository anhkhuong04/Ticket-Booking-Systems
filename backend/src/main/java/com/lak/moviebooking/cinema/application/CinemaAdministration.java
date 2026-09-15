package com.lak.moviebooking.cinema.application;

import java.util.List;
import java.util.UUID;

public interface CinemaAdministration {
    List<CinemaAdminView> cinemas(java.util.Set<UUID> cinemaIds);
    CinemaDetail createCinema(UUID actorId, CinemaWriteCommand command);
    CinemaDetail updateCinema(UUID actorId, UUID cinemaId, CinemaWriteCommand command);
    void deactivateCinema(UUID actorId, UUID cinemaId);
    List<AuditoriumView> auditoriums(UUID cinemaId);
    AuditoriumView createAuditorium(UUID actorId, UUID cinemaId, AuditoriumWriteCommand command);
    AuditoriumView updateAuditorium(UUID actorId, UUID auditoriumId, AuditoriumWriteCommand command);
    void deactivateAuditorium(UUID actorId, UUID auditoriumId);
    AuditoriumView auditorium(UUID auditoriumId);
    List<SeatView> seats(UUID auditoriumId);
    List<SeatView> replaceSeats(UUID actorId, UUID auditoriumId, List<SeatWriteCommand> seats);
}
