package com.lak.moviebooking.showtime.application;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
public record ShowtimeCreateCommand(UUID movieId, UUID auditoriumId, Instant startAt, Map<String, Long> priceOverrides) { public ShowtimeCreateCommand { priceOverrides = Map.copyOf(priceOverrides); } }
