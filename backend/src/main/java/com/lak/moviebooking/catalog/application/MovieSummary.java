package com.lak.moviebooking.catalog.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MovieSummary(
        UUID id,
        String title,
        int durationMinutes,
        String ageRating,
        LocalDate releaseDate,
        String posterUrl,
        String status,
        List<String> genres) {
    public MovieSummary {
        genres = List.copyOf(genres);
    }
}
