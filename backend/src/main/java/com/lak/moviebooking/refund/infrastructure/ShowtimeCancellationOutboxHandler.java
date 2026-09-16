package com.lak.moviebooking.refund.infrastructure;

import java.util.UUID;

import com.lak.moviebooking.common.outbox.application.OutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventHandler;
import com.lak.moviebooking.refund.application.RefundManagement;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Runs the cancellaton workflow after its showtime status transition has committed. */
@Component
class ShowtimeCancellationOutboxHandler implements OutboxEventHandler {

    private static final String EVENT_TYPE = "refund.showtime_cancellation_requested";
    private final ObjectMapper objectMapper;
    private final RefundManagement refundManagement;

    ShowtimeCancellationOutboxHandler(ObjectMapper objectMapper, RefundManagement refundManagement) {
        this.objectMapper = objectMapper;
        this.refundManagement = refundManagement;
    }

    @Override
    public boolean supports(String eventType) {
        return EVENT_TYPE.equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        try {
            ShowtimeCancellationPayload payload = objectMapper.readValue(event.payload(), ShowtimeCancellationPayload.class);
            refundManagement.processShowtimeCancellation(payload.showtimeId(), payload.cancelledAt());
        }
        catch (JacksonException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid showtime cancellation event", exception);
        }
    }

    private record ShowtimeCancellationPayload(UUID showtimeId, java.time.Instant cancelledAt) {
    }
}
