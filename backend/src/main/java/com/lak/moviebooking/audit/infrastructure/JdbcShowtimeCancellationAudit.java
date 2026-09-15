package com.lak.moviebooking.audit.infrastructure;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;

import com.lak.moviebooking.audit.application.ShowtimeCancellationAudit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class JdbcShowtimeCancellationAudit implements ShowtimeCancellationAudit {
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    JdbcShowtimeCancellationAudit(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRequested(UUID actorId, UUID showtimeId) {
        jdbcTemplate.update("""
                INSERT INTO audit_logs (id, actor_id, action, entity_type, entity_id, metadata, created_at)
                VALUES (?, ?, 'SHOWTIME_CANCELLATION_REQUESTED', 'showtime', ?, '{}'::jsonb, ?)
                """, UUID.randomUUID(), actorId, showtimeId, clock.instant().atOffset(ZoneOffset.UTC));
    }
}
