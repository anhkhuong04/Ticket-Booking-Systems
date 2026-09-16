package com.lak.moviebooking.showtime.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShowtimeManagement {
    PriceProfileView createPriceProfile(UUID actorId, PriceProfileCommand command);
    PriceProfileView updatePriceProfile(UUID actorId, UUID profileId, PriceProfileCommand command);
    void deactivatePriceProfile(UUID actorId, UUID profileId);
    PriceRuleView createPriceRule(UUID actorId, UUID profileId, PriceRuleCommand command);
    PriceRuleView updatePriceRule(UUID actorId, UUID ruleId, PriceRuleCommand command);
    void deletePriceRule(UUID actorId, UUID ruleId);
    Optional<UUID> findPriceProfileCinemaId(UUID profileId);
    Optional<UUID> findPriceRuleCinemaId(UUID ruleId);
    List<PriceProfileDetail> findPriceProfiles(UUID cinemaId);
    ShowtimeView createShowtime(UUID actorId, ShowtimeCreateCommand command);
    List<AdminShowtimeView> findAdminShowtimes(UUID cinemaId, LocalDate date);
    UUID findShowtimeCinemaId(UUID showtimeId);
    AdminShowtimeView cancelShowtime(UUID actorId, UUID showtimeId);
    ShowtimeAvailabilityView findAvailability(UUID movieId, Instant now);
    List<ShowtimeView> findOpenShowtimes(UUID movieId, LocalDate date, UUID cinemaId, Instant now);
    ShowtimeSeatMap findSeatMap(UUID showtimeId, Instant now);
}
