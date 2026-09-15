package com.lak.moviebooking.cinema.application;

import java.util.UUID;

public record AuditoriumForShowtime(UUID id, UUID cinemaId, String cinemaName, String cinemaAddress, String screenFormat, int cleanupMinutes) { }
