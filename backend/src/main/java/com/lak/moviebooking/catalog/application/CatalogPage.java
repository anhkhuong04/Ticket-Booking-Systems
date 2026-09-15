package com.lak.moviebooking.catalog.application;

import java.util.List;

public record CatalogPage<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public CatalogPage {
        content = List.copyOf(content);
    }
}
