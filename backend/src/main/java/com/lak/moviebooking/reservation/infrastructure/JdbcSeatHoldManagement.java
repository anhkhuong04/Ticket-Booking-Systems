package com.lak.moviebooking.reservation.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.common.outbox.application.NewOutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventWriter;
import com.lak.moviebooking.reservation.application.SeatHoldCommand;
import com.lak.moviebooking.reservation.application.SeatHoldManagement;
import com.lak.moviebooking.reservation.application.SeatHoldView;
import com.lak.moviebooking.reservation.application.SeatAvailabilityEvent;
import com.lak.moviebooking.showtime.application.ShowtimeSeatForHold;
import com.lak.moviebooking.showtime.application.ShowtimeSeatInventory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
class JdbcSeatHoldManagement implements SeatHoldManagement {

    private static final String CREATE_OPERATION = "SEAT_HOLD_CREATE";
    private static final String ACTIVE = "ACTIVE";
    private final JdbcTemplate jdbcTemplate;
    private final ShowtimeSeatInventory showtimeSeatInventory;
    private final OutboxEventWriter outboxEventWriter;
    private final RedisSeatHoldTtlStore redisSeatHoldTtlStore;
    private final ExpiredSeatHoldReleaser expiredSeatHoldReleaser;
    private final ReservationProperties properties;
    private final Clock clock;

    JdbcSeatHoldManagement(
            JdbcTemplate jdbcTemplate,
            ShowtimeSeatInventory showtimeSeatInventory,
            OutboxEventWriter outboxEventWriter,
            RedisSeatHoldTtlStore redisSeatHoldTtlStore,
            ExpiredSeatHoldReleaser expiredSeatHoldReleaser,
            ReservationProperties properties,
            Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.showtimeSeatInventory = showtimeSeatInventory;
        this.outboxEventWriter = outboxEventWriter;
        this.redisSeatHoldTtlStore = redisSeatHoldTtlStore;
        this.expiredSeatHoldReleaser = expiredSeatHoldReleaser;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SeatHoldView create(UUID userId, SeatHoldCommand command) {
        validate(command);
        Instant now = databasePrecision(clock.instant());
        String requestHash = requestHash(command);
        UUID holdId = claimIdempotency(userId, command.idempotencyKey(), requestHash, now);
        SeatHoldRow existing = findStored(holdId);
        if (existing != null) {
            if (isExpired(existing, now)) {
                expiredSeatHoldReleaser.expire(existing.id(), now);
                throw ApplicationException.expired("SEAT_HOLD_EXPIRED", "Your seat hold has expired");
            }
            return view(existing);
        }

        // All expiry paths lock the hold before its seats.  Do this before acquiring the
        // requested seats so concurrent expiry and hold transactions cannot deadlock.
        expireDueHoldsInternal(now);
        List<ShowtimeSeatForHold> lockedSeats = showtimeSeatInventory.lockForHold(
                command.showtimeId(), command.showtimeSeatIds(), now);
        requireCompleteCoupleSelection(command.showtimeSeatIds(), lockedSeats);
        requireAllAvailable(lockedSeats);

        Instant expiresAt = now.plus(properties.holdTtl());
        jdbcTemplate.update("""
                INSERT INTO seat_holds (id,user_id,showtime_id,status,expires_at,hard_expires_at,created_at,updated_at)
                VALUES (?,?,?,'ACTIVE',?,?,?,?)
                """, holdId, userId, command.showtimeId(), atUtc(expiresAt), atUtc(expiresAt), atUtc(now), atUtc(now));
        for (UUID seatId : command.showtimeSeatIds()) {
            jdbcTemplate.update("INSERT INTO seat_hold_items (hold_id,showtime_seat_id) VALUES (?,?)", holdId, seatId);
        }
        showtimeSeatInventory.markHeld(command.showtimeSeatIds(), holdId, expiresAt, now);
        appendSeatEvent("SEATS_UPDATED", command.showtimeId(), holdId, command.showtimeSeatIds());
        afterCommit(() -> redisSeatHoldTtlStore.write(
                command.showtimeId(), command.showtimeSeatIds(), holdId, Duration.between(now, expiresAt)));
        return new SeatHoldView(holdId, command.showtimeId(), ACTIVE, now, expiresAt, expiresAt, command.showtimeSeatIds());
    }

    @Override
    @Transactional
    public SeatHoldView find(UUID userId, UUID holdId) {
        SeatHoldRow hold = findHoldForUser(userId, holdId);
        Instant now = clock.instant();
        if (isExpired(hold, now)) {
            expiredSeatHoldReleaser.expire(hold.id(), now);
            throw ApplicationException.expired("SEAT_HOLD_EXPIRED", "Your seat hold has expired");
        }
        hold = lockHoldForUser(userId, holdId);
        return view(hold);
    }

    @Override
    @Transactional
    public void release(UUID userId, UUID holdId) {
        SeatHoldRow hold = findHoldForUser(userId, holdId);
        Instant now = clock.instant();
        if (isExpired(hold, now)) {
            expiredSeatHoldReleaser.expire(hold.id(), now);
            throw ApplicationException.expired("SEAT_HOLD_EXPIRED", "Your seat hold has expired");
        }
        hold = lockHoldForUser(userId, holdId);
        if (!ACTIVE.equals(hold.status())) {
            return;
        }
        SeatHoldRow activeHold = hold;
        List<UUID> releasedSeatIds = showtimeSeatInventory.releaseHold(activeHold.id(), now);
        jdbcTemplate.update("UPDATE seat_holds SET status='RELEASED',updated_at=? WHERE id=?", atUtc(now), activeHold.id());
        appendSeatEvent("SEATS_UPDATED", activeHold.showtimeId(), activeHold.id(), releasedSeatIds);
        afterCommit(() -> redisSeatHoldTtlStore.delete(activeHold.showtimeId(), releasedSeatIds));
    }

    @Override
    @Transactional
    public void consumeForCheckout(UUID userId, UUID holdId) {
        SeatHoldRow hold = findHoldForUser(userId, holdId);
        Instant now = clock.instant();
        if (isExpired(hold, now)) {
            expiredSeatHoldReleaser.expire(hold.id(), now);
            throw ApplicationException.expired("SEAT_HOLD_EXPIRED", "Your seat hold has expired");
        }
        hold = lockHoldForUser(userId, holdId);
        if (!ACTIVE.equals(hold.status())) {
            throw ApplicationException.businessRule("SEAT_HOLD_NOT_ACTIVE", "Seat hold is no longer active");
        }
        jdbcTemplate.update("UPDATE seat_holds SET status='CONSUMED',updated_at=? WHERE id=? AND status='ACTIVE'",
                atUtc(now), hold.id());
    }

    @Override
    @Transactional
    public int expireDueHolds(Instant now) {
        return expireDueHoldsInternal(now);
    }

    @Override
    @Transactional
    public List<UUID> cancelActiveForShowtime(UUID showtimeId, Instant now) {
        List<SeatHoldRow> holds = jdbcTemplate.query("""
                SELECT id,user_id,showtime_id,status,expires_at,hard_expires_at
                FROM seat_holds WHERE showtime_id=? AND status='ACTIVE'
                ORDER BY id FOR UPDATE
                """, this::mapHold, showtimeId);
        List<UUID> released = new ArrayList<>();
        for (SeatHoldRow hold : holds) {
            List<UUID> releasedSeatIds = showtimeSeatInventory.releaseHold(hold.id(), now);
            jdbcTemplate.update("UPDATE seat_holds SET status='CANCELLED',updated_at=? WHERE id=? AND status='ACTIVE'",
                    atUtc(now), hold.id());
            released.addAll(releasedSeatIds);
            appendSeatEvent("SEATS_UPDATED", showtimeId, hold.id(), releasedSeatIds);
            afterCommit(() -> redisSeatHoldTtlStore.delete(showtimeId, releasedSeatIds));
        }
        return List.copyOf(released);
    }

    private int expireDueHoldsInternal(Instant now) {
        List<SeatHoldRow> holds = jdbcTemplate.query("""
                SELECT id,user_id,showtime_id,status,expires_at,hard_expires_at
                FROM seat_holds
                WHERE status='ACTIVE' AND expires_at <= ?
                ORDER BY expires_at,id
                FOR UPDATE SKIP LOCKED
                LIMIT ?
                """, this::mapHold, atUtc(now), properties.expiryBatchSize());
        holds.forEach(hold -> expire(hold, now));
        jdbcTemplate.update("DELETE FROM idempotency_requests WHERE expires_at <= ?", atUtc(now));
        return holds.size();
    }

    private UUID claimIdempotency(UUID userId, String key, String requestHash, Instant now) {
        UUID proposedHoldId = UUID.randomUUID();
        int inserted = jdbcTemplate.update("""
                INSERT INTO idempotency_requests (id,actor_id,operation,idempotency_key,request_hash,resource_id,created_at,expires_at)
                VALUES (?,?,?,?,?,?,?,?) ON CONFLICT (actor_id,operation,idempotency_key) DO NOTHING
                """, UUID.randomUUID(), userId, CREATE_OPERATION, key, requestHash, proposedHoldId,
                atUtc(now), atUtc(now.plus(properties.idempotencyTtl())));
        if (inserted == 1) {
            return proposedHoldId;
        }
        IdempotencyRow stored = jdbcTemplate.query("""
                SELECT request_hash,resource_id FROM idempotency_requests
                WHERE actor_id=? AND operation=? AND idempotency_key=? FOR UPDATE
                """, (resultSet, rowNumber) -> new IdempotencyRow(resultSet.getString("request_hash"),
                resultSet.getObject("resource_id", UUID.class)), userId, CREATE_OPERATION, key)
                .stream().findFirst().orElseThrow(() -> ApplicationException.conflict(
                        "IDEMPOTENCY_REQUEST_UNAVAILABLE", "The request is still being processed"));
        if (!stored.requestHash().equals(requestHash)) {
            throw ApplicationException.businessRule("IDEMPOTENCY_KEY_REUSED", "Idempotency key was already used for a different request");
        }
        return stored.resourceId();
    }

    private void expire(SeatHoldRow hold, Instant now) {
        if (!ACTIVE.equals(hold.status())) {
            return;
        }
        List<UUID> releasedSeatIds = showtimeSeatInventory.releaseHold(hold.id(), now);
        jdbcTemplate.update("UPDATE seat_holds SET status='EXPIRED',updated_at=? WHERE id=? AND status='ACTIVE'", atUtc(now), hold.id());
        appendSeatEvent("SEATS_UPDATED", hold.showtimeId(), hold.id(), releasedSeatIds);
        appendSeatEvent("HOLD_EXPIRED", hold.showtimeId(), hold.id(), releasedSeatIds);
        afterCommit(() -> redisSeatHoldTtlStore.delete(hold.showtimeId(), releasedSeatIds));
    }

    private SeatHoldRow lockHoldForUser(UUID userId, UUID holdId) {
        SeatHoldRow hold = jdbcTemplate.query("""
                SELECT id,user_id,showtime_id,status,expires_at,hard_expires_at
                FROM seat_holds WHERE id=? FOR UPDATE
                """, this::mapHold, holdId).stream().findFirst()
                .orElseThrow(() -> ApplicationException.notFound("SEAT_HOLD_NOT_FOUND", "Seat hold was not found"));
        if (!hold.userId().equals(userId)) {
            throw ApplicationException.forbidden("SEAT_HOLD_FORBIDDEN", "You do not have access to this seat hold");
        }
        return hold;
    }

    private SeatHoldRow findHoldForUser(UUID userId, UUID holdId) {
        SeatHoldRow hold = jdbcTemplate.query("""
                SELECT id,user_id,showtime_id,status,expires_at,hard_expires_at
                FROM seat_holds WHERE id=?
                """, this::mapHold, holdId).stream().findFirst()
                .orElseThrow(() -> ApplicationException.notFound("SEAT_HOLD_NOT_FOUND", "Seat hold was not found"));
        if (!hold.userId().equals(userId)) {
            throw ApplicationException.forbidden("SEAT_HOLD_FORBIDDEN", "You do not have access to this seat hold");
        }
        return hold;
    }

    private SeatHoldRow findStored(UUID holdId) {
        return jdbcTemplate.query("""
                SELECT id,user_id,showtime_id,status,expires_at,hard_expires_at
                FROM seat_holds WHERE id=?
                """, this::mapHold, holdId).stream().findFirst().orElse(null);
    }

    private void requireCompleteCoupleSelection(List<UUID> requestedIds, List<ShowtimeSeatForHold> lockedSeats) {
        Set<UUID> requested = Set.copyOf(requestedIds);
        Map<String, List<UUID>> pairs = lockedSeats.stream()
                .filter(seat -> seat.pairKey() != null)
                .collect(java.util.stream.Collectors.groupingBy(ShowtimeSeatForHold::pairKey,
                        java.util.stream.Collectors.mapping(ShowtimeSeatForHold::id, java.util.stream.Collectors.toList())));
        boolean incomplete = pairs.values().stream().anyMatch(pair -> !requested.containsAll(pair));
        if (incomplete) {
            throw ApplicationException.businessRule("COUPLE_SEAT_PAIR_REQUIRED", "Couple seats must be held together");
        }
    }

    private void requireAllAvailable(List<ShowtimeSeatForHold> seats) {
        if (seats.stream().anyMatch(seat -> !"AVAILABLE".equals(seat.status()))) {
            throw ApplicationException.conflict("SEAT_UNAVAILABLE", "One or more selected seats are no longer available");
        }
    }

    private void appendSeatEvent(String type, UUID showtimeId, UUID holdId, List<UUID> seatIds) {
        outboxEventWriter.append(new NewOutboxEvent(
                "reservation." + type.toLowerCase(java.util.Locale.ROOT), "seat_hold", holdId,
                new SeatAvailabilityEvent(type, showtimeId, holdId, seatIds)));
    }

    private void validate(SeatHoldCommand command) {
        if (command.showtimeId() == null || command.idempotencyKey() == null || command.idempotencyKey().isBlank()
                || command.showtimeSeatIds().isEmpty() || command.showtimeSeatIds().size() > 12
                || new HashSet<>(command.showtimeSeatIds()).size() != command.showtimeSeatIds().size()) {
            throw ApplicationException.businessRule("INVALID_SEAT_HOLD", "Seat hold request is invalid");
        }
    }

    private String requestHash(SeatHoldCommand command) {
        String input = command.showtimeId() + ":" + command.showtimeSeatIds().stream()
                .sorted(Comparator.naturalOrder()).map(UUID::toString).collect(java.util.stream.Collectors.joining(","));
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private SeatHoldView view(SeatHoldRow hold) {
        return new SeatHoldView(hold.id(), hold.showtimeId(), hold.status(), clock.instant(), hold.expiresAt(), hold.hardExpiresAt(),
                jdbcTemplate.query("SELECT showtime_seat_id FROM seat_hold_items WHERE hold_id=? ORDER BY showtime_seat_id",
                        (resultSet, rowNumber) -> resultSet.getObject("showtime_seat_id", UUID.class), hold.id()));
    }

    private SeatHoldRow mapHold(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        return new SeatHoldRow(resultSet.getObject("id", UUID.class), resultSet.getObject("user_id", UUID.class),
                resultSet.getObject("showtime_id", UUID.class), resultSet.getString("status"),
                resultSet.getObject("expires_at", OffsetDateTime.class).toInstant(),
                resultSet.getObject("hard_expires_at", OffsetDateTime.class).toInstant());
    }

    private boolean isExpired(SeatHoldRow hold, Instant now) {
        return ACTIVE.equals(hold.status()) && !hold.expiresAt().isAfter(now);
    }

    private void afterCommit(Runnable callback) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                callback.run();
            }
        });
    }

    private String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }

    private OffsetDateTime atUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private Instant databasePrecision(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    private record IdempotencyRow(String requestHash, UUID resourceId) {
    }

    private record SeatHoldRow(UUID id, UUID userId, UUID showtimeId, String status, Instant expiresAt, Instant hardExpiresAt) {
    }
}
