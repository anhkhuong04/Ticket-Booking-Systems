package com.lak.moviebooking.catalog.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.authorization.application.CinemaScopeAuthorizer;
import com.lak.moviebooking.catalog.application.CatalogAdministration;
import com.lak.moviebooking.catalog.application.GenreSummary;
import com.lak.moviebooking.catalog.application.GenreWriteCommand;
import com.lak.moviebooking.catalog.application.MediaUploadRequest;
import com.lak.moviebooking.catalog.application.MediaUploadSignature;
import com.lak.moviebooking.catalog.application.MediaUploadSignatureIssuer;
import com.lak.moviebooking.catalog.application.MovieDetail;
import com.lak.moviebooking.catalog.application.MovieWriteCommand;
import com.lak.moviebooking.common.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin")
public class CatalogAdministrationController {

    private final CatalogAdministration catalogAdministration;
    private final MediaUploadSignatureIssuer mediaUploadSignatureIssuer;
    private final CinemaScopeAuthorizer cinemaScopeAuthorizer;

    public CatalogAdministrationController(CatalogAdministration catalogAdministration,
            MediaUploadSignatureIssuer mediaUploadSignatureIssuer, CinemaScopeAuthorizer cinemaScopeAuthorizer) {
        this.catalogAdministration = catalogAdministration;
        this.mediaUploadSignatureIssuer = mediaUploadSignatureIssuer;
        this.cinemaScopeAuthorizer = cinemaScopeAuthorizer;
    }

    @GetMapping("/movies")
    public List<MovieDetail> movies(@AuthenticationPrincipal AuthenticatedPrincipal actor) {
        cinemaScopeAuthorizer.requireSuperAdmin(actor, "/api/admin/movies");
        return catalogAdministration.movies();
    }

    @PostMapping("/movies")
    public MovieDetail createMovie(@AuthenticationPrincipal AuthenticatedPrincipal actor, @Valid @RequestBody MovieRequest request) {
        cinemaScopeAuthorizer.requireSuperAdmin(actor, "/api/admin/movies");
        return catalogAdministration.createMovie(actor.userId(), request.toCommand());
    }

    @PutMapping("/movies/{movieId}")
    public MovieDetail updateMovie(@AuthenticationPrincipal AuthenticatedPrincipal actor,
            @PathVariable UUID movieId, @Valid @RequestBody MovieRequest request) {
        cinemaScopeAuthorizer.requireSuperAdmin(actor, "/api/admin/movies/" + movieId);
        return catalogAdministration.updateMovie(actor.userId(), movieId, request.toCommand());
    }

    @DeleteMapping("/movies/{movieId}")
    public void archiveMovie(@AuthenticationPrincipal AuthenticatedPrincipal actor, @PathVariable UUID movieId) {
        cinemaScopeAuthorizer.requireSuperAdmin(actor, "/api/admin/movies/" + movieId);
        catalogAdministration.archiveMovie(actor.userId(), movieId);
    }

    @PostMapping("/genres")
    public GenreSummary createGenre(@AuthenticationPrincipal AuthenticatedPrincipal actor, @Valid @RequestBody GenreRequest request) {
        cinemaScopeAuthorizer.requireSuperAdmin(actor, "/api/admin/genres");
        return catalogAdministration.createGenre(actor.userId(), request.toCommand());
    }

    @PutMapping("/genres/{genreId}")
    public GenreSummary updateGenre(@AuthenticationPrincipal AuthenticatedPrincipal actor, @PathVariable UUID genreId,
            @Valid @RequestBody GenreRequest request) {
        cinemaScopeAuthorizer.requireSuperAdmin(actor, "/api/admin/genres/" + genreId);
        return catalogAdministration.updateGenre(actor.userId(), genreId, request.toCommand());
    }

    @DeleteMapping("/genres/{genreId}")
    public void deleteGenre(@AuthenticationPrincipal AuthenticatedPrincipal actor, @PathVariable UUID genreId) {
        cinemaScopeAuthorizer.requireSuperAdmin(actor, "/api/admin/genres/" + genreId);
        catalogAdministration.deleteGenre(actor.userId(), genreId);
    }

    @PostMapping("/media/signatures")
    public MediaUploadSignature signMedia(@AuthenticationPrincipal AuthenticatedPrincipal actor,
            @Valid @RequestBody MediaSignatureRequest request) {
        cinemaScopeAuthorizer.requireSuperAdmin(actor, "/api/admin/media/signatures");
        return mediaUploadSignatureIssuer.issue(actor.userId(), new MediaUploadRequest(request.filename(), request.contentType(), request.sizeBytes()));
    }

    public record MovieRequest(@NotBlank String title, String description, @Min(1) int durationMinutes, @NotBlank String ageRating,
            LocalDate releaseDate, String posterUrl, String trailerUrl,
            @Pattern(regexp = "NOW_SHOWING|COMING_SOON|ARCHIVED") String status, List<UUID> genreIds) {
        MovieWriteCommand toCommand() { return new MovieWriteCommand(title, description, durationMinutes, ageRating, releaseDate, posterUrl, trailerUrl, status, genreIds == null ? List.of() : genreIds); }
    }

    public record GenreRequest(@NotBlank String name, @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug) {
        GenreWriteCommand toCommand() { return new GenreWriteCommand(name, slug); }
    }

    public record MediaSignatureRequest(@NotBlank String filename, @NotBlank String contentType, @Min(1) @Max(5_242_880) long sizeBytes) {
    }
}
