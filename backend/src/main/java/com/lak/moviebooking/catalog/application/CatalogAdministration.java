package com.lak.moviebooking.catalog.application;

import java.util.List;
import java.util.UUID;

public interface CatalogAdministration {

    List<MovieDetail> movies();

    MovieDetail createMovie(UUID actorId, MovieWriteCommand command);

    MovieDetail updateMovie(UUID actorId, UUID movieId, MovieWriteCommand command);

    void archiveMovie(UUID actorId, UUID movieId);

    GenreSummary createGenre(UUID actorId, GenreWriteCommand command);

    GenreSummary updateGenre(UUID actorId, UUID genreId, GenreWriteCommand command);

    void deleteGenre(UUID actorId, UUID genreId);
}
