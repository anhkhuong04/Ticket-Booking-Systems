package com.lak.moviebooking.showtime.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShowtimeManagement {
    PriceProfileView createPriceProfile(UUID actorId, PriceProfileCommand command);
    PriceRuleView createPriceRule(UUID actorId, UUID profileId, PriceRuleCommand command);
    Optional<UUID> findPriceProfileCinemaId(UUID profileId);
    ShowtimeView createShowtime(UUID actorId, ShowtimeCreateCommand command);
    List<ShowtimeView> findOpenShowtimes(UUID movieId, LocalDate date, UUID cinemaId, Instant now);
    ShowtimeSeatMap findSeatMap(UUID showtimeId, Instant now);
}
