package com.lak.moviebooking.showtime.application;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
public record ShowtimeSeatMap(UUID showtimeId, UUID movieId, String movieTitle, UUID cinemaId, String cinemaName, String auditoriumName, Instant startAt, List<ShowtimeSeatView> seats, Map<String, Long> prices) { public ShowtimeSeatMap { seats = List.copyOf(seats); prices = Map.copyOf(prices); } }
