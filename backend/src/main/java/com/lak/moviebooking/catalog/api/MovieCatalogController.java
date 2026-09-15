package com.lak.moviebooking.catalog.api;

import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.catalog.application.GenreSummary;
import com.lak.moviebooking.catalog.application.CatalogPage;
import com.lak.moviebooking.catalog.application.MovieCatalogQuery;
import com.lak.moviebooking.catalog.application.MovieDetail;
import com.lak.moviebooking.catalog.application.MovieSearchCriteria;
import com.lak.moviebooking.catalog.application.MovieSummary;
import com.lak.moviebooking.common.api.pagination.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api")
public class MovieCatalogController {

    private final MovieCatalogQuery movieCatalogQuery;

    public MovieCatalogController(MovieCatalogQuery movieCatalogQuery) {
        this.movieCatalogQuery = movieCatalogQuery;
    }

    @GetMapping("/movies")
    public PageResponse<MovieSummary> movies(
            @RequestParam(required = false) @Pattern(regexp = "(?i)NOW_SHOWING|COMING_SOON") String status,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String genre,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        String normalizedStatus = status == null ? null : status.toUpperCase();
        CatalogPage<MovieSummary> result = movieCatalogQuery.findMovies(
                new MovieSearchCriteria(normalizedStatus, normalize(query), genre, page, size));
        return new PageResponse<>(result.content(), result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/movies/{movieId}")
    public MovieDetail movie(@PathVariable UUID movieId) {
        return movieCatalogQuery.findMovie(movieId);
    }

    @GetMapping("/genres")
    public List<GenreSummary> genres() {
        return movieCatalogQuery.findGenres();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
