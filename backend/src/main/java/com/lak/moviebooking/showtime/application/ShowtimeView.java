package com.lak.moviebooking.showtime.application;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
public record ShowtimeView(UUID id, UUID movieId, UUID cinemaId, String cinemaName, String cinemaAddress, String screenFormat, Instant startAt, Instant endAt, Instant salesCloseAt, Map<String, Long> prices) { public ShowtimeView { prices = Map.copyOf(prices); } }
