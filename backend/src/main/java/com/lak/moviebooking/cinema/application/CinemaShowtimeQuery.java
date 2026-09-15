package com.lak.moviebooking.cinema.application;

import java.util.List;
import java.util.UUID;

public interface CinemaShowtimeQuery {
    AuditoriumForShowtime findAuditorium(UUID auditoriumId);
    List<SeatForShowtime> findSeats(UUID auditoriumId);
}
