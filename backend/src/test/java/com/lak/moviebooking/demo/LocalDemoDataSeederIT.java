package com.lak.moviebooking.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import com.lak.moviebooking.support.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class LocalDemoDataSeederIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Test
    @Transactional
    void importsVerifiedMoviesAndOperationalDataIdempotently() {
        LocalDemoDataSeeder seeder = new LocalDemoDataSeeder(jdbcTemplate, clock);
        seeder.run(new DefaultApplicationArguments(new String[0]));
        SeedCounts afterFirstRun = counts();
        seeder.run(new DefaultApplicationArguments(new String[0]));

        assertThat(counts()).isEqualTo(afterFirstRun);
        assertThat(afterFirstRun).satisfies(seedCounts -> {
            assertThat(seedCounts.movies()).isGreaterThanOrEqualTo(35);
            assertThat(seedCounts.cinemas()).isGreaterThanOrEqualTo(3);
            assertThat(seedCounts.auditoriums()).isGreaterThanOrEqualTo(9);
            assertThat(seedCounts.seats()).isGreaterThanOrEqualTo(720);
            assertThat(seedCounts.priceProfiles()).isGreaterThanOrEqualTo(1);
            assertThat(seedCounts.priceRules()).isGreaterThanOrEqualTo(3);
            assertThat(seedCounts.showtimes()).isGreaterThanOrEqualTo(504);
            assertThat(seedCounts.showtimePrices()).isGreaterThanOrEqualTo(1_512);
            assertThat(seedCounts.showtimeSeats()).isGreaterThanOrEqualTo(40_320);
        });
    }

    private SeedCounts counts() {
        return new SeedCounts(count("movies"), count("cinemas"), count("auditoriums"), count("seats"),
                count("price_profiles"), count("price_rules"), count("showtimes"), count("showtime_prices"),
                count("showtime_seats"));
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private record SeedCounts(int movies, int cinemas, int auditoriums, int seats, int priceProfiles, int priceRules,
                              int showtimes, int showtimePrices, int showtimeSeats) { }
}
