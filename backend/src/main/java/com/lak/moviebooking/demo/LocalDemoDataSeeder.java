package com.lak.moviebooking.demo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds local-only, fictional cinema operations around verified CGV movie metadata. */
@Component
@Profile("local")
@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true")
class LocalDemoDataSeeder implements ApplicationRunner {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<CinemaSeed> CINEMAS = List.of(
            new CinemaSeed("district-1", "LAK Cinema Quận 1", "12 Nguyễn Huệ", "Hồ Chí Minh"),
            new CinemaSeed("cau-giay", "LAK Cinema Cầu Giấy", "88 Trần Thái Tông", "Hà Nội"),
            new CinemaSeed("hai-chau", "LAK Cinema Hải Châu", "25 Bạch Đằng", "Đà Nẵng"));
    private static final List<RoomSeed> ROOMS = List.of(
            new RoomSeed("room-1", "Phòng 1", "2D", 15),
            new RoomSeed("room-2", "Phòng 2", "3D", 20),
            new RoomSeed("room-3", "Phòng 3", "IMAX", 25));
    private static final List<LocalTime> START_TIMES = List.of(
            LocalTime.of(8, 30), LocalTime.of(12, 30), LocalTime.of(16, 30), LocalTime.of(20, 30));

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    LocalDemoDataSeeder(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<MovieSeed> movies = loadMovies();
        seedMovies(movies);
        List<AuditoriumSeed> auditoriums = seedCinemasAndAuditoriums();
        seedSystemPrices();
        seedShowtimes(movies.stream().filter(movie -> "NOW_SHOWING".equals(movie.status())).toList(), auditoriums);
    }

    private List<MovieSeed> loadMovies() {
        try (InputStream input = new ClassPathResource("demo/cgv-movies.json").getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim)
                    .filter(line -> line.startsWith("{"))
                    .map(this::parseMovie)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load local CGV movie seed data", exception);
        }
    }

    private MovieSeed parseMovie(String line) {
        return new MovieSeed(string(line, "title"), number(line, "durationMinutes"), string(line, "ageRating"),
                LocalDate.parse(string(line, "releaseDate")), string(line, "status"), strings(line, "genres"));
    }

    private String string(String line, String field) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\":\\\"([^\\\"]*)\\\"").matcher(line);
        if (!matcher.find()) {
            throw new IllegalStateException("Invalid local movie seed field: " + field);
        }
        return matcher.group(1);
    }

    private int number(String line, String field) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\":(\\d+)").matcher(line);
        if (!matcher.find()) {
            throw new IllegalStateException("Invalid local movie seed field: " + field);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private List<String> strings(String line, String field) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\":\\[(.*?)]").matcher(line);
        if (!matcher.find() || matcher.group(1).isBlank()) {
            return List.of();
        }
        return Pattern.compile("\\\"([^\\\"]*)\\\"").matcher(matcher.group(1)).results().map(result -> result.group(1)).toList();
    }

    private void seedMovies(List<MovieSeed> movies) {
        OffsetDateTime now = now();
        for (MovieSeed movie : movies) {
            UUID movieId = id("movie:" + movie.title());
            jdbcTemplate.update("""
                    INSERT INTO movies (id,title,duration_minutes,age_rating,release_date,status,created_at,updated_at)
                    VALUES (?,?,?,?,?,?,?,?) ON CONFLICT (id) DO NOTHING
                    """, movieId, movie.title(), movie.durationMinutes(), movie.ageRating(), movie.releaseDate(), movie.status(), now, now);
            for (String genreName : movie.genres()) {
                String slug = slug(genreName);
                UUID genreId = id("genre:" + slug);
                jdbcTemplate.update("""
                        INSERT INTO genres (id,name,slug,created_at,updated_at)
                        VALUES (?,?,?,?,?) ON CONFLICT (slug) DO NOTHING
                        """, genreId, genreName, slug, now, now);
                UUID persistedGenreId = jdbcTemplate.queryForObject("SELECT id FROM genres WHERE slug=?", UUID.class, slug);
                jdbcTemplate.update("INSERT INTO movie_genres (movie_id,genre_id) VALUES (?,?) ON CONFLICT DO NOTHING", movieId, persistedGenreId);
            }
        }
    }

    private List<AuditoriumSeed> seedCinemasAndAuditoriums() {
        OffsetDateTime now = now();
        List<AuditoriumSeed> auditoriums = new java.util.ArrayList<>();
        for (CinemaSeed cinema : CINEMAS) {
            UUID cinemaId = id("cinema:" + cinema.key());
            jdbcTemplate.update("""
                    INSERT INTO cinemas (id,name,address,city,timezone,status,created_at,updated_at)
                    VALUES (?,?,?,?,?,'ACTIVE',?,?) ON CONFLICT (id) DO NOTHING
                    """, cinemaId, cinema.name(), cinema.address(), cinema.city(), VIETNAM.getId(), now, now);
            for (RoomSeed room : ROOMS) {
                UUID auditoriumId = id("auditorium:" + cinema.key() + ":" + room.key());
                jdbcTemplate.update("""
                        INSERT INTO auditoriums (id,cinema_id,name,screen_format,cleanup_minutes,status,created_at,updated_at)
                        VALUES (?,?,?,?,?,'ACTIVE',?,?) ON CONFLICT (id) DO NOTHING
                        """, auditoriumId, cinemaId, room.name(), room.format(), room.cleanupMinutes(), now, now);
                seedSeats(auditoriumId, now);
                auditoriums.add(new AuditoriumSeed(auditoriumId, room.cleanupMinutes()));
            }
        }
        return List.copyOf(auditoriums);
    }

    private void seedSeats(UUID auditoriumId, OffsetDateTime now) {
        for (char row = 'A'; row <= 'G'; row++) {
            String type = row == 'G' ? "VIP" : "STANDARD";
            for (int number = 1; number <= 10; number++) {
                insertSeat(auditoriumId, String.valueOf(row), number, type, null, now);
            }
        }
        for (int number = 1; number <= 10; number += 2) {
            String pairKey = "H-" + number + "-" + (number + 1);
            insertSeat(auditoriumId, "H", number, "COUPLE", pairKey, now);
            insertSeat(auditoriumId, "H", number + 1, "COUPLE", pairKey, now);
        }
    }

    private void insertSeat(UUID auditoriumId, String row, int number, String type, String pairKey, OffsetDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO seats (id,auditorium_id,row_label,seat_number,seat_type,pair_key,status,created_at,updated_at)
                VALUES (?,?,?,?,?,?,'ACTIVE',?,?) ON CONFLICT (id) DO NOTHING
                """, id("seat:" + auditoriumId + ":" + row + number), auditoriumId, row, number, type, pairKey, now, now);
    }

    private void seedSystemPrices() {
        UUID profileId = id("price-profile:local-system-default");
        OffsetDateTime now = now();
        LocalDate effectiveFrom = LocalDate.now(clock.withZone(VIETNAM)).minusDays(1);
        jdbcTemplate.update("""
                INSERT INTO price_profiles (id,cinema_id,name,effective_from,effective_to,status,created_at,updated_at)
                VALUES (?,NULL,'Local default price',?,NULL,'ACTIVE',?,?) ON CONFLICT (id) DO NOTHING
                """, profileId, effectiveFrom, now, now);
        insertPriceRule(profileId, "STANDARD", 90_000, now);
        insertPriceRule(profileId, "VIP", 120_000, now);
        insertPriceRule(profileId, "COUPLE", 180_000, now);
    }

    private void insertPriceRule(UUID profileId, String seatType, long amount, OffsetDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO price_rules (id,profile_id,day_type,time_from,time_to,screen_format,seat_type,amount,priority,created_at,updated_at)
                VALUES (?,?,'ANY',NULL,NULL,NULL,?,?,0,?,?) ON CONFLICT (id) DO NOTHING
                """, id("price-rule:local-system-default:" + seatType), profileId, seatType, amount, now, now);
    }

    private void seedShowtimes(List<MovieSeed> movies, List<AuditoriumSeed> auditoriums) {
        if (movies.isEmpty()) {
            return;
        }
        LocalDate firstDate = LocalDate.now(clock.withZone(VIETNAM));
        int movieIndex = 0;
        for (int day = 0; day < 14; day++) {
            LocalDate date = firstDate.plusDays(day);
            for (AuditoriumSeed auditorium : auditoriums) {
                for (LocalTime time : START_TIMES) {
                    MovieSeed movie = movies.get(movieIndex++ % movies.size());
                    seedShowtime(movie, auditorium, date, time);
                }
            }
        }
    }

    private void seedShowtime(MovieSeed movie, AuditoriumSeed auditorium, LocalDate date, LocalTime time) {
        UUID movieId = id("movie:" + movie.title());
        UUID showtimeId = id("showtime:" + auditorium.id() + ":" + date + ":" + time);
        Instant startAt = date.atTime(time).atZone(VIETNAM).toInstant();
        Instant endAt = startAt.plusSeconds((long) (movie.durationMinutes() + auditorium.cleanupMinutes()) * 60);
        OffsetDateTime now = now();
        if (Boolean.TRUE.equals(jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM showtimes WHERE id=?)", Boolean.class, showtimeId))) {
            seedShowtimeSnapshot(showtimeId, auditorium.id(), now);
            return;
        }
        boolean overlaps = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM showtimes
                WHERE auditorium_id=? AND status='SCHEDULED'
                  AND tstzrange(start_at,end_at,'[)') && tstzrange(?::timestamptz,?::timestamptz,'[)'))
                """, Boolean.class, auditorium.id(), atUtc(startAt), atUtc(endAt)));
        if (overlaps) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO showtimes (id,movie_id,auditorium_id,start_at,end_at,sales_close_at,status,created_at,updated_at)
                VALUES (?,?,?,?,?,?,'SCHEDULED',?,?)
                """, showtimeId, movieId, auditorium.id(), atUtc(startAt), atUtc(endAt), atUtc(startAt.minusSeconds(300)), now, now);
        seedShowtimeSnapshot(showtimeId, auditorium.id(), now);
    }

    private void seedShowtimeSnapshot(UUID showtimeId, UUID auditoriumId, OffsetDateTime now) {
        for (PriceSeed price : List.of(new PriceSeed("STANDARD", 90_000), new PriceSeed("VIP", 120_000), new PriceSeed("COUPLE", 180_000))) {
            jdbcTemplate.update("""
                    INSERT INTO showtime_prices (id,showtime_id,seat_type,price,source,created_at,updated_at)
                    VALUES (?,?,?,?,'SYSTEM_PROFILE',?,?) ON CONFLICT (showtime_id,seat_type) DO NOTHING
                    """, id("showtime-price:" + showtimeId + ":" + price.seatType()), showtimeId, price.seatType(), price.amount(), now, now);
        }
        jdbcTemplate.update("""
                INSERT INTO showtime_seats (id,showtime_id,seat_id,status,created_at,updated_at)
                SELECT md5(? || ':' || ?::text || ':' || s.id::text)::uuid, ?, s.id,
                       CASE WHEN s.status='LOCKED' THEN 'BLOCKED' ELSE 'AVAILABLE' END, ?, ?
                FROM seats s WHERE s.auditorium_id=?
                ON CONFLICT (showtime_id,seat_id) DO NOTHING
                """, "lak-demo-showtime-seat", showtimeId, showtimeId, now, now, auditoriumId);
    }

    private UUID id(String key) {
        return UUID.nameUUIDFromBytes(("lak-demo:" + key).getBytes(StandardCharsets.UTF_8));
    }

    private String slug(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private OffsetDateTime now() {
        return clock.instant().atOffset(ZoneOffset.UTC);
    }

    private OffsetDateTime atUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private record MovieSeed(String title, int durationMinutes, String ageRating, LocalDate releaseDate, String status, List<String> genres) { }
    private record CinemaSeed(String key, String name, String address, String city) { }
    private record RoomSeed(String key, String name, String format, int cleanupMinutes) { }
    private record AuditoriumSeed(UUID id, int cleanupMinutes) { }
    private record PriceSeed(String seatType, long amount) { }
}
