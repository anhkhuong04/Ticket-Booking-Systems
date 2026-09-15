package com.lak.moviebooking.common.outbox.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.lak.moviebooking.common.outbox.application.OutboxEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcOutboxRepository {

	private static final String CLAIM_SQL = """
			WITH candidates AS (
			    SELECT id
			    FROM outbox_events
			    WHERE (status = 'PENDING' AND available_at <= ?)
			       OR (status = 'PROCESSING' AND updated_at <= ?)
			    ORDER BY available_at, created_at
			    FOR UPDATE SKIP LOCKED
			    LIMIT ?
			)
			UPDATE outbox_events AS event
			SET status = 'PROCESSING',
			    retry_count = CASE
			        WHEN event.status = 'PROCESSING' THEN event.retry_count + 1
			        ELSE event.retry_count
			    END,
			    updated_at = ?
			FROM candidates
			WHERE event.id = candidates.id
			RETURNING event.id, event.event_type, event.aggregate_type,
			          event.aggregate_id, event.payload::text AS payload,
			          event.retry_count, event.occurred_at
			""";

	private final JdbcTemplate jdbcTemplate;

	JdbcOutboxRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	void insert(UUID id, String eventType, String aggregateType, UUID aggregateId,
			String payload, Instant now) {
		jdbcTemplate.update("""
				INSERT INTO outbox_events (
				    id, event_type, aggregate_type, aggregate_id, payload, status,
				    retry_count, available_at, occurred_at, created_at, updated_at
				) VALUES (?, ?, ?, ?, CAST(? AS jsonb), 'PENDING', 0, ?, ?, ?, ?)
				""", id, eventType, aggregateType, aggregateId, payload,
				atUtc(now), atUtc(now), atUtc(now), atUtc(now));
	}

	@Transactional
	List<OutboxEvent> claimBatch(Instant now, Instant staleBefore, int batchSize) {
		return jdbcTemplate.query(CLAIM_SQL, this::mapEvent,
				atUtc(now), atUtc(staleBefore), batchSize, atUtc(now));
	}

	@Transactional
	boolean markPublished(UUID id, Instant publishedAt) {
		return jdbcTemplate.update("""
				UPDATE outbox_events
				SET status = 'PUBLISHED', published_at = ?, last_error_code = NULL, updated_at = ?
				WHERE id = ? AND status = 'PROCESSING'
				""", atUtc(publishedAt), atUtc(publishedAt), id) == 1;
	}

	@Transactional
	boolean scheduleRetry(UUID id, Instant availableAt, Instant now, String errorCode) {
		return jdbcTemplate.update("""
				UPDATE outbox_events
				SET status = 'PENDING', retry_count = retry_count + 1,
				    available_at = ?, last_error_code = ?, updated_at = ?
				WHERE id = ? AND status = 'PROCESSING'
				""", atUtc(availableAt), errorCode, atUtc(now), id) == 1;
	}

	@Transactional
	boolean markDeadLetter(UUID id, Instant now, String errorCode) {
		return jdbcTemplate.update("""
				UPDATE outbox_events
				SET status = 'DEAD_LETTER', retry_count = retry_count + 1,
				    last_error_code = ?, updated_at = ?
				WHERE id = ? AND status = 'PROCESSING'
				""", errorCode, atUtc(now), id) == 1;
	}

	private OutboxEvent mapEvent(ResultSet resultSet, int rowNumber) throws SQLException {
		return new OutboxEvent(
				resultSet.getObject("id", UUID.class),
				resultSet.getString("event_type"),
				resultSet.getString("aggregate_type"),
				resultSet.getObject("aggregate_id", UUID.class),
				resultSet.getString("payload"),
				resultSet.getInt("retry_count"),
				resultSet.getObject("occurred_at", OffsetDateTime.class).toInstant());
	}

	private static OffsetDateTime atUtc(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC);
	}
}
