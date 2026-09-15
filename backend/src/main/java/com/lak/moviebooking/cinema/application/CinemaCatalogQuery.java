package com.lak.moviebooking.cinema.application;

import java.util.List;
import java.util.UUID;

public interface CinemaCatalogQuery {

    List<CinemaSummary> findActiveCinemas(String city);

    CinemaDetail findCinema(UUID cinemaId);
}
