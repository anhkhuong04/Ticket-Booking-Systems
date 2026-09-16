package com.lak.moviebooking.reservation.infrastructure;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.lak.moviebooking.reservation.application.SeatAvailabilityEvent;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

class RedisSeatHoldTtlStoreTests {

    @Test
    void treatsAnUnavailableRedisConnectionAsABestEffortMirror() {
        RedisSeatHoldTtlStore store = new RedisSeatHoldTtlStore(new StringRedisTemplate());
        UUID showtimeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();

        assertThatCode(() -> store.write(showtimeId, List.of(seatId), UUID.randomUUID(), Duration.ofMinutes(5)))
                .doesNotThrowAnyException();
        assertThatCode(() -> store.delete(showtimeId, List.of(seatId)))
                .doesNotThrowAnyException();
    }

    @Test
    void removesAFailingWebSocketClientWithoutFailingTheBroadcast() {
        AtomicInteger sendAttempts = new AtomicInteger();
        WebSocketSession failingSession = (WebSocketSession) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {WebSocketSession.class}, (proxy, method, arguments) -> {
                    return switch (method.getName()) {
                        case "isOpen" -> true;
                        case "sendMessage" -> {
                            sendAttempts.incrementAndGet();
                            throw new IOException("client disconnected");
                        }
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == arguments[0];
                        default -> null;
                    };
                });
        SeatEventsWebSocketHandler handler = new SeatEventsWebSocketHandler(new ObjectMapper());
        handler.afterConnectionEstablished(failingSession);
        SeatAvailabilityEvent event = new SeatAvailabilityEvent(
                "SEATS_UPDATED", UUID.randomUUID(), UUID.randomUUID(), List.of(UUID.randomUUID()));

        assertThatCode(() -> handler.broadcast(event)).doesNotThrowAnyException();
        assertThatCode(() -> handler.broadcast(event)).doesNotThrowAnyException();
        assertThat(sendAttempts).hasValue(1);
    }
}
