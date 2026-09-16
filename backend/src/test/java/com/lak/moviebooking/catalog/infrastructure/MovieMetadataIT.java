package com.lak.moviebooking.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.catalog.application.CatalogAdministration;
import com.lak.moviebooking.catalog.application.MovieCatalogQuery;
import com.lak.moviebooking.catalog.application.MovieWriteCommand;
import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.support.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MovieMetadataIT extends AbstractIntegrationTest {

    @Autowired private CatalogAdministration administration;
    @Autowired private MovieCatalogQuery query;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void createsAndUpdatesCreditsWithoutLosingOrder() {
        UUID actor = UUID.randomUUID();
        var created = administration.createMovie(actor, command("Film", List.of("First Actor", "Second Actor")));
        assertThat(query.findMovie(created.id()).country()).isEqualTo("Việt Nam");
        assertThat(query.findMovie(created.id()).director()).isEqualTo("Director");
        assertThat(query.findMovie(created.id()).castMembers()).containsExactly("First Actor", "Second Actor");

        administration.updateMovie(actor, created.id(), command("Film edited", List.of("Third Actor")));
        assertThat(query.findMovie(created.id()).castMembers()).containsExactly("Third Actor");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM movie_cast_members WHERE movie_id=?", Integer.class, created.id())).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT action FROM audit_logs WHERE entity_id=? ORDER BY created_at", String.class, created.id()))
                .contains("MOVIE_CREATED", "MOVIE_UPDATED");
    }

    @Test
    void refusesArchiveWhileSalesAreOpenThenHidesMovie() {
        UUID actor = UUID.randomUUID();
        UUID id = administration.createMovie(actor, command("Archive candidate", List.of())).id();
        UUID cinema = UUID.randomUUID();
        UUID auditorium = UUID.randomUUID();
        OffsetDateTime now = Instant.now().atOffset(ZoneOffset.UTC);
        jdbc.update("INSERT INTO cinemas (id,name,address,city,timezone,status,created_at,updated_at) VALUES (?,?,?,?,'Asia/Ho_Chi_Minh','ACTIVE',?,?)",
                cinema, "Test cinema", "Test address", "HCM", now, now);
        jdbc.update("INSERT INTO auditoriums (id,cinema_id,name,screen_format,cleanup_minutes,status,created_at,updated_at) VALUES (?,?,?,'2D',15,'ACTIVE',?,?)",
                auditorium, cinema, "Room", now, now);
        UUID showtime = UUID.randomUUID();
        jdbc.update("INSERT INTO showtimes (id,movie_id,auditorium_id,start_at,end_at,sales_close_at,status,created_at,updated_at) VALUES (?,?,?,?,?,?,'SCHEDULED',?,?)",
                showtime, id, auditorium, now.plusDays(1), now.plusDays(1).plusHours(2), now.plusDays(1).minusMinutes(5), now, now);

        assertThatThrownBy(() -> administration.archiveMovie(actor, id))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).code()).isEqualTo("MOVIE_HAS_OPEN_SHOWTIMES");
        jdbc.update("UPDATE showtimes SET status='CANCELLED' WHERE id=?", showtime);
        administration.archiveMovie(actor, id);
        assertThatThrownBy(() -> query.findMovie(id)).isInstanceOf(ApplicationException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM movies WHERE id=?", String.class, id)).isEqualTo("ARCHIVED");
    }

    private MovieWriteCommand command(String title, List<String> cast) {
        return new MovieWriteCommand(title, "Description", 120, "T13", LocalDate.now(), null, null,
                "NOW_SHOWING", List.of(), "Việt Nam", "Director", cast);
    }
}
