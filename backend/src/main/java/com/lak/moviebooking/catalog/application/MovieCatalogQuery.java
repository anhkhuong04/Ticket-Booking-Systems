package com.lak.moviebooking.catalog.application;

import java.util.List;
import java.util.UUID;

public interface MovieCatalogQuery {

    CatalogPage<MovieSummary> findMovies(MovieSearchCriteria criteria);

    MovieDetail findMovie(UUID movieId);

    List<GenreSummary> findGenres();
}
