package com.lak.moviebooking.catalog.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MovieDetail(
        UUID id,
        String title,
        String description,
        int durationMinutes,
        String ageRating,
        LocalDate releaseDate,
        String posterUrl,
        String trailerUrl,
        String status,
        List<String> genres) {
    public MovieDetail {
        genres = List.copyOf(genres);
    }
}
