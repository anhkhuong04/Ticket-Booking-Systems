package com.lak.moviebooking.common.outbox.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import com.lak.moviebooking.common.outbox.application.NewOutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventWriter;
import com.lak.moviebooking.support.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class OutboxClaimConcurrencyIT extends AbstractIntegrationTest {

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
	void competingWorkersNeverClaimTheSameEvent() throws Exception {
		transactionTemplate.executeWithoutResult(status -> {
			for (int index = 0; index < 10; index++) {
				writer.append(new NewOutboxEvent(
						"booking.confirmed",
						"booking",
						UUID.randomUUID(),
						Map.of("sequence", index)));
			}
		});

		CyclicBarrier startTogether = new CyclicBarrier(2);
		Instant now = clock.instant().plusSeconds(1);
		CompletableFuture<List<OutboxEvent>> first = claimAsync(startTogether, now);
		CompletableFuture<List<OutboxEvent>> second = claimAsync(startTogether, now);

		List<OutboxEvent> firstClaim = first.get(10, TimeUnit.SECONDS);
		List<OutboxEvent> secondClaim = second.get(10, TimeUnit.SECONDS);
		Set<UUID> firstIds = idsOf(firstClaim);
		Set<UUID> secondIds = idsOf(secondClaim);
		Set<UUID> allIds = new HashSet<>(firstIds);
		allIds.addAll(secondIds);

		assertThat(Collections.disjoint(firstIds, secondIds)).isTrue();
		assertThat(allIds).hasSize(10);
	}

	private CompletableFuture<List<OutboxEvent>> claimAsync(CyclicBarrier barrier, Instant now) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				barrier.await(5, TimeUnit.SECONDS);
				return repository.claimBatch(now, now.minusSeconds(120), 10);
			}
			catch (Exception exception) {
				throw new IllegalStateException(exception);
			}
		});
	}

	private Set<UUID> idsOf(List<OutboxEvent> events) {
		Set<UUID> ids = new HashSet<>();
		for (OutboxEvent event : events) {
			ids.add(event.id());
		}
		return ids;
	}
}
