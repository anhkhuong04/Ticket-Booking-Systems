package com.lak.moviebooking.audit.infrastructure;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.lak.moviebooking.audit.application.AuditLogWriter;
import com.lak.moviebooking.audit.application.DeniedAccessAudit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class JdbcDeniedAccessAudit implements DeniedAccessAudit, AuditLogWriter {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    JdbcDeniedAccessAudit(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public void record(UUID actorId, String path, String requiredAuthority) {
        record(actorId, "AUTHORIZATION_DENIED", "http_request", null,
                Map.of("path", path, "requiredAuthority", requiredAuthority));
    }

    @Override
    public void record(UUID actorId, String action, String entityType, UUID entityId, Map<String, String> metadata) {
        jdbcTemplate.update("""
                INSERT INTO audit_logs (id, actor_id, action, entity_type, entity_id, metadata, created_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
                """, UUID.randomUUID(), actorId, action, entityType, entityId, json(metadata),
                clock.instant().atOffset(ZoneOffset.UTC));
    }

    private String json(Map<String, String> metadata) {
        return metadata.entrySet().stream()
                .map(entry -> "\"" + escapeJson(entry.getKey()) + "\":\"" + escapeJson(entry.getValue()) + "\"")
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
