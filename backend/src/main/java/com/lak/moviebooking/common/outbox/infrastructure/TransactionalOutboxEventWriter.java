package com.lak.moviebooking.common.outbox.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.lak.moviebooking.common.outbox.application.NewOutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class TransactionalOutboxEventWriter implements OutboxEventWriter {

	private final JdbcOutboxRepository repository;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	TransactionalOutboxEventWriter(
			JdbcOutboxRepository repository,
			ObjectMapper objectMapper,
			Clock clock) {
		this.repository = repository;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public UUID append(NewOutboxEvent event) {
		UUID id = UUID.randomUUID();
		Instant now = clock.instant();
		repository.insert(
				id,
				event.eventType(),
				event.aggregateType(),
				event.aggregateId(),
				serialize(event.payload()),
				now);
		return id;
	}

	private String serialize(Object payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		}
		catch (JacksonException exception) {
			throw new IllegalArgumentException("Outbox payload cannot be serialized", exception);
		}
	}
}
