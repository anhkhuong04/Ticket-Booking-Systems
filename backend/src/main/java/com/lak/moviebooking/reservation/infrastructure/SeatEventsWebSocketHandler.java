package com.lak.moviebooking.reservation.infrastructure;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.lak.moviebooking.reservation.application.SeatAvailabilityEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class SeatEventsWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    SeatEventsWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    void broadcast(SeatAvailabilityEvent event) {
        String payload = serialize(event);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
            catch (IOException exception) {
                sessions.remove(session);
            }
        }
    }

    private String serialize(SeatAvailabilityEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        }
        catch (JacksonException exception) {
            throw new IllegalArgumentException("Seat event cannot be serialized", exception);
        }
    }
}
