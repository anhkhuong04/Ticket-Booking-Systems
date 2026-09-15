package com.lak.moviebooking.reservation.infrastructure;

import com.lak.moviebooking.common.outbox.application.OutboxEvent;
import com.lak.moviebooking.common.outbox.application.OutboxEventHandler;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class SeatEventsOutboxHandler implements OutboxEventHandler {

    private static final java.util.Set<String> EVENT_TYPES = java.util.Set.of(
            "reservation.seats_updated", "reservation.hold_expired");
    private final ObjectMapper objectMapper;
    private final SeatEventsWebSocketHandler webSocketHandler;

    SeatEventsOutboxHandler(ObjectMapper objectMapper, SeatEventsWebSocketHandler webSocketHandler) {
        this.objectMapper = objectMapper;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public boolean supports(String eventType) {
        return EVENT_TYPES.contains(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        try {
            webSocketHandler.broadcast(objectMapper.readValue(event.payload(), SeatAvailabilityEvent.class));
        }
        catch (JacksonException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid seat availability event", exception);
        }
    }
}
