package com.lak.moviebooking.catalog.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MovieWriteCommand(
        String title, String description, int durationMinutes, String ageRating, LocalDate releaseDate,
        String posterUrl, String trailerUrl, String status, List<UUID> genreIds,
        String country, String director, List<String> castMembers) {
    public MovieWriteCommand {
        genreIds = List.copyOf(genreIds);
        castMembers = List.copyOf(castMembers);
    }
}
