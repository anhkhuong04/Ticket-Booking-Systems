package com.lak.moviebooking.common.outbox.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.lak.moviebooking.common.outbox.application.NewOutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventHandler;
import com.lak.moviebooking.common.outbox.application.OutboxEventWriter;
import com.lak.moviebooking.support.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

class OutboxIntegrationIT extends AbstractIntegrationTest {

	@Autowired
	private OutboxEventWriter writer;

	@Autowired
	private JdbcOutboxRepository repository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private Clock clock;

	@AfterEach
	void clearOutbox() {
		jdbcTemplate.update("DELETE FROM outbox_events");
	}

	@Test
	void writesOnlyInsideTheBusinessTransaction() {
		NewOutboxEvent event = event();

		assertThatThrownBy(() -> writer.append(event))
				.isInstanceOf(IllegalTransactionStateException.class);

		UUID committedId = transactionTemplate.execute(status -> writer.append(event));
		assertThat(countById(committedId)).isOne();

		AtomicReference<UUID> rolledBackId = new AtomicReference<>();
		transactionTemplate.executeWithoutResult(status -> {
			rolledBackId.set(writer.append(event));
			status.setRollbackOnly();
		});
		assertThat(countById(rolledBackId.get())).isZero();
	}

	@Test
	void publishesAnEventAndMarksItComplete() {
		UUID eventId = transactionTemplate.execute(status -> writer.append(event()));
		AtomicReference<OutboxEvent> delivered = new AtomicReference<>();
		OutboxEventHandler handler = handlerThat(delivered);
		OutboxDispatcher dispatcher = dispatcher(List.of(handler), 3);

		assertThat(dispatcher.dispatchBatch()).isOne();
		assertThat(delivered.get().id()).isEqualTo(eventId);
		assertThat(delivered.get().payload()).contains("bookingCode", "LAK-TEST");
		assertThat(statusOf(eventId)).isEqualTo("PUBLISHED");
		assertThat(publishedAtExists(eventId)).isTrue();
	}

	@Test
	void retriesWithBackoffThenMovesTheEventToDeadLetter() {
		UUID eventId = transactionTemplate.execute(status -> writer.append(event()));
		OutboxEventHandler failingHandler = new OutboxEventHandler() {
			@Override
			public boolean supports(String eventType) {
				return true;
			}

			@Override
			public void handle(OutboxEvent event) {
				throw new IllegalStateException("simulated downstream failure");
			}
		};
		OutboxDispatcher dispatcher = dispatcher(List.of(failingHandler), 2);

		assertThat(dispatcher.dispatchBatch()).isOne();
		assertThat(statusOf(eventId)).isEqualTo("PENDING");
		assertThat(retryCountOf(eventId)).isOne();

		assertThat(dispatcher.dispatchBatch()).isOne();
		assertThat(statusOf(eventId)).isEqualTo("DEAD_LETTER");
		assertThat(retryCountOf(eventId)).isEqualTo(2);
		assertThat(lastErrorCodeOf(eventId)).isEqualTo(IllegalStateException.class.getName());
	}

	private OutboxDispatcher dispatcher(List<OutboxEventHandler> handlers, int maxAttempts) {
		OutboxProperties properties = new OutboxProperties(
				false,
				10,
				Duration.ofSeconds(1),
				maxAttempts,
				Duration.ZERO,
				Duration.ofMinutes(1),
				Duration.ofMinutes(2));
		return new OutboxDispatcher(repository, handlers, properties, clock);
	}

	private OutboxEventHandler handlerThat(AtomicReference<OutboxEvent> delivered) {
		return new OutboxEventHandler() {
			@Override
			public boolean supports(String eventType) {
				return "booking.confirmed".equals(eventType);
			}

			@Override
			public void handle(OutboxEvent event) {
				delivered.set(event);
			}
		};
	}

	private NewOutboxEvent event() {
		return new NewOutboxEvent(
				"booking.confirmed",
				"booking",
				UUID.randomUUID(),
				Map.of("bookingCode", "LAK-TEST"));
	}

	private long countById(UUID id) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM outbox_events WHERE id = ?",
				Long.class,
				id);
	}

	private String statusOf(UUID id) {
		return jdbcTemplate.queryForObject(
				"SELECT status FROM outbox_events WHERE id = ?",
				String.class,
				id);
	}

	private int retryCountOf(UUID id) {
		return jdbcTemplate.queryForObject(
				"SELECT retry_count FROM outbox_events WHERE id = ?",
				Integer.class,
				id);
	}

	private boolean publishedAtExists(UUID id) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
				"SELECT published_at IS NOT NULL FROM outbox_events WHERE id = ?",
				Boolean.class,
				id));
	}

	private String lastErrorCodeOf(UUID id) {
		return jdbcTemplate.queryForObject(
				"SELECT last_error_code FROM outbox_events WHERE id = ?",
				String.class,
				id);
	}
}
