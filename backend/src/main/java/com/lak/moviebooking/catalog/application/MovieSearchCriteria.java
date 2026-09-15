package com.lak.moviebooking.catalog.application;

public record MovieSearchCriteria(
        String status,
        String query,
        String genre,
        int page,
        int size) {
}
