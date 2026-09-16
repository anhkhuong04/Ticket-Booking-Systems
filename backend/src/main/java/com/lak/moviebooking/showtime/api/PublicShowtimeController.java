package com.lak.moviebooking.showtime.api;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.showtime.application.ShowtimeManagement;
import com.lak.moviebooking.showtime.application.ShowtimeAvailabilityView;
import com.lak.moviebooking.showtime.application.ShowtimeSeatMap;
import com.lak.moviebooking.showtime.application.ShowtimeView;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/showtimes")
public class PublicShowtimeController {
    private final ShowtimeManagement showtimeManagement;
    private final Clock clock;
    public PublicShowtimeController(ShowtimeManagement showtimeManagement, Clock clock){this.showtimeManagement=showtimeManagement;this.clock=clock;}
    @GetMapping("/availability") public ShowtimeAvailabilityView availability(@RequestParam @NotNull UUID movieId){return showtimeManagement.findAvailability(movieId,clock.instant());}
    @GetMapping public List<ShowtimeView> showtimes(@RequestParam @NotNull UUID movieId,@RequestParam @NotNull LocalDate date,@RequestParam(required=false) UUID cinemaId){return showtimeManagement.findOpenShowtimes(movieId,date,cinemaId,clock.instant());}
    @GetMapping("/{showtimeId}/seats") public ShowtimeSeatMap seats(@PathVariable UUID showtimeId){return showtimeManagement.findSeatMap(showtimeId,clock.instant());}
}
