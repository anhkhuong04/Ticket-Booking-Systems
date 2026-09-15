package com.lak.moviebooking.cinema.application;
import java.util.UUID;
public record AuditoriumView(UUID id, UUID cinemaId, String name, String screenFormat, int cleanupMinutes, String status) { }
