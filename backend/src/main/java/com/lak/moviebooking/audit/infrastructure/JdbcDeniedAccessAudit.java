package com.lak.moviebooking.audit.infrastructure;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.lak.moviebooking.audit.application.DeniedAccessAudit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class JdbcDeniedAccessAudit implements DeniedAccessAudit {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    JdbcDeniedAccessAudit(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public void record(UUID actorId, String path, String requiredAuthority) {
        jdbcTemplate.update("""
                INSERT INTO audit_logs (id, actor_id, action, entity_type, metadata, created_at)
                VALUES (?, ?, 'AUTHORIZATION_DENIED', 'http_request', CAST(? AS jsonb), ?)
                """, UUID.randomUUID(), actorId,
                "{\"path\":\"" + escapeJson(path) + "\",\"requiredAuthority\":\""
                        + escapeJson(requiredAuthority) + "\"}",
                clock.instant().atOffset(ZoneOffset.UTC));
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
