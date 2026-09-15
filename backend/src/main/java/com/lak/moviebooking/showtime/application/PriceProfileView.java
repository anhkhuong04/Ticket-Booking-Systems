package com.lak.moviebooking.showtime.application;
import java.time.LocalDate;
import java.util.UUID;
public record PriceProfileView(UUID id, UUID cinemaId, String name, LocalDate effectiveFrom, LocalDate effectiveTo, String status) { }
