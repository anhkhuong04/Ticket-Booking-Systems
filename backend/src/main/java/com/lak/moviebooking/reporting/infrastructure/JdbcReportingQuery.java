package com.lak.moviebooking.reporting.infrastructure;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.lak.moviebooking.reporting.application.ReportSummary;
import com.lak.moviebooking.reporting.application.ReportingQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
class JdbcReportingQuery implements ReportingQuery {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String SCOPE = "(? OR c.id=ANY(CAST(? AS uuid[]))) AND (CAST(? AS uuid) IS NULL OR c.id=?)";
    private static final String EVENTS = """
            WITH events AS (
                SELECT p.paid_at AS event_at, st.movie_id, p.amount AS revenue, 1::bigint AS bookings,
                       (SELECT count(*) FROM booking_items item WHERE item.booking_id=b.id) AS seats
                FROM payments p
                JOIN bookings b ON b.id=p.booking_id
                JOIN showtimes st ON st.id=b.showtime_id
                JOIN auditoriums a ON a.id=st.auditorium_id
                JOIN cinemas c ON c.id=a.cinema_id
                WHERE p.status='SUCCESS' AND p.paid_at>=? AND p.paid_at<?
                  AND %s
                UNION ALL
                SELECT r.refunded_at, st.movie_id, -r.amount, 0::bigint, 0::bigint
                FROM refunds r
                JOIN bookings b ON b.id=r.booking_id
                JOIN showtimes st ON st.id=b.showtime_id
                JOIN auditoriums a ON a.id=st.auditorium_id
                JOIN cinemas c ON c.id=a.cinema_id
                WHERE r.status='REFUNDED' AND r.refunded_at>=? AND r.refunded_at<?
                  AND %s
            )
            """.formatted(SCOPE, SCOPE);

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Duration refundSla;

    JdbcReportingQuery(JdbcTemplate jdbc, Clock clock,
                       @Value("${app.reporting.refund-sla-hours:24}") long refundSlaHours) {
        this.jdbc = jdbc;
        this.clock = clock;
        if (refundSlaHours < 1) throw new IllegalArgumentException("Refund SLA must be positive");
        this.refundSla = Duration.ofHours(refundSlaHours);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReportSummary summary(LocalDate from, LocalDate to, UUID cinemaId, Set<UUID> cinemaScope,
                                 boolean allCinemas) {
        if (!allCinemas && cinemaScope.isEmpty()) {
            throw new IllegalArgumentException("A scoped report requires at least one cinema");
        }
        Instant asOf = clock.instant();
        List<ReportSummary.DailyMetric> daily = daily(from, to, cinemaId, cinemaScope, allCinemas);
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        List<ReportSummary.DailyMetric> previous = daily(from.minusDays(days), from.minusDays(1),
                cinemaId, cinemaScope, allCinemas);
        ReportSummary.Comparison comparison = totals(previous);
        ReportSummary.Comparison current = totals(daily);
        long occupancy = occupancy(from, to, cinemaId, cinemaScope, allCinemas);
        ReportSummary.OperationalAlerts alerts = alerts(cinemaId, cinemaScope, allCinemas, asOf);
        List<ReportSummary.TopMovie> topMovies = topMovies(from, to, cinemaId, cinemaScope, allCinemas);
        return new ReportSummary(current.netRevenue(), current.bookings(), current.seatsSold(),
                occupancy, alerts, comparison, daily, topMovies, asOf);
    }

    private List<ReportSummary.DailyMetric> daily(LocalDate from, LocalDate to, UUID cinemaId,
                                                   Set<UUID> cinemaScope, boolean allCinemas) {
        Map<LocalDate, ReportSummary.DailyMetric> rows = new HashMap<>();
        jdbc.query(EVENTS + """
                SELECT (event_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS day,
                       sum(revenue) AS net_revenue, sum(bookings) AS bookings, sum(seats) AS seats_sold
                FROM events GROUP BY 1 ORDER BY 1
                """, (result, index) -> new ReportSummary.DailyMetric(
                result.getObject("day", LocalDate.class), result.getLong("net_revenue"),
                result.getLong("bookings"), result.getLong("seats_sold")),
                eventArgs(from, to, cinemaId, cinemaScope, allCinemas)).forEach(row -> rows.put(row.date(), row));
        return from.datesUntil(to.plusDays(1))
                .map(day -> rows.getOrDefault(day, new ReportSummary.DailyMetric(day, 0, 0, 0))).toList();
    }

    private List<ReportSummary.TopMovie> topMovies(LocalDate from, LocalDate to, UUID cinemaId,
                                                    Set<UUID> cinemaScope, boolean allCinemas) {
        return jdbc.query(EVENTS + """
                SELECT movie.id, movie.title, sum(event.revenue) AS net_revenue,
                       sum(event.seats) AS seats_sold
                FROM events event JOIN movies movie ON movie.id=event.movie_id
                GROUP BY movie.id, movie.title
                ORDER BY net_revenue DESC, seats_sold DESC, movie.title LIMIT 5
                """, (result, index) -> new ReportSummary.TopMovie(result.getObject("id", UUID.class),
                result.getString("title"), result.getLong("net_revenue"), result.getLong("seats_sold")),
                eventArgs(from, to, cinemaId, cinemaScope, allCinemas));
    }

    private long occupancy(LocalDate from, LocalDate to, UUID cinemaId, Set<UUID> cinemaScope,
                           boolean allCinemas) {
        long[] values = jdbc.queryForObject("""
                SELECT count(*) FILTER (WHERE ss.status='SOLD') AS sold,
                       count(*) FILTER (WHERE ss.status<>'BLOCKED') AS capacity
                FROM showtime_seats ss
                JOIN showtimes st ON st.id=ss.showtime_id
                JOIN auditoriums a ON a.id=st.auditorium_id
                JOIN cinemas c ON c.id=a.cinema_id
                WHERE st.status='SCHEDULED' AND st.start_at>=? AND st.start_at<?
                  AND %s
                """.formatted(SCOPE), (result, index) -> new long[] { result.getLong("sold"),
                result.getLong("capacity") }, start(from), end(to), allCinemas,
                scopeArray(cinemaScope), cinemaId, cinemaId);
        return values[1] == 0 ? 0 : values[0] * 100 / values[1];
    }

    private ReportSummary.OperationalAlerts alerts(UUID cinemaId, Set<UUID> cinemaScope,
                                                     boolean allCinemas, Instant asOf) {
        return jdbc.queryForObject("""
                WITH scoped AS (
                    SELECT b.status, b.payment_deadline, st.status AS showtime_status,
                           EXISTS (SELECT 1 FROM refunds r WHERE r.booking_id=b.id
                                   AND r.status='REQUESTED' AND r.requested_at<?) AS overdue_refund,
                           EXISTS (SELECT 1 FROM refunds r WHERE r.booking_id=b.id
                                   AND r.status='REFUND_FAILED') AS failed_refund,
                           NOT EXISTS (SELECT 1 FROM tickets t WHERE t.booking_id=b.id) AS missing_ticket
                    FROM bookings b
                    JOIN showtimes st ON st.id=b.showtime_id
                    JOIN auditoriums a ON a.id=st.auditorium_id
                    JOIN cinemas c ON c.id=a.cinema_id
                    WHERE %s
                ), flagged AS (
                    SELECT status='PAYMENT_REVIEW' AS payment_review,
                           status='PENDING_PAYMENT' AND payment_deadline<? AS overdue_payment,
                           status='REFUND_PENDING' AND overdue_refund AS overdue_refund,
                           failed_refund,
                           showtime_status='CANCELLED' AND status IN ('PAID','PAYMENT_REVIEW','REFUND_PENDING') AS cancelled,
                           status='PAID' AND missing_ticket AS paid_without_ticket
                    FROM scoped
                )
                SELECT count(*) FILTER (WHERE payment_review OR overdue_payment OR overdue_refund
                                       OR failed_refund OR cancelled OR paid_without_ticket) AS total,
                       count(*) FILTER (WHERE payment_review) AS payment_review,
                       count(*) FILTER (WHERE overdue_payment) AS overdue_payment,
                       count(*) FILTER (WHERE overdue_refund) AS overdue_refund,
                       count(*) FILTER (WHERE failed_refund) AS failed_refund,
                       count(*) FILTER (WHERE cancelled) AS cancelled,
                       count(*) FILTER (WHERE paid_without_ticket) AS paid_without_ticket
                FROM flagged
                """.formatted(SCOPE), (result, index) -> new ReportSummary.OperationalAlerts(
                result.getLong("total"), result.getLong("payment_review"),
                result.getLong("overdue_payment"), result.getLong("overdue_refund"),
                result.getLong("failed_refund"), result.getLong("cancelled"),
                result.getLong("paid_without_ticket")),
                Timestamp.from(asOf.minus(refundSla)), allCinemas, scopeArray(cinemaScope),
                cinemaId, cinemaId, Timestamp.from(asOf));
    }

    private ReportSummary.Comparison totals(List<ReportSummary.DailyMetric> daily) {
        return new ReportSummary.Comparison(
                daily.stream().mapToLong(ReportSummary.DailyMetric::netRevenue).sum(),
                daily.stream().mapToLong(ReportSummary.DailyMetric::bookings).sum(),
                daily.stream().mapToLong(ReportSummary.DailyMetric::seatsSold).sum());
    }

    private Object[] eventArgs(LocalDate from, LocalDate to, UUID cinemaId, Set<UUID> cinemaScope,
                               boolean allCinemas) {
        Object[] branch = { start(from), end(to), allCinemas, scopeArray(cinemaScope), cinemaId, cinemaId };
        Object[] args = Arrays.copyOf(branch, branch.length * 2);
        System.arraycopy(branch, 0, args, branch.length, branch.length);
        return args;
    }

    private Timestamp start(LocalDate date) {
        return Timestamp.from(date.atStartOfDay(BUSINESS_ZONE).toInstant());
    }

    private Timestamp end(LocalDate date) {
        return start(date.plusDays(1));
    }

    private String scopeArray(Set<UUID> scope) {
        return "{" + scope.stream().map(UUID::toString).collect(Collectors.joining(",")) + "}";
    }
}
