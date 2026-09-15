package com.lak.moviebooking.reservation.infrastructure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.common.outbox.application.NewOutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventWriter;
import com.lak.moviebooking.showtime.application.ShowtimeSeatInventory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Commits expiry cleanup before the caller returns the corresponding 410 response. */
@Service
class ExpiredSeatHoldReleaser {

    private final JdbcTemplate jdbcTemplate;
    private final ShowtimeSeatInventory showtimeSeatInventory;
    private final OutboxEventWriter outboxEventWriter;
    private final RedisSeatHoldTtlStore redisSeatHoldTtlStore;

    ExpiredSeatHoldReleaser(
            JdbcTemplate jdbcTemplate,
            ShowtimeSeatInventory showtimeSeatInventory,
            OutboxEventWriter outboxEventWriter,
            RedisSeatHoldTtlStore redisSeatHoldTtlStore) {
        this.jdbcTemplate = jdbcTemplate;
        this.showtimeSeatInventory = showtimeSeatInventory;
        this.outboxEventWriter = outboxEventWriter;
        this.redisSeatHoldTtlStore = redisSeatHoldTtlStore;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void expire(UUID holdId, Instant now) {
        HoldRow hold = jdbcTemplate.query("""
                SELECT id,showtime_id,status,expires_at
                FROM seat_holds WHERE id=? FOR UPDATE
                """, (resultSet, rowNumber) -> new HoldRow(
                resultSet.getObject("id", UUID.class), resultSet.getObject("showtime_id", UUID.class),
                resultSet.getString("status"), resultSet.getObject("expires_at", OffsetDateTime.class).toInstant()), holdId)
                .stream().findFirst().orElse(null);
        if (hold == null || !"ACTIVE".equals(hold.status()) || hold.expiresAt().isAfter(now)) {
            return;
        }

        List<UUID> releasedSeatIds = showtimeSeatInventory.releaseHold(hold.id(), now);
        jdbcTemplate.update("UPDATE seat_holds SET status='EXPIRED',updated_at=? WHERE id=? AND status='ACTIVE'",
                atUtc(now), hold.id());
        appendSeatEvent("SEATS_UPDATED", hold, releasedSeatIds);
        appendSeatEvent("HOLD_EXPIRED", hold, releasedSeatIds);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisSeatHoldTtlStore.delete(hold.showtimeId(), releasedSeatIds);
            }
        });
    }

    private void appendSeatEvent(String type, HoldRow hold, List<UUID> seatIds) {
        outboxEventWriter.append(new NewOutboxEvent(
                "reservation." + type.toLowerCase(java.util.Locale.ROOT), "seat_hold", hold.id(),
                new SeatAvailabilityEvent(type, hold.showtimeId(), hold.id(), seatIds)));
    }

    private OffsetDateTime atUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private record HoldRow(UUID id, UUID showtimeId, String status, Instant expiresAt) {
    }
}
