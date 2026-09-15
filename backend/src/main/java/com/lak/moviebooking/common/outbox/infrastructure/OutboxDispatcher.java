package com.lak.moviebooking.common.outbox.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.lak.moviebooking.common.outbox.application.OutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
class OutboxDispatcher {

	private static final String NO_HANDLER_ERROR = "NO_OUTBOX_HANDLER";

	private final JdbcOutboxRepository repository;
	private final List<OutboxEventHandler> handlers;
	private final OutboxProperties properties;
	private final Clock clock;

	OutboxDispatcher(
			JdbcOutboxRepository repository,
			List<OutboxEventHandler> handlers,
			OutboxProperties properties,
			Clock clock) {
		this.repository = repository;
		this.handlers = List.copyOf(handlers);
		this.properties = properties;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${app.outbox.poll-interval:1s}")
	void poll() {
		dispatchBatch();
	}

	int dispatchBatch() {
		Instant now = clock.instant();
		List<OutboxEvent> events = repository.claimBatch(
				now,
				now.minus(properties.processingTimeout()),
				properties.batchSize());

		for (OutboxEvent event : events) {
			dispatch(event);
		}
		return events.size();
	}

	private void dispatch(OutboxEvent event) {
		OutboxEventHandler handler = handlers.stream()
				.filter(candidate -> candidate.supports(event.eventType()))
				.findFirst()
				.orElse(null);

		if (handler == null) {
			recordFailure(event, NO_HANDLER_ERROR);
			return;
		}

		try {
			handler.handle(event);
			repository.markPublished(event.id(), clock.instant());
		}
		catch (Exception exception) {
			recordFailure(event, exception.getClass().getName());
		}
	}

	private void recordFailure(OutboxEvent event, String errorCode) {
		Instant now = clock.instant();
		if (event.retryCount() + 1 >= properties.maxAttempts()) {
			repository.markDeadLetter(event.id(), now, errorCode);
			return;
		}

		repository.scheduleRetry(
				event.id(),
				now.plus(backoffFor(event.retryCount())),
				now,
				errorCode);
	}

	private Duration backoffFor(int retryCount) {
		long multiplier = 1L << Math.min(retryCount, 30);
		Duration candidate;
		try {
			candidate = properties.initialBackoff().multipliedBy(multiplier);
		}
		catch (ArithmeticException exception) {
			return properties.maxBackoff();
		}
		return candidate.compareTo(properties.maxBackoff()) > 0
				? properties.maxBackoff()
				: candidate;
	}
}
